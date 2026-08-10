# EAShell — Improvement Backlog

Findings from a full read of the 1.2.1 source. Ordered by impact, with the exact call sites.
Context for every item is in [`ARCHITECTURE.md`](ARCHITECTURE.md); planned **new capabilities** live
in [`ROADMAP.md`](ROADMAP.md). This document is defects and technical debt only.

Two items here blocked roadmap features and were scheduled accordingly — both now fixed:
**item 5** blocked packaging ([`PACKAGING.md`](PACKAGING.md)) · **item 8** blocked script groups.

Legend: **P0** = user-visible defect · **P1** = correctness/robustness risk · **P2** = maintainability
· **P3** = polish / new capability.

## Status (as of v2.0.0)

| # | Item | Status |
| --- | --- | --- |
| 1 | Finished script never marked finished | ✅ Fixed — v1.2.3 |
| 2 | STOP leaves child processes alive | ✅ Fixed — v1.2.4 |
| 3 | Non-ASCII output mojibake | ✅ Fixed — v1.2.2 |
| 4 | Running indicators reset on list change | ✅ Fixed — v1.2.3 |
| 5 | Data file location CWD-relative | ✅ Fixed — v1.2.6 |
| 6 | Corrupt JSON prevents startup | ✅ Fixed — v1.2.6 |
| 7 | Saving not crash-safe | ✅ Fixed — v1.2.6 |
| 8 | Renaming orphans runner (name-based identity) | ✅ Fixed — v1.2.7 |
| 9 | No input validation in script dialog | ✅ Fixed — v1.2.7 |
| 10 | `process` reassigned while reader may still run | Open |
| 11 | `flushBuffer()` posts to FX thread while holding lock | Open |
| 12 | `getOutputAreaFromTab` depends on child order | Open |
| 13 | Cleanup only runs on window close button | Open |
| 14 | `ProcessRunner` coupled to JavaFX widgets | Open — blocks real `ProcessRunner` tests |
| 15 | Styling split between inline strings and synthetic stylesheet | ✅ Fixed — v1.2.9 |
| 16 | Palette constants misleading | ✅ Fixed — v1.2.9 |
| 17 | Version duplicated in three places | Open |
| 18 | Stale/empty build files, groupId typo | Open |
| 19 | `TopBar` polls once a second forever | Open |
| 20 | No real tests | Partially fixed — v1.2.6 added `ScriptEntry`/`ScriptRepository` coverage; `ProcessRunner` still blocked by item 14 |
| 21 | Drop shadows applied twice, cost frames | ✅ Fixed — v1.2.2 |
| 22 | No CI | Open |

---

## P0 — user-visible defects

### 1. A finished script is never marked as finished

> ✅ **Fixed in v1.2.3.**

`ProcessRunner.run()` ends by setting the tab title (`ProcessRunner.java:65`) and nothing else. The
`onStatusChange` callback that `MainWindow` supplies is wired only into `Tab.setOnClosed`
(`OutputPanel.java:110`).

Result: the card keeps its 🟢 dot, the entry stays in `MainWindow.runningProcesses`, "Running: N"
never decreases, and clicking RUN again shows *"Script is already running"* until the tab is closed
by hand.

**Fix:** give `ProcessRunner` a `BiConsumer<String, Boolean> onStatusChange` (or a simpler
`Runnable onFinished`) and call it in the `finally` block alongside `running = false`. `MainWindow`
already has the right handler — `updateScriptStatus`, which both repaints the dot and removes the map
entry. Keep the tab-close wiring; the handler is idempotent.

### 2. STOP leaves child processes alive

> ✅ **Fixed in v1.2.4.**

`ProcessRunner.stop()` calls `process.destroy()` on the shell only (`ProcessRunner.java:134`). A
PowerShell/`sh` wrapper dies, its children (`node`, `yt-dlp.exe`, dev servers) keep running and keep
holding ports and file handles.

**Fix:** walk the tree before killing the parent —

```java
process.descendants().forEach(ProcessHandle::destroy);
process.destroy();
if (!process.waitFor(PROCESS_STOP_TIMEOUT_SECONDS, SECONDS)) {
    process.descendants().forEach(ProcessHandle::destroyForcibly);
    process.destroyForcibly();
}
```

`ProcessHandle` is Java 9+, already available on the project's baseline of 17.

