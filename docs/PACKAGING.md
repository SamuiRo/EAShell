# EAShell — Packaging without a Java requirement

**Goal:** an EAShell that runs on a machine with **no Java installed** — double-click and it works.

Current state (1.2.1): `target/EAShell.exe` is a Launch4j wrapper around the JAR. `pom.xml:194`
points its JRE search at `%JAVA_HOME%;%PATH%`, so it **requires a system JRE 17+** and fails with an
error dialog on a clean machine. It is also not a fat JAR — it is a JAR plus a sibling `libs/`
directory that must travel with it.

The tool that solves this is **`jpackage`**, shipped inside JDK 14+. Your JDK 17 already has it.
Verify:

```bash
jpackage --version
```

---

## 0. Prerequisite that is not optional

`Constants.DATA_FILE = "eashell_data.json"` is resolved against the **current working directory**.
An application installed under `C:\Program Files\EAShell\` cannot write to its own directory —
every save fails silently and the user loses their scripts on first use.

**Fix [`IMPROVEMENTS.md`](IMPROVEMENTS.md) item 5 before packaging anything.** Target:

```java
public static final Path DATA_FILE =
        Path.of(System.getProperty("user.home"), ".eashell", "eashell_data.json");
```

…with a one-time migration that moves an existing file from the CWD on first launch, so current
users keep their scripts.

---

## 1. The `Launcher` class — do this first, it is not optional either

JavaFX refuses to start when the **main class extends `Application`** and `javafx.graphics` is on the
classpath rather than the module path. The failure is:

```
Error: JavaFX runtime components are missing, and are required to run this application
```

The current `EAShell.exe` avoids it because Launch4j passes `--module-path libs --add-modules ...`
(`pom.xml:196–197`). A jpackage app-image does not do that by default.

The standard workaround is a launcher that does **not** extend `Application`:

```java
package com.eashell;

/** Entry point for packaged builds. Must NOT extend Application — see docs/PACKAGING.md. */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
```

Then point the JAR manifest, the Launch4j config and jpackage at `com.eashell.Launcher` instead of
`com.eashell.App`. `App` itself is unchanged.

This costs 5 lines and removes an entire class of packaging failure. Do it before anything else.

---

## 2. Stage the jars

`--main-jar` is resolved **relative to `--input`**, so the application JAR has to sit in the same
directory as its dependencies. Today `maven-dependency-plugin` puts dependencies in `target/libs`
while the app JAR stays in `target/`.

Easiest fix — one extra execution in `pom.xml` that copies the built artifact next to its deps:

```xml
<plugin>
  <artifactId>maven-antrun-plugin</artifactId>
  <version>3.1.0</version>
  <executions>
    <execution>
      <id>stage-app-jar</id>
      <phase>package</phase>
      <goals><goal>run</goal></goals>
      <configuration>
        <target>
          <copy file="${project.build.directory}/${project.artifactId}-${project.version}.jar"
                todir="${project.build.directory}/libs"/>
        </target>
      </configuration>
    </execution>
  </executions>
</plugin>
```

`target/libs/` now contains everything the app needs and nothing it doesn't.

> ✅ **Fixed in v2.0.1.** `copy-dependencies` does not honour scope by default: despite being
> declared `<scope>test</scope>`, the JUnit stack (`junit-jupiter-api`, `junit-jupiter-params`,
> `junit-platform-commons`, `opentest4j`, `apiguardian-api` — 947 KB) was copied into `target/libs`,
> shipped inside the app-image, and listed on the runtime classpath in `app/EAShell.cfg` (found and
> verified against the v2.0.0 build).
>
> The fix - one line added to the `copy-dependencies` configuration in `pom.xml`:
>
> ```xml
> <includeScope>runtime</includeScope>
> ```
>
> That keeps compile+runtime dependencies and drops test and provided ones. Re-verified: a clean
> `mvn package` no longer copies any JUnit jar into `target/libs`.

---

## 3. Level 1 — self-contained app image (no extra tools)

```bash
jpackage --type app-image \
         --name EAShell \
         --app-version 1.2.1 \
         --input target/libs \
         --main-jar EAShell-1.2.1.jar \
         --main-class com.eashell.Launcher \
         --icon src/main/resources/app.ico \
         --dest target/dist \
         --vendor "StarLith" \
         --copyright "Copyright © 2025"
