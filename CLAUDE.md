# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

EAShell — a JavaFX 17 desktop launcher for shell commands. The user stores named *scripts*
(working directory + ordered list of commands, optionally grouped), runs them with one click,
answers prompts via stdin, and watches merged stdout/stderr in per-script console tabs. Scripts
persist to a JSON file under the user's profile.

~1700 lines, 15 classes, no framework beyond JavaFX + Gson. Single-window, no network, no database.
Also ships as a self-contained Windows app-image (`scripts/package.ps1` → jpackage) that needs no
system Java.

### Documentation map

| Document | Contents |
| --- | --- |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | How it works today: packages, threading model, persistence, styling, extension points |
| [`docs/IMPROVEMENTS.md`](docs/IMPROVEMENTS.md) | Defects and technical debt, P0–P3, with exact call sites. Items 1–22 are the original review; items 23–32 came out of the v2.0.0 verification pass (regressions in that release). Both sets are now mostly fixed — see its status table for exactly what's still open |
| [`docs/ROADMAP.md`](docs/ROADMAP.md) | Feature designs (groups, stdin, packaging, output quality) — most are now implemented; §8 tracks what's done vs. still open, plus what's deliberately out of scope |
| [`docs/PACKAGING.md`](docs/PACKAGING.md) | jpackage runbook — shipping without requiring Java on the target machine; app-image done, MSI not yet (needs WiX) |

**Check `IMPROVEMENTS.md` before "fixing" anything** — the issue may already be fixed (check its
status table first) or already catalogued with a call site and a suggested approach. **Check
`ROADMAP.md` before designing a feature** — it may already be built, or the design decision (and the
traps) may already be recorded, including features that were deliberately rejected.

## Commands

```bash
mvn clean javafx:run      # run from source (fastest feedback loop)
mvn clean package         # → target/EAShell-<version>.jar + target/libs/ + target/EAShell.exe
mvn test                  # JUnit 5; ScriptEntry + ScriptRepository have real coverage
mvn -q compile            # syntax check without packaging
powershell -File scripts/package.ps1   # self-contained app-image via jpackage, see PACKAGING.md
```

The Windows `.exe` from `mvn clean package` is produced by launch4j-maven-plugin and still requires a
system JRE 17+ — it's kept for that use case. `scripts/package.ps1` produces a separate,
bundled-runtime `target/dist/EAShell/EAShell.exe` that needs no Java installed at all.

There is no linter, formatter config, or CI in this repo.

## Architecture in one screen

```
App (Application)
 ├── SingleInstanceLock ............. FileChannel lock, checked before anything else builds
 └── MainWindow ..................... composition root; owns ALL coordination
      ├── ScriptRepository ......... in-memory List + Gson ⇄ %USERPROFILE%\.eashell\eashell_data.json
      ├── runningProcesses ......... ConcurrentHashMap<scriptId, ProcessRunner>
      ├── executorService .......... bounded pool (8), daemon threads
      ├── TopBar ................... title, "Running: N" (1s Timer poll), NEW / STOP ALL
      ├── ScriptListPanel .......... left 40%: TitledPane per group → ScriptCard, id→card map
      └── OutputPanel .............. right 60%: TabPane, one tab per run, stdin field per tab
```

Two rules that govern every change here:

1. **`MainWindow` is the only mediator.** Components never call each other or the repository; they
   take `Consumer` / `Runnable` / `Supplier` callbacks in their constructors. Keep it that way — new
   coordination logic belongs in `MainWindow`, not in a component.
2. **`ScriptEntry.id` (a UUID) is the primary key, not `name`.** `equals`/`hashCode` are id-based, and
   both `scriptCards` and `runningProcesses` are keyed by id. `name` is display-only and never
   validated for uniqueness beyond the add/edit dialog's own check. Anything that constructs a new
   `ScriptEntry` for an *existing* script (edit flows, migrations) must carry the original id forward
   — minting a fresh one silently orphans whatever was keyed by the old one.

## Threading — read this before touching `ProcessRunner`

| Thread | Role |
| --- | --- |
| JavaFX Application Thread | the only thread allowed to touch widgets |
| Runner thread (per script) | `ProcessRunner.run()`: sequential command loop, blocks on `waitFor()` |
| Reader thread (per command) | drains the merged stdout+stderr stream |
| `java.util.Timer` in `TopBar` | 1 s poll of `runningProcesses.size()` — still polling, not yet converted to a bound property (`docs/IMPROVEMENTS.md` item 19) |

- Every UI mutation from a background thread goes through `Platform.runLater`. No exceptions.
- Output is throttled, never pushed raw: buffered in a `StringBuilder`, flushed when ≥100 ms elapsed
  **or** the buffer passes 4 KB, then the `TextArea` is truncated to the last 10 000 chars. All numbers
  live in `Constants.java` — that file is authoritative, the README's older figures were stale (now
  corrected).
- `ProcessRunner.running` is the cooperative cancellation flag, read by both the runner and the
  reader loop. Preserve the `if (!running) break;` checks when editing the command loop.
- `ProcessRunner` fires a `BiConsumer<String, Boolean> onStatusChange(scriptId, running)` from its
  `finally` block on every exit path, and exposes `sendInput(line)` for stdin. It still holds a
  `TextArea` and a `Tab` directly, though — that coupling is deliberate to note, not to imitate, see
  `docs/IMPROVEMENTS.md` item 14 for the intended inversion (still open).