### 3. Non-ASCII output is mojibake on Windows

> ✅ **Fixed in v1.2.2.**

`new InputStreamReader(process.getInputStream())` (`ProcessRunner.java:76`) uses the JVM default
charset, while the Windows console emits the OEM code page. Ukrainian/Cyrillic output, box-drawing
characters and progress bars all render as garbage.

**Fix:** decode explicitly. Practical approach — read the console code page once
(`System.getProperty("sun.jnu.encoding")` is a good default on Windows, `UTF-8` elsewhere), expose it
as a constant, and pass it to the `InputStreamReader`. For PowerShell specifically, prefixing the
command with `[Console]::OutputEncoding=[Text.Encoding]::UTF8;` and decoding as UTF-8 is the more
predictable option.

### 4. Running indicators reset on any list change

> ✅ **Fixed in v1.2.3.**

`MainWindow.refreshScriptList()` → `ScriptListPanel.refresh()` clears and rebuilds every card
(`ScriptListPanel.java:100`), and fresh cards start stopped (`ScriptCard.java:110`). Add, edit or
delete anything while a script runs and all green dots vanish, though the processes are alive.

**Fix:** after `refresh()`, re-apply status from the authoritative source —
`runningProcesses.keySet().forEach(n -> scriptListPanel.updateScriptStatus(n, true))`. A cleaner
long-term answer is a `BooleanProperty` on `ScriptEntry`/card bound to the run registry.

### 5. Data file location depends on the launch directory · ⛔ blocks packaging

> ✅ **Fixed in v1.2.6.**

`Constants.DATA_FILE = "eashell_data.json"` is resolved against the current working directory. Launch
`EAShell.exe` from a shortcut, from Explorer, and from a terminal and you may get three different
(or empty) script lists. Silent data "loss" from the user's point of view.

**Fix:** resolve to a stable per-user location —
`Path.of(System.getProperty("user.home"), ".eashell", "eashell_data.json")` — and, on first run,
migrate an existing file from the CWD if one is found.