```

Output: `target/dist/EAShell/` containing `EAShell.exe`, an `app/` folder and a **bundled `runtime/`**.
Copy that folder to any Windows machine — no Java required. Zip it and you have a portable release.

Notes:

- Do **not** pass `--win-console`; this is a GUI application.
- `--app-version` must be numeric (`1.2.1` is fine). MSI later rejects anything else.
- The icon must be `.ico` on Windows — `app.ico` already exists.
- **You can only build a Windows bundle on Windows.** jpackage does not cross-compile, and the
  JavaFX jars in `target/libs` are platform-specific (Maven resolves the classifier for the build
  host). A Linux/macOS build needs to run on that OS.

### Verify honestly

Test on a machine or VM with **no JDK and no `JAVA_HOME`**. Testing on your dev box proves nothing —
it will silently pick up the system Java through the environment. Short of a VM, scrubbing the child
environment gets most of the way there:

```powershell
$env:JAVA_HOME = ""; $env:PATH = "C:\Windows\system32;C:\Windows"
Get-Command java -ErrorAction SilentlyContinue    # must return nothing
Start-Process "target\dist\EAShell\EAShell.exe"
```

**Don't be fooled by the process list.** The jpackage Windows launcher starts the JVM as a *child*
process, so `Start-Process -PassThru` hands you the parent stub — ~5 threads, ~8 MB,
`MainWindowHandle = 0` — which looks exactly like a crashed launch. Enumerate every process with the
app's name and look at the one with a non-zero window handle:

```powershell
Get-Process EAShell | ForEach-Object { $_.Refresh(); $_ } |
    Select-Object Id, MainWindowTitle, @{n='Threads';e={$_.Threads.Count}}
```

A healthy run shows one process with the window title and ~100 threads, alongside the stub.

The runtime image has no `runtime\bin\java.exe` — jpackage strips the launcher binaries. That is
normal and not a sign of a broken build.

---

## 4. Level 2 — trim the runtime

A non-modular app makes jpackage bundle a broad default module set. Expect roughly 120–180 MB
initially; measure yours rather than trusting the figure.

Trim by naming only what is used:

```bash
--add-modules java.base,java.desktop,java.logging,java.scripting,jdk.unsupported
```

- `java.desktop` — required by JavaFX.
- `jdk.unsupported` — JavaFX uses `sun.misc.Unsafe`; omitting it produces runtime failures that are
  hard to trace.
- Add modules back one at a time if something breaks. **Trim iteratively and re-test after each
  step** — a missing module usually shows up as a `NoClassDefFoundError` at some later, unrelated
  moment, not at startup.

Expect ~60–80 MB after trimming.

---

## 5. Level 3 — modular build with `jlink` (smallest output)

Optional. Gets you to roughly 45–60 MB, at the cost of introducing JPMS to the project.

`src/main/java/module-info.java`:

```java
module com.eashell {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;
    requires com.google.gson;

    opens com.eashell.model to com.google.gson;   // Gson uses reflection on ScriptEntry
    exports com.eashell;
}
```

Two things to know:

- **`opens com.eashell.model to com.google.gson;` is mandatory.** Without it Gson cannot read or write
  `ScriptEntry` and persistence breaks at runtime, not at compile time.
- Gson 2.10.1 ships a real `module-info` (module name `com.google.gson`), so `requires` works
  directly — no automatic-module warnings.
- `javafx.fxml` is declared in `pom.xml` but the project contains no FXML files. Drop the dependency
  rather than adding a `requires` for it.

Then use the `jlink` goal of `javafx-maven-plugin` (0.0.8 supports it) and hand the resulting image
to `jpackage --runtime-image`.

**Recommendation:** only go here if the ~20 MB saved over Level 2 actually matters. It adds a
permanent constraint to every future dependency you add.

---

## 6. Level 4 — a real installer (MSI)

Requires **WiX Toolset 3.x** on the build machine (JDK 17's jpackage does not support WiX 4).
Install it and make sure `candle.exe`/`light.exe` are on `PATH`.

```bash
jpackage --type msi \
         --name EAShell \
         --app-version 1.2.1 \
         --input target/libs \
         --main-jar EAShell-1.2.1.jar \
         --main-class com.eashell.Launcher \
         --icon src/main/resources/app.ico \
         --dest target/dist \
         --vendor "StarLith" \
         --win-menu --win-menu-group "EAShell" \
         --win-shortcut \
         --win-dir-chooser \
         --win-upgrade-uuid <a fixed GUID, generated once and never changed>
