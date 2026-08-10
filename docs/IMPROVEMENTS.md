# EAShell — Improvement Backlog

Findings from a full read of the 1.2.1 source. Ordered by impact, with the exact call sites.
Context for every item is in [`ARCHITECTURE.md`](ARCHITECTURE.md); planned **new capabilities** live
in [`ROADMAP.md`](ROADMAP.md). This document is defects and technical debt only.

Two items here blocked roadmap features and were scheduled accordingly — both now fixed:
**item 5** blocked packaging ([`PACKAGING.md`](PACKAGING.md)) · **item 8** blocked script groups.

Legend: **P0** = user-visible defect · **P1** = correctness/robustness risk · **P2** = maintainability
· **P3** = polish / new capability.

Items **1–22** come from the original 1.2.1 review. Items **23–32** were found during the
[v2.0.0 verification pass](#verification-pass-v200) — regressions and gaps in the fixes
themselves. All ten were fixed in v2.0.1; see the status table for exact versions.

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
| 23 | `\r` discard flag lost when a newline follows in the same chunk | ✅ Fixed — v2.0.1 |
| 24 | `stop()` blocks the FX thread up to 2 s per script | ✅ Fixed — v2.0.1 |
| 25 | Re-running a group stacks N modal "already running" dialogs | ✅ Fixed — v2.0.1 |
| 26 | Queued scripts are shown and counted as running | ✅ Fixed — v2.0.1 |
| 27 | Tab ends as "✓ success" after STOP | ✅ Fixed — v2.0.1 |
| 28 | Scroll handler doesn't consume the event (double scroll) | ✅ Fixed — v2.0.1 |
| 29 | Test-scope jars ship inside the app-image | ✅ Fixed — v2.0.1 |
| 30 | `package.ps1` assumes `mvn` is on PATH | ✅ Fixed — v2.0.1 (clear error instead of a confusing failure; still no `mvnw`) |
| 31 | Stale comment: `runningProcesses` keyed by name | ✅ Fixed — v2.0.1 |
| 32 | A script with zero commands passes validation | ✅ Fixed — v2.0.1 |

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

## Verification pass (v2.0.0)

A full re-read of the source against this document plus a real build, test run and packaging run.

### What was confirmed working

| Check | Result |
| --- | --- |
| `mvn -B clean test` | BUILD SUCCESS — 14/14 tests (`AppTest` 1, `ScriptEntryTest` 7, `ScriptRepositoryTest` 6) |
| `mvn -B clean package` | jar + staged `target/libs` + Launch4j `EAShell.exe` |
| `jpackage --type app-image` | `target/dist/EAShell/` — 74 MB |
| **App-image with no system Java** | **Window "EA Shell" opened; 99 threads, 153 MB RSS** |
| User data safety during the test | `~/.eashell/eashell_data.json` hash identical before and after |
| CSS migration completeness | No `setStyle(`, no hex literal and no `-fx-` string left in `src/main/java` (one comment aside) |

The JDK-less run used an empty `JAVA_HOME` and `PATH=C:\Windows\system32;C:\Windows`, verified with
`Get-Command java` returning nothing to the child process. **Items 1–9, 15, 16 and 21 were verified
in the source, not just taken from the status table** — including that editing a script preserves its
id, which is the load-bearing part of item 8.

One diagnostic subtlety worth recording, because it will mislead the next person too: the jpackage
Windows launcher runs the JVM as a **child** process. `Start-Process -PassThru` returns the parent
stub — 5 threads, 8 MB, `MainWindowHandle = 0` — which looks exactly like a failed launch. Enumerate
all processes by name and check the one with a non-zero window handle.

### 23. `\r` handling loses the discard flag when a newline follows · P0

> ✅ **Fixed in v2.0.1.** Removed the `discardPendingAreaLine = false;` line from the `\n`
> branch, per the fix below. Manually verified with a real progress-bar-style PowerShell loop.

`ProcessRunner.java:127` — in the `\n` branch of `bufferOutput`, `discardPendingAreaLine = false;`
erases the pending "delete the partial line already in the TextArea" request that a `\r` earlier in
the *same chunk* just set. The old progress line is then never removed.

Reproduced by replaying the exact algorithm with the `TextArea` swapped for a `StringBuilder` and one
flush per chunk (what the 100 ms timer does in practice):

```
input   : "[download]  10%", "\r[download]  50%", "\r[download] 100%\n", "done\n"
expected: [download] 100%\ndone\n
actual  : [download]  50%[download] 100%\ndone\n     <-- WRONG
```

```
input   : "first\n", "aaa", "\rbbb\n"
expected: first\nbbb\n
actual  : first\naaabbb\n                            <-- WRONG
```

So progress-bar output (yt-dlp, npm, pip) still leaves duplicated lines — the exact symptom
[`ROADMAP.md`](ROADMAP.md) §5.1 was written to remove. Two of three test cases fail.

**Fix:** delete the `discardPendingAreaLine = false;` line. Resetting on `\n` is not needed for
correctness: after a newline `currentLineStartInBuffer > 0`, so a later `\r` cannot set the flag
again, and `flushBuffer()` already clears it when it consumes it. Both failing cases pass with the
line removed and the passing case stays passing.

### 24. `stop()` blocks the JavaFX thread for up to 2 s per script · P0

> ✅ **Fixed in v2.0.1.** The destroy-and-wait sequence now runs on a small daemon thread
> spawned by `stop()`, which returns immediately; `running = false` is still set synchronously.
> Manually verified: STOP ALL on multiple long-running scripts no longer freezes the window.

`ProcessRunner.stop()` calls `process.waitFor(PROCESS_STOP_TIMEOUT_SECONDS, SECONDS)`
(`ProcessRunner.java:220`), and every caller is on the FX thread: the tab's STOP button
(`OutputPanel.java:153`), `Tab.setOnClosed` (`OutputPanel.java:124`) and `MainWindow.handleStopAll`
(`MainWindow.java:259`), which `cleanup()` also calls on window close.

The defect pre-dates v2.0.0, but item 2 (descendant enumeration) and the bounded pool of 8 made it
much worse: **STOP ALL, or closing the window, can freeze the UI for up to ~16 seconds** when
processes don't die promptly.

**Fix:** do the destroy-and-wait off the FX thread — hand `stop()` to the executor (or a small
single-thread stopper) and let the existing `onStatusChange` callback update the UI when it
completes. The `running = false` flag can still be set synchronously so the UI reacts immediately.

### 25. Re-running a group stacks N modal dialogs · P0

> ✅ **Fixed in v2.0.1.** `ScriptListPanel` now takes a separate `onRunGroup` callback;
> `MainWindow.handleRunGroup` filters out already-running/queued entries before looping, so the
> per-card warning never fires from a group click. Manually verified.

`ScriptListPanel.createGroupHeader` runs a group with `groupEntries.forEach(onRun)`
(`ScriptListPanel.java:162`), and `MainWindow.handleRunScript` answers an already-running script with
a blocking `DeleteConfirmDialog.showAlreadyRunning()` (`MainWindow.java:216`). Running a group of 10
where 5 are already going means clicking through 5 modal dialogs in a row.

**Fix:** make group-run skip already-running entries silently, or collect them into a single summary
alert. The simplest version is a separate code path that filters
`!runningProcesses.containsKey(id)` before looping — the per-card RUN button keeps its current
warning, which is still the right behaviour for an explicit single click.

### 26. Queued scripts are shown and counted as running · P1

> ✅ **Fixed in v2.0.1** for the card indicator: a new 🟡 "queued" state
> (`ScriptListPanel.updateScriptQueuedStatus`) is applied at submit time, and `ProcessRunner.run()`
> fires the real `onStatusChange(id, true)` as its first action once a thread actually picks it
> up. **Not fixed:** "Running: N" still counts queued entries (it reads `runningProcesses.size()`,
> and a queued runner is still in that map) - out of scope for the minimal version of this fix.

With `newFixedThreadPool(MAX_CONCURRENT_SCRIPTS)` (8), `handleRunScript` puts the runner in
`runningProcesses` and calls `updateScriptStatus(id, true)` **before** `executorService.submit`
(`MainWindow.java:241–248`). Script 9 and beyond therefore show 🟢, inflate "Running: N", and open a
tab titled 🟢 while they are merely queued.

Two follow-on effects: `stop()` on a queued runner does nothing visible, because
`process == null` skips the whole body — no tab update, no `onStatusChange`; and when the queue does
reach it, `running` is already `false`, so the loop breaks immediately and it prints
`>>> All commands completed.` with a ✓ despite never having executed a command.

**Fix:** introduce a third state (queued) rather than reusing "running" — a distinct indicator and a
tab title that says so. Minimum viable version: have `ProcessRunner.run()` fire
`onStatusChange(id, true)` as its first action, and have `handleRunScript` mark the card queued
instead of running.

### 27. After STOP the tab title ends as "✓ success" · P1

> ✅ **Fixed in v2.0.1.** The completion block is now guarded with `if (running)`. Manually verified.

`stop()` posts `tab.setText(name + STATUS_TERMINATED)` (`ProcessRunner.java:232`), but the runner
thread then leaves the command loop, falls through to `appendOutput(">>> All commands completed.")`
and posts `tab.setText(name + STATUS_SUCCESS)` (`ProcessRunner.java:79–80`). The later post wins, so
a deliberately stopped script reports success.

**Fix:** guard the completion block with `if (running)`, and emit the "all commands completed"
line/title only on a natural finish.

### 28. The custom scroll handler doesn't consume the event · P2

> ✅ **Fixed in v2.0.1** — added `e.consume();`.

`ScriptListPanel.java:88` adjusts `scrollPane.setVvalue(...)` on `setOnScroll` but never calls
`e.consume()`, so the event keeps bubbling to the `ScrollPane`, whose own skin handler scrolls again.
The effective speed is `SCROLL_SPEED_FACTOR + 1`, not `SCROLL_SPEED_FACTOR`.

**Fix:** add `e.consume();` — then the constant means what it says.

### 29. Test-scope jars ship inside the app-image · P2

> ✅ **Fixed in v2.0.1** — added `<includeScope>runtime</includeScope>`. Verified: a clean
> `mvn package` no longer copies any `junit-*`/`opentest4j`/`apiguardian-api` jar into `target/libs`.

`maven-dependency-plugin:copy-dependencies` does **not** filter by scope unless told to. Despite
being declared `<scope>test</scope>`, the JUnit stack lands in `target/libs`, inside the app-image,
and on the runtime classpath in `app/EAShell.cfg`:

```
junit-jupiter-params  578 KB
junit-jupiter-api     211 KB
junit-platform-commons 137 KB
opentest4j             14 KB
apiguardian-api         7 KB   → 947 KB total
```

**Fix:** add `<includeScope>runtime</includeScope>` to the `copy-dependencies` configuration in
`pom.xml`. See [`PACKAGING.md`](PACKAGING.md) §2.

### 30. `package.ps1` assumes `mvn` is on PATH · P3

> ✅ **Fixed in v2.0.1**, partially: the script now checks `Get-Command mvn` first and fails with
> a clear message instead of a confusing "mvn not recognized" error. A `mvnw`/`mvnw.cmd` wrapper
> (the more complete fix) is still not done.

The script calls `mvn clean package` directly. On this machine Maven is not on PATH at all — it only
exists as a wrapper distribution under `~/.m2/wrapper/dists/`. The script already resolves `jpackage`
carefully and errors clearly when it is missing; `mvn` deserves the same treatment, or the project
should carry a `mvnw`/`mvnw.cmd` wrapper so the build is reproducible without a system Maven.

### 31. Stale comment: `runningProcesses` keyed by name · P3

> ✅ **Fixed in v2.0.1.**

`MainWindow.java:59` still reads `Map of active processes: script_name -> ProcessRunner`. It has been
keyed by `ScriptEntry.getId()` since v1.2.7, and the id-vs-name distinction is exactly the thing a
reader must not get wrong here.

### 32. A script with zero commands passes validation · P3

> ✅ **Fixed in v2.0.1** — added the `!commands.isEmpty()` check plus a listener on
> `commandsArea.textProperty()`. Manually verified: OK stays disabled with a blank commands box.

`ScriptDialog.wireValidation` checks name, path and name-uniqueness (`ScriptDialog.java:74–89`) but
not the command list. A script with an empty commands box saves fine and, when run, immediately
reports `>>> All commands completed.` with a ✓ having done nothing.

**Fix:** include `!commands.isEmpty()` in the validation predicate. It needs a listener on
`commandsArea.textProperty()` too, which is currently not wired.

### Suggested order for these

All ten shipped together in v2.0.1, in roughly this order:

1. Items **23, 27, 28** — roughly five lines total, and they fix the most visible wrong behaviour.
2. Item **29** — one line in `pom.xml`, before the next release is packaged.
3. Item **25** — small and self-contained.
4. Items **24, 26** — these needed a design decision (moving `stop()` off the FX thread; introducing
   a queued state) — see their entries above for what shipped vs. what's still open (item 26's
   "Running: N" counter).
5. Items **30, 31, 32** — housekeeping.

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