> This is a **hard prerequisite for shipping an installer**: an app under `C:\Program Files\` cannot
> write to its own directory, so every save would fail silently. See
> [`PACKAGING.md`](PACKAGING.md) §0.

---

## P1 — correctness and robustness

### 6. A corrupt JSON file prevents startup

> ✅ **Fixed in v1.2.6.**

`ScriptRepository.loadEntries()` catches `IOException` only (`ScriptRepository.java:72`).
`JsonSyntaxException` is unchecked, so it escapes the constructor, escapes `MainWindow`'s
constructor, and the window never appears — with no message for a GUI user.

**Fix:** catch `JsonSyntaxException` too; rename the bad file to `eashell_data.json.corrupt-<ts>`,
start with an empty list, and show an `Alert` explaining what happened.

### 7. Saving is not crash-safe

> ✅ **Fixed in v1.2.6.**

`save()` truncates the real file and streams Gson into it (`ScriptRepository.java:54`). Any failure
mid-write leaves a truncated, unparseable file — which then triggers item 6 on the next launch. There
is also no explicit charset on the `FileWriter` or on `Files.readAllBytes` decoding.

**Fix:** write UTF-8 to `eashell_data.json.tmp`, then
`Files.move(tmp, target, REPLACE_EXISTING, ATOMIC_MOVE)`.

### 8. Renaming a running script orphans its runner · ⛔ blocks script groups

> ✅ **Fixed in v1.2.7.**

`runningProcesses` is keyed by name (`MainWindow.java:228`). Rename via EDIT while it runs and the
key no longer matches any card: the process can't be stopped from its card, and the counter is stuck
until `STOP ALL`.

**Fix:** give `ScriptEntry` a stable `id` (UUID, generated on creation, persisted) and key every map
— `runningProcesses`, `scriptCards` — by id instead of name. Gson deserialises a missing `id` as
`null`, so backfill on load.

> Groups make duplicate names across groups a normal occurrence, which turns this latent collision
> into a routine one. Do this **before** [`ROADMAP.md`](ROADMAP.md) §1.

### 9. No input validation in the script dialog

> ✅ **Fixed in v1.2.7.**

`ScriptDialog.extractScriptEntry()` (`:104`) accepts an empty name, an empty path, a non-existent
directory, and a name that duplicates an existing script — all get persisted. Duplicates then collide
in `scriptCards`, `runningProcesses`, and `ScriptEntry.equals`.

**Fix:** disable the OK button until name and path are non-empty (bind to
`dialogPane.lookupButton(ButtonType.OK).disableProperty()`), verify `Files.isDirectory(path)`, and
reject a name already present in the repository (except when editing that same entry).

### 10. `process` is reassigned while the previous reader may still run

The loop overwrites the `process` field per command (`ProcessRunner.java:50`) and joins the reader
with a 1 s cap (`:57`). A reader still draining a large tail can then read the *next* command's
stream.

**Fix:** make `readProcessOutput` take the `Process` (or its `InputStream`) as a parameter instead of
reading the mutable field; make `process` `volatile`, likewise `lastUIUpdate`.

### 11. `flushBuffer()` posts to the FX thread while holding the buffer lock

`Platform.runLater` is called inside `synchronized (outputBuffer)` (`ProcessRunner.java:107–120`).
Harmless today, but it means the lock is held across a queue operation on a hot path.

**Fix:** copy and clear the buffer inside the lock, then post outside it.

### 12. `getOutputAreaFromTab` depends on child order

`((VBox) tab.getContent()).getChildren().get(0)` (`OutputPanel.java:162`). Adding a header, a search
bar or a status strip to the tab silently redirects all output into a `ClassCastException`.

**Fix:** return the `TextArea` alongside the `Tab` (a small record `TabHandle(Tab tab, TextArea area)`),
or store it in the tab's properties map. Removing this method also lets `createOutputTab` construct a
fully-initialised `ProcessRunner`, eliminating the null-then-set dance in
`MainWindow.handleRunScript()` (`:209–226`).

### 13. Cleanup only runs on the window's close button

`primaryStage.setOnCloseRequest(e -> cleanup())` (`MainWindow.java:133`) does not fire on
`Platform.exit()` or an OS-level shutdown of the app.

**Fix:** override `Application.stop()` in `App` and route cleanup there (it runs for every normal
shutdown path).

---

## P2 — maintainability

### 14. `ProcessRunner` is coupled to JavaFX widgets

It holds a `TextArea` and a `Tab` and calls `Platform.runLater` directly. This is why the service
layer cannot be tested headlessly and why `AppTest` asserts `true`.

**Fix:** invert it — `ProcessRunner(ScriptEntry entry, Consumer<String> output, BiConsumer<String,
Status> onStatus)`. The UI supplies sinks that marshal onto the FX thread. Then `ProcessRunner` is a
plain `Runnable` testable with `echo`/`cmd /c` fixtures.

### 15. Styling is split between inline strings and a synthetic stylesheet

> ✅ **Fixed in v1.2.9.**

Hover handlers replace the whole style string (`ScriptCard.java:173`, `StyleManager.java:181`), so any
other style on the node is lost. `getStylesheet()` assembles a `data:text/css,` URI by concatenation
(`StyleManager.java:343`) with no syntax checking, no IDE support, and no reload.

**Fix:** move rules into `src/main/resources/styles/app.css` with real style classes
(`.script-card`, `.script-card:hover`, `.btn-danger`, …), load it with
`scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm())`, and replace
`setStyle` calls with `getStyleClass().add(...)`. Keep `StyleManager` only as a place for the palette
and the button factories.

### 16. Palette constants no longer mean what they say

> ✅ **Fixed in v1.2.9.**

`ACCENT_GREEN = "#B794D4"` (purple), `ACCENT_BLUE = "transparent"`, `SECONDARY_BUTTON = ""`, and many
hex values are hardcoded inside method bodies (`#7DD3E8`, `#2D1B3D`, `#E291B5`) instead of referencing
constants. `getOutputAreaStyle()` sets `-fx-text-fill` twice and carries a `FIXME` at
`StyleManager.java:317`.

**Fix:** rename by role, not by hue — `ACCENT_PRIMARY`, `ACCENT_DANGER`, `SURFACE_RAISED`,
`TEXT_CONSOLE` — and route every literal through them. Do this together with item 15.

### 17. Version is duplicated in three places