```

`--win-upgrade-uuid` must stay **identical across every release** — it is what lets 1.2.2 upgrade
1.2.1 in place instead of installing a second copy. Generate it once and record it in the build
script.

Once an installer exists, the **single-instance lock**
([`ROADMAP.md`](ROADMAP.md) §6) stops being optional: a Start Menu shortcut makes launching two
copies trivial, and two copies overwrite each other's `eashell_data.json`.

---

## 7. Wiring it into the build

Keep it out of `pom.xml`. jpackage failures are noisy and much easier to debug from a script than
from inside a Maven plugin execution, and this runs once per release, not once per build.

`scripts/package.ps1`:

```powershell
mvn clean package
if ($LASTEXITCODE -ne 0) { exit 1 }
jpackage --type app-image --name EAShell --app-version 1.2.1 `
         --input target/libs --main-jar EAShell-1.2.1.jar `
         --main-class com.eashell.Launcher `
         --icon src/main/resources/app.ico --dest target/dist
```

Read the version from `pom.xml` rather than hardcoding it — otherwise this becomes a **fourth** place
the version has to be updated by hand (see [`IMPROVEMENTS.md`](IMPROVEMENTS.md) item 17).

---

## 8. What happens to Launch4j

Once jpackage produces a self-contained image, the launch4j-maven-plugin block in `pom.xml`
(`:169–214`) is redundant — it exists solely to make a `.exe` that finds a system JRE, which is the
problem being eliminated.

Keep both during the transition, then delete the Launch4j block. That also removes one of the three
places the version number is duplicated.

---

## 9. Checklist

- [x] Item 5 done — data file lives under `%USERPROFILE%\.eashell\`, with migration
- [x] `Launcher` class added; manifest and jpackage point at it
- [x] App JAR staged into `target/libs`
- [x] `--type app-image` builds and launches locally
- [x] **Verified without a system Java** — the app-image was launched with an empty `JAVA_HOME`
      and `PATH=C:\Windows\system32;C:\Windows`, confirmed via `Get-Command java` returning
      nothing to the child process. The window opened and the app ran normally (99 threads,
      153 MB RSS); image size 74 MB. A clean VM would additionally rule out unrelated system
      dependencies, but the bundled runtime is demonstrably being used.
- [x] Test-scope jars excluded from `target/libs` — fixed in v2.0.1, `<includeScope>runtime</includeScope>`.
      947 KB of JUnit no longer ships inside the app-image. See §2 and
      [`IMPROVEMENTS.md`](IMPROVEMENTS.md) item 29.
- [x] `scripts/package.ps1` fails clearly when `mvn` is missing — fixed in v2.0.1 with a
      `Get-Command mvn` check up front (item 30). Still no `mvnw` wrapper.
- [x] Scripts save and reload correctly from the installed location
- [x] STOP terminates child processes ([`IMPROVEMENTS.md`](IMPROVEMENTS.md) item 2) — orphaned
      processes are far worse in an installed app than in a dev run
- [x] Runtime trimmed with `--add-modules` (§4) — `scripts/package.ps1` builds this directly,
      manually verified: run/output/STOP all work against the trimmed build
- [x] Single-instance lock in place ([`ROADMAP.md`](ROADMAP.md) §6) — manually verified: a
      second launch is blocked with a warning and exits; the lock releases on close
- [ ] `--win-upgrade-uuid` generated and recorded — not applicable yet, no MSI (see below)
- [ ] Launch4j block removed from `pom.xml` — kept intentionally; see note below

**Not done in this pass:** the MSI installer (§6) needs WiX Toolset, which isn't installed on
this machine. What exists today is the Level 1+2 app-image only (`scripts/package.ps1`) — a
portable folder (`target/dist/EAShell/`) you copy wherever you want to run it, not a Start
Menu-integrated install. Launch4j is kept for now rather than removed, since it's still the
only way to produce a `.exe` that relies on a system JRE for anyone who prefers that over the
bundled-runtime app-image; revisit removing it once the MSI path is actually built.