## Conventions

- **Java 17**, UTF-8 sources, 4-space indent, no wildcard imports except where already present.
- **No FXML, no scene builder.** All UI is constructed programmatically. Don't introduce FXML for a
  single component — it would split the layout across two idioms.
- **All colours and CSS go through `src/main/resources/styles/app.css`.** Colors are custom
  properties on `.root`, referenced by every rule that needs them — never hardcode a hex value in a
  component class. Components apply looks via `node.getStyleClass().add("some-class")`, not
  `setStyle(...)`. `StyleManager.java` only holds style-class-name constants (named by role, e.g.
  `BTN_DANGER`, not by hue) plus the button/label factory methods. Hover and focus states are real
  CSS `:hover`/`:focused` rules, not JS listeners that replace the whole style string.
- **Dialogs need the stylesheet attached explicitly.** A `Dialog` owns its own `Scene` and does not
  inherit the main window's stylesheet — see `ScriptDialog.showDialog()` for the pattern
  (`dialogPane.getStylesheets().add(...)`). Forgetting this silently falls back to default Modena
  styling; it won't error.
- **All sizes, timeouts, labels and emoji go in `Constants.java`.** No magic numbers or literal UI
  strings in components.
- Doc comments in the UI package use ASCII box drawings of the widget tree. Match that style when
  adding a component — it is the codebase's house documentation format.
- Class-level comments are in English; `util/UI.md` and `util/style_guide/` are in Ukrainian (and
  describe the pre-CSS-migration styling approach — treat as historical design notes, not current
  mechanics). Keep new code comments in English.

## Gotchas that will bite

The four P0s found in the v2.0.0 verification pass (`\r` handling, `stop()` blocking the FX thread,
group re-run stacking dialogs, queued scripts shown as running) were all fixed in v2.0.1 — see
`docs/IMPROVEMENTS.md` items 23-26 for exactly what changed if you're touching that code again.

Still-open traps:

- **`getOutputAreaFromTab` indexes tab children positionally** (`get(0)`). Changing the tab's VBox
  child order (it's `[outputArea, stdin field, controlBox]` today) breaks output routing silently —
  `docs/IMPROVEMENTS.md` item 12.
- **The `process` field is reassigned per command** while the previous reader thread may still hold a
  reference, joined with only a 1 s timeout. A slow-draining reader can observe the next command's
  process — item 10.
- **Cleanup only runs from the window's close button** (`primaryStage.setOnCloseRequest`), not from
  `Platform.exit()` or an OS-level shutdown — item 13. `SingleInstanceLock` release doesn't depend on
  this though: OS file locks are released on process exit regardless of how it exits.
- **Version lives in three places** (`pom.xml`, Launch4j `versionInfo`, README) and none of them
  derive from another — item 17. Bump all three together.
- **`TopBar` polls every second forever**, even fully idle — item 19. Cheap today, but it's the thing
  to replace first if `runningProcesses` ever needs an observable count elsewhere.
- **Editing a script must carry its id forward** — see rule 2 above. `ScriptDialog.extractScriptEntry`
  is the one place this is currently handled correctly; don't add another `new ScriptEntry(...)` call
  for an edit path without doing the same.
- **`ProcessRunner.run()` fires `onStatusChange(id, true)` as its own first action** now (not
  `MainWindow` at submit time) - `MainWindow.handleRunScript` only marks the card *queued* before
  `executorService.submit(...)`. If you add another path that starts a runner, remember the card
  won't show "running" until `run()` itself says so.
- **"Running: N" still counts queued scripts**, not just actively-executing ones - it reads
  `runningProcesses.size()`, and a queued runner is already in that map. Known, not fixed — item 26.
- **Maven may not be on PATH here** — it exists only as a wrapper distribution under
  `~/.m2/wrapper/dists/`. `scripts/package.ps1` checks for it now and fails clearly if missing
  (item 30), but there's still no `mvnw`. The JDK at `C:\Program Files\Java\jdk-17` is a full JDK
  with `jpackage`/`jlink`.
- **Diagnosing a packaged build:** the jpackage launcher runs the JVM as a *child* process, so the
  process you started looks dead (5 threads, 8 MB, no window handle) while the real app runs beside
  it. `docs/PACKAGING.md` §3 has the correct check.

## Files not to touch

- `eashell_data.json` (repo root, legacy) and `%USERPROFILE%\.eashell\eashell_data.json` (current
  location) — the user's real script list with real local paths. Git-ignored. **Never read, edit,
  commit, or use its contents as example data.** If you need fixture data, invent it.
- `target/` — build output.
- `dependency-reduced-pom.xml` — stale leftover from a removed shade plugin; it does not affect the
  build. Delete it as its own change, don't edit it.
- `.idea/` — IDE state.

## Versioning

The version appears in three places and they drift: `pom.xml` `<version>`, the Launch4j `versionInfo`
block (four hardcoded fields), and the README's run command. When bumping, update all three, or
implement `docs/IMPROVEMENTS.md` item 17 to collapse them.

Commit messages in this repo follow `vX.Y.Z <short description>`.