`pom.xml` `<version>1.2.1</version>`, the Launch4j `versionInfo` block (`fileVersion`,
`txtFileVersion`, `productVersion`, `txtProductVersion` — all hardcoded `1.2.1`), and the README's
`java -jar target/EAShell-1.1.1.jar` (already stale by two releases).

**Fix:** use `${project.version}` in the Launch4j configuration, and reference
`target/EAShell-${version}.jar` generically in the README.

### 18. Stale and empty build files

`dependency-reduced-pom.xml` is a leftover from a removed `maven-shade-plugin`; `.mvn/jvm.config` and
`.mvn/maven.config` are both empty. The pom's `groupId` is `com.ealshell` while every package is
`com.eashell` — a typo that will surface the moment the artifact is published anywhere.

**Fix:** delete the reduced pom (and git-ignore it), drop or populate the `.mvn` files, and fix the
groupId.

### 19. `TopBar` polls once per second forever

`java.util.Timer` + `Platform.runLater` (`TopBar.java:100`) wakes the app every second even when idle.

**Fix:** an `IntegerProperty runningCount` on `MainWindow`, updated where the map is mutated, with
`statusLabel.textProperty().bind(runningCount.asString("Running: %d"))`. Zero polling, always exact.
Failing that, a JavaFX `Timeline` at least stays on the FX toolkit's own scheduler.

### 20. No real tests

> **Partially fixed in v1.2.6.** `ScriptEntryTest`/`ScriptRepositoryTest` now cover the first two
> steps below. `ProcessRunner` is still blocked on item 14.

`AppTest.shouldAnswerWithTrue` is the Maven archetype placeholder. JUnit 5 is already configured.

**Fix, in dependency order:** `ScriptEntry` equality/serialisation → `ScriptRepository` round-trip and
corrupt-file handling (`@TempDir` + an injectable data path, which item 5 introduces anyway) →
`ProcessRunner` command loop after item 14. UI smoke tests would need TestFX; not worth it yet.

---

## P3 — polish

### 21. Drop shadows are applied twice and cost frames

> ✅ **Fixed in v1.2.2.**

`ScriptCard.initializeCard()` attaches a `DropShadow` via `setEffect()` (`ScriptCard.java:70–72`)
while `StyleManager.getCardStyle()` separately sets `-fx-effect: dropshadow(...)`. Inline styles
outrank setter values in JavaFX, so the green shadow is almost certainly never rendered — and the
remaining Gaussian blur is recomputed per card during scrolling.

**Fix:** delete the `setEffect` block, add `setCache(true); setCacheHint(CacheHint.SPEED);` to each
card. Details and the accompanying wheel-speed fix in [`ROADMAP.md`](ROADMAP.md) §4.

### 22. No CI

A GitHub Actions workflow running `mvn -B verify` on push would catch nothing today (see item 20),
but it is the prerequisite for the tests above to matter.

---

## New features

Everything the app does not do yet — script groups, stdin, packaging without a Java requirement,
console output quality, convenience items, and the list of things deliberately **not** being built —
is designed in [`ROADMAP.md`](ROADMAP.md).

---

## Suggested order of work

This list covered defects only; the merged sequence that interleaved these fixes with the roadmap
features (and respected the blocking dependencies) was [`ROADMAP.md`](ROADMAP.md) §8, now complete
through step 9. What's below is the original plan, kept for history — items 1–9, 15, 16, 21 (and
partially 20) are done; 10–14, 17–19, 22 are not, and weren't blocking anything else.

1. Items **1, 4** — smallest diffs, most visible defects. One commit. ✅ done (v1.2.3)
2. Items **2, 3** — process control and encoding; both live inside `ProcessRunner.java`. ✅ done (v1.2.4, v1.2.2)
3. Items **5, 6, 7** — data safety, all inside `ScriptRepository` + `Constants`. Add tests here first. ✅ done (v1.2.6)
4. Items **12, 14** — decouple `ProcessRunner` from the UI. This is the refactor that unlocks testing. **Not done** — turned out not to be a hard prerequisite for items 8/9 after all; see below.
5. Items **8, 9** — identity and validation. ✅ done (v1.2.7) — done without waiting on item 14, since
   the actual blocker for stable identity was giving `ScriptEntry` a UUID, not decoupling `ProcessRunner`.
6. Items **15, 16** — the CSS migration. Self-contained; can proceed in parallel with everything above. ✅ done (v1.2.9)
