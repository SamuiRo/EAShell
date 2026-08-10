# EAShell — Feature Roadmap

Planned capabilities, with design decisions already made, effort, and the traps found during
analysis. Defects and technical debt live separately in [`IMPROVEMENTS.md`](IMPROVEMENTS.md);
the packaging runbook is [`PACKAGING.md`](PACKAGING.md).

**Guiding constraint for every item here:** the application must stay small, fast and obvious.
An item that doubles the complexity of the codebase to serve a narrow scenario does not belong in
this document — see [§7 Explicitly out of scope](#7-explicitly-out-of-scope).

**Status as of v2.0.1:** §1–§4 done. §5: 5.1/5.2 done, 5.3/5.5 not done, 5.4 still deliberately
deferred. §6: single-instance lock done, the rest not done. See §8's table for the full picture.

**The v2.0.0 verification pass** found that three shipped features had defects undercutting them
(§5.1 didn't actually remove duplicate progress lines, §4.1 scrolled twice, §1's group-run and
bounded pool produced stacked modal dialogs and phantom "running" indicators) — **all fixed in
v2.0.1**. See [`IMPROVEMENTS.md`](IMPROVEMENTS.md) items 23–32 for exactly what changed; one gap
remains open there by design choice, not oversight (item 26's "Running: N" still counts queued
scripts).

---

## 1. Script groups / folders

> ✅ **Implemented in v1.2.8**, following the design below almost exactly — one deviation: expanded/
> collapsed state is not yet persisted (still §6 below), so `ScriptListPanel.refresh()` currently
> re-expands every group on any list change.
>
> ✅ The v2.0.0 verification pass found two consequences the traps below did not anticipate, both
> arising from group-run meeting the bounded pool: re-running a group popped one **blocking modal
> dialog per already-running script** ([`IMPROVEMENTS.md`](IMPROVEMENTS.md) item 25, fixed in
> v2.0.1 via a separate `onRunGroup` callback that filters already-running/queued entries), and
> scripts **queued** behind the pool limit were displayed and counted as running (item 26, the card
> indicator fixed in v2.0.1 with a distinct 🟡 queued state - the "Running: N" counter still counts
> queued scripts, left as a known gap rather than fixed). Trap 1 below correctly called for bounding
> the pool; it just didn't originally follow through to what the UI should show once runs start
> queueing.

**Verdict: yes — cheapest feature of the set.** Do it as a flat field, never as a tree.

### Data model

```java
public class ScriptEntry {
    private String name;
    private String group;       // null → rendered as "Ungrouped"
    private String workingDir;
    private List<String> commands;
}
```

Gson deserialises a missing field to `null`, so existing `eashell_data.json` files load unchanged.
No migration step, no version field.

### Rendering

Grouping is a **view concern only** — nothing changes in `ScriptRepository` or `ProcessRunner`.
In `ScriptListPanel.refresh()`:

```java
Map<String, List<ScriptEntry>> byGroup = entries.stream()
    .collect(Collectors.groupingBy(e -> e.getGroup() == null ? "Ungrouped" : e.getGroup(),
                                   TreeMap::new, Collectors.toList()));
```

Render one `TitledPane` per group into the existing container. `TitledPane` is built into JavaFX and
brings collapsing, animation and styling for free — do not hand-roll a collapsible header. Persist
the expanded/collapsed state alongside window geometry (§6).

Use `Accordion` **only** if you want exactly one group open at a time. For this app that is the
wrong behaviour — a plain `VBox` of `TitledPane`s is correct.

### Dialog

In `ScriptDialog`, an editable `ComboBox<String>` populated from the distinct groups already in the
repository: pick an existing group or type a new one. No group-management UI, no rename dialog, no
drag-and-drop. A group exists exactly as long as some script references it.

### Running a whole group

A ▶ button in the `TitledPane` header that loops over the group's entries and calls the existing
`MainWindow.handleRunScript`. **No new execution logic at all** — this is why the feature is cheap.

### Traps

1. **Unbounded thread pool.** `Executors.newCachedThreadPool` (`MainWindow.java:75`) has no ceiling.
   A group of 20 scripts spawns 20 threads, 20 OS processes and 20 tabs at once. Switch to a bounded
   pool (`newFixedThreadPool(8)` with a daemon `ThreadFactory`) and/or confirm before launching a
   group above a threshold. Do this **in the same change**, not later.
2. **Name is still the primary key.** `runningProcesses` and `scriptCards` are keyed by script name
   (`MainWindow.java:228`, `ScriptListPanel.java:114`). Groups make duplicate names across groups a
   normal occurrence, and they will collide silently. **[`IMPROVEMENTS.md`](IMPROVEMENTS.md) item 8
   (stable UUID identity) is a hard prerequisite, not a nice-to-have.**
3. **Parallel vs sequential.** Default to parallel — it is just a loop. Sequential group execution
   (`build` → `test` → `deploy`) needs a coordinator that chains runners and aborts the chain on a
   non-zero exit code. Add it only if a real need appears; it is a separate, larger feature.

**Effort:** ~150 lines. **Risk:** low, once item 8 is done.

---

## 2. Interactive input (stdin)

> ✅ **Implemented in v1.2.5**, matching the design below (writer created/closed per command, local
> echo, shared `CONSOLE_CHARSET`). Manually verified against a `Read-Host` prompt. Traps 4 and 5
> below remain real limitations — not fixed, by design (trap 4) or not yet (trap 5).

**Verdict: yes.** The channel already exists in `ProcessBuilder` — it is simply never used.

### Implementation

In `ProcessRunner`, after `pb.start()`:

```java
writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), CONSOLE_CHARSET));

public void sendInput(String line) throws IOException {
    writer.write(line);
    writer.write(System.lineSeparator());
    writer.flush();                       // без flush процес просто висить
}
```

The writer is per-process, so it is created and closed inside the command loop, not once per runner.

UI: a `TextField` under the `TextArea` in each console tab, `setOnAction` sending on Enter. Disable
it while no process is alive.

### Traps

1. **`flush()` is mandatory.** `BufferedWriter` holds the line; the child blocks forever waiting for
   input that never arrives. This is the single most common mistake with this API.
2. **Local echo.** The child process does not send typed input back on stdout. Append it to the
   `TextArea` yourself (prefixed, e.g. `>>> `), otherwise the user cannot see what they typed.
3. **Charset, again.** An `OutputStreamWriter` without an explicit charset breaks non-ASCII input
   exactly the way [`IMPROVEMENTS.md`](IMPROVEMENTS.md) item 3 breaks output. Fix both directions in
   one change and share one `CONSOLE_CHARSET` constant.
4. **Not every program reads stdin.** Tools that talk to the raw console handle (`pause` in cmd,
   full-screen TUIs, some password prompts) ignore the pipe and will hang regardless. This cannot be
   fixed without a terminal emulator, and a terminal emulator is out of scope (§7). Document the
   limitation in the README instead.
5. **PowerShell swallows stdin.** `powershell.exe -Command "<cmd>"` sometimes consumes the pipe
   before the child sees it. `cmd.exe /c` is more transparent for interactive scripts — which argues
   for making the shell a per-script field (§6).

**Effort:** ~60 lines. **Risk:** low.

---

## 3. Ship without requiring Java on the target machine

> ✅ **Implemented in v1.2.10** — Level 1 (app-image) and Level 2 (trimmed `--add-modules`) from
> `PACKAGING.md`, via `scripts/package.ps1`. **Not done:** Level 3 (jlink) and Level 4 (MSI — needs
> WiX Toolset, not installed on the dev machine).
>
> ✅ **The no-Java claim is now verified** (v2.0.0 verification pass): the app-image was launched with
> an empty `JAVA_HOME` and `PATH=C:\Windows\system32;C:\Windows`, with `java` confirmed invisible to
> the child process. The window opened and the app ran normally (99 threads, 153 MB). Output is
> 74 MB. A clean VM would additionally rule out unrelated system dependencies, but the bundled
> runtime is doing its job. The 947 KB of test-scope jars that were shipping alongside it are gone
> as of v2.0.1 ([`IMPROVEMENTS.md`](IMPROVEMENTS.md) item 29, `<includeScope>runtime</includeScope>`).

**Verdict: yes, and this is the highest-value item.**

Today `EAShell.exe` is a Launch4j wrapper around the JAR that **requires a system JRE 17+**
(`pom.xml:194` → `%JAVA_HOME%;%PATH%`). On a machine without a JDK it does not start. It is also not
a fat JAR — it is a JAR plus a sibling `libs/` directory.

The fix is `jpackage`, which is **built into JDK 17** — nothing to install for the basic path.

Full step-by-step recipe, pom changes, the `Launcher` class workaround, runtime trimming and the
WiX-based MSI installer: **[`PACKAGING.md`](PACKAGING.md)**.

### The one thing that must happen first

`Constants.DATA_FILE` is resolved against the current working directory. An application installed
under `C:\Program Files\` **cannot write to its own directory**. Every save silently fails.

**[`IMPROVEMENTS.md`](IMPROVEMENTS.md) item 5 (move the data file to `%USERPROFILE%\.eashell\`) is a
blocker for this feature.** Do it before packaging, not after — otherwise the first installed build
loses the user's scripts.

**Effort:** half a day for a working app-image; a further half day for a trimmed runtime and MSI.
**Risk:** medium — mostly build plumbing, no application logic.

---

## 4. Scroll performance in the script list

> ✅ **Implemented in v1.2.2** — both 4.1 and 4.2 below, plus 4.2's own suggested verification (the
> green shadow was confirmed dead code and removed, not just commented out).
>
> One gap found in the v2.0.0 verification pass, fixed in v2.0.1: the handler didn't call
> `e.consume()`, so the `ScrollPane` scrolled a second time on its own and the effective speed was
> `SCROLL_SPEED_FACTOR + 1` — [`IMPROVEMENTS.md`](IMPROVEMENTS.md) item 28.

**Verdict: yes, and the cause is two separate problems.**

### 4.1 Wheel step is too small

`ScrollPaneSkin` derives the mouse-wheel increment from the content height, so the more cards there
are, the less each notch moves. This is standard JavaFX behaviour, not a bug in this app.

Five lines in `ScriptListPanel`:

```java
scriptListContainer.setOnScroll(e -> {
    double delta = e.getDeltaY() * Constants.SCROLL_SPEED_FACTOR;   // 3–5
    scrollPane.setVvalue(scrollPane.getVvalue() - delta / scriptListContainer.getHeight());
});
```

### 4.2 Drop shadows are recomputed on every frame

`ScriptCard.initializeCard()` attaches a `DropShadow` via `setEffect()` (`ScriptCard.java:70–72`),
and `StyleManager.getCardStyle()` **separately** sets `-fx-effect: dropshadow(...)`. Gaussian blur is
recomputed per card while scrolling, and it is the expensive part.

Two consequences:

- In JavaFX an inline style set through `setStyle` outranks a value set through the corresponding
  setter, so the green shadow at `ScriptCard.java:70` is almost certainly **never visible**. Verify
  by commenting it out — the appearance should not change. If it doesn't, delete it.
- Add `setCache(true); setCacheHint(CacheHint.SPEED);` to each card. The card is rasterised once and
  scrolling stops re-running the blur. This is the actual fix.

### 4.3 If the list ever exceeds ~50 scripts

Replace `ScrollPane` + `VBox` with a `ListView` and a custom `cellFactory`. That brings virtualisation
(only visible cards are constructed), correct wheel scrolling and keyboard navigation for free — but
it is a rewrite of `ScriptListPanel` and interacts with the grouping design in §1. Not needed below
~50 cards; revisit if the number grows.

**Effort:** ~15 lines for 4.1 + 4.2. **Risk:** none.

---

## 5. Console output quality

Small changes, disproportionate effect on daily use.

> ✅ 5.1 and 5.2 **implemented in v1.2.2**. The v2.0.0 verification pass found 5.1 didn't actually
> work — duplicated progress lines in two of three reproduced cases — fixed in v2.0.1
> ([`IMPROVEMENTS.md`](IMPROVEMENTS.md) item 23), manually re-verified against a real progress-bar
> loop. 5.3 and 5.5 **not done**. 5.4 remains deliberately deferred.

### 5.1 Carriage returns (`\r`)

Progress-bar tools (yt-dlp, npm, pip, curl) overwrite the current line with `\r`. `TextArea` has no
concept of that, so one progress bar becomes thousands of lines — this is likely the most visible
output problem in real use.

Handle it in `ProcessRunner.bufferOutput`: when a chunk contains `\r` not followed by `\n`, replace
the last line instead of appending. Keep the logic in the buffer, not in the UI.

### 5.2 ANSI escape sequences

Coloured output renders as literal `[32m` garbage. Two options; take the second:

- Strip them: `text.replaceAll("\u001B\\[[;\\d]*m", "")` — costs a regex per flush.
- **Prevent them:** `pb.environment().put("NO_COLOR", "1")` and optionally `TERM=dumb`. Zero
  processing, respected by most modern CLI tools.

Do **not** implement ANSI colour rendering (§7).

### 5.3 Auto-scroll lock

`flushBuffer` calls `outputArea.setScrollTop(Double.MAX_VALUE)` on every flush
(`ProcessRunner.java:119`), which yanks the view back down every 100 ms and makes reading earlier
output impossible. Track whether the user has scrolled away from the bottom and suppress the
auto-scroll until they return.

### 5.4 Distinguish stderr

`pb.redirectErrorStream(true)` (`ProcessRunner.java:49`) merges the streams, so errors are visually
indistinguishable. Reading them separately and colouring stderr requires replacing `TextArea` with
`TextFlow`, which loses selection and copy behaviour. **Deferred** — the cost is higher than it
first appears.

### 5.5 Tab reuse

Tabs accumulate without limit. One tab per script — re-running clears and reuses it — removes most
of the reasons to close tabs manually, and interacts well with the "already running" check.

**Effort:** 5.1 ~40 lines, 5.2 one line, 5.3 ~20 lines, 5.5 ~30 lines. **Risk:** low.

---

## 6. Convenience items

Ordered by value per line of code.

| Item | Status | Notes |
| --- | --- | --- |
| **Single-instance lock** | ✅ Done — v1.2.10 | Two copies overwrite each other's `eashell_data.json` — last write wins, silently. `FileChannel.tryLock()` on a lock file under the data dir; a second launch shows a warning and exits before touching the data file. |
| **Search / filter** | Not done | `TextField` above the list, filter before `refresh()`. Becomes more valuable now that groups exist. |
| **Persist window geometry** | Not done | Window size, position, `SplitPane` divider, expanded groups (this is what would fix `ARCHITECTURE.md` §9's group-collapse-state gap). Store next to `eashell_data.json` in a separate `settings.json` — never mix UI state into the script file. |
| **Keyboard shortcuts** | Not done | Ctrl+N new, Ctrl+F search, Ctrl+W close tab, Ctrl+R run selected. |
| **Per-script shell** | Not done | A `shell` field (`powershell` / `cmd` / `sh`) on `ScriptEntry`. Would help the stdin trap about PowerShell swallowing input (§2, trap 5) and is cheap to add — one branch in `ProcessRunner.run()`. |
| **Per-script environment variables** | Not done | `Map<String,String>` on `ScriptEntry` applied via `pb.environment()`. Already on the README roadmap. |
| **Exit-code colouring** | Not done | Non-zero exit codes are currently plain text like everything else. A coloured tab title is enough; no output styling needed. |
| **Export / import** | Not done | The file-location blocker ([`IMPROVEMENTS.md`](IMPROVEMENTS.md) item 5) is fixed, so this is now trivial whenever it's wanted. |

---

## 7. Explicitly out of scope

Recorded so the decision does not get re-litigated. Each of these roughly doubles the complexity of
the codebase in exchange for a narrow scenario.

- **Full terminal emulator / ANSI rendering.** See §5.2. The moment this is on the table, the app is
  no longer a launcher.
- **Plugin system / scripting API.** There is no extension audience.
- **Theme editor.** The palette lives in `StyleManager`; a CSS file (item 15) is the right level of
  configurability.
- **Nested folder tree.** §1 is flat for a reason — a tree brings drag-and-drop, move semantics,
  path collisions and a whole management UI.
- **Built-in scheduler / cron.** The OS already has one. A `schedule` field would mean the app must
  run permanently in the tray, which changes what the application *is*.
- **Remote / SSH execution.** Different product.

---

## 8. Combined execution order

Merging defects from [`IMPROVEMENTS.md`](IMPROVEMENTS.md) with the features above. Dependencies are
real — the order matters. **All 10 steps are done**, except the MSI installer half of step 9 (needs
WiX Toolset). Steps 1, 2, 4 and 8 shipped with regressions the v2.0.0 verification pass caught -
[`IMPROVEMENTS.md`](IMPROVEMENTS.md) items 23-32, all fixed in v2.0.1.

| # | Work | Rationale | Status |
| --- | --- | --- | --- |
| 1 | Charset (item 3) + `\r` handling (§5.1) + `NO_COLOR` (§5.2) | One file (`ProcessRunner`), most visible effect on everyday output. | ✅ v1.2.2 |
| 2 | Shadows + scroll speed (§4.1, §4.2) | ~15 lines, instant, zero risk. | ✅ v1.2.2 |
| 3 | Completion callback (item 1) + status refresh (item 4) | Real defects; without them everything built on top looks broken. | ✅ v1.2.3 |
| 4 | Descendant kill (item 2) | STOP must actually stop before group-run multiplies the problem. | ✅ v1.2.4 |
| 5 | stdin (§2) | Isolated; depends on the charset fix from step 1. | ✅ v1.2.5 |
| 6 | Data file location (item 5) + crash-safe save (items 6, 7) | **Blocked packaging.** Add the first real tests here. | ✅ v1.2.6 |
| 7 | UUID identity (item 8) + validation (item 9) | **Blocked groups.** | ✅ v1.2.7 |
| 8 | Groups (§1) + bounded thread pool | Safe now that keys are stable and STOP works. | ✅ v1.2.8 |
| 9 | Packaging (§3 → [`PACKAGING.md`](PACKAGING.md)) + single-instance lock (§6) | Package something already correct. | ✅ v1.2.10 (app-image; MSI not done — needs WiX) |
| 10 | CSS migration (items 15, 16) | Self-contained; can run in parallel with anything above. | ✅ v1.2.9 |

Steps 1 and 2 were a good single first commit: visible improvement, effectively no risk — and that's
exactly how it went.
