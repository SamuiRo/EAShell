# EAShell

![Loading Demo](src/main/resources/app.ico)

<div align="center">

**A modern JavaFX application for managing and executing CLI commands from a single interface**

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-17.0.2-blue.svg)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-red.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-GPL-green.svg)](LICENSE)

</div>

---

## 📋 Overview

EAShell is a lightweight desktop application designed to simplify working with command-line scripts and programs. Instead of juggling multiple terminal windows, you can organize, manage, and execute all your CLI commands from one elegant interface.

### ✨ Key Features

- **🚀 Quick Script Execution** - Run multiple commands with a single click
- **📁 Working Directory Support** - Execute scripts in specific folders
- **🗂️ Script Groups** - Organize scripts into collapsible groups, with a one-click "run whole group" action
- **⌨️ Interactive Input** - Answer a running process's prompts (e.g. `Read-Host`) from a stdin field under the console
- **📟 Real-time Console Output** - Monitor execution in separate tabs, with progress-bar (`\r`) and ANSI-color noise cleaned up
- **💾 Persistent, Crash-Safe Storage** - Scripts are saved under your user profile (`%USERPROFILE%\.eashell\`), written atomically, and recovered gracefully if the file ever gets corrupted
- **🎨 Modern UI** - Clean, dark-themed interface built with JavaFX
- **⚡ Multi-threaded** - Run multiple scripts simultaneously (up to 8 at once) without blocking
- **📦 Standalone Windows build** - `scripts/package.ps1` produces a self-contained `.exe` that needs no Java installed
- **🖥️ Cross-platform** - Works on Windows, Linux, and macOS

---

## 🖼️ Screenshots

![Demo](src/main/resources/demo.png)

```
┌──────────────────────────────────────────────────────────┐
│ ⚡ Shell  Running: 2      [+ NEW SCRIPT] [⏹ STOP ALL]   │
├────────────────────┬─────────────────────────────────────┤
│ Script List        │ 📟 CONSOLE                          │
│ ┌────────────────┐ │ ┌─────────────────────────────────┐ │
│ │ [ScriptCard 1] │ │ │ [Tab1] [Tab2]                   │ │
│ │ [ScriptCard 2] │ │ │ >>> Executing: npm start        │ │
│ │ [ScriptCard 3] │ │ │ Server running on port 3000...  │ │
│ │ ...            │ │ │                                 │ │
│ └────────────────┘ │ │ [⏹ STOP] [🗑 CLEAR]             │ │
│                    │ └─────────────────────────────────┘ │
└────────────────────┴─────────────────────────────────────┘
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17 or higher** installed on your system
- **Maven 3.8+** (for building from source)

### Installation

#### Option 1: Standalone build (Windows, no Java required)

Double-click **`build-portable.cmd`**, or:

```bash
powershell -ExecutionPolicy Bypass -File scripts/package.ps1
```

This produces **`target/dist/EAShell-<version>-portable.zip`** (~32 MB) - unzip it anywhere (a
USB stick works) and run `EAShell\EAShell.exe`. It carries its own trimmed Java runtime, so the
target machine needs no Java. The build machine needs JDK 17+; Maven is found on `PATH` or, failing
that, in an existing wrapper distribution under `~/.m2/wrapper/dists`.

**Portable mode:** the zipped folder contains a `portable.txt` next to `EAShell.exe`, which makes
the app keep its scripts in `EAShell\data\` instead of `%USERPROFILE%\.eashell` - the folder is
fully self-contained and the window title shows *(portable)*. Delete `portable.txt` to switch to
per-user storage (also what happens automatically if the folder isn't writable, e.g. under
`C:\Program Files`). Build with `-NoPortable` to omit it. See
[`docs/PACKAGING.md`](docs/PACKAGING.md) for details, including the (not yet built) MSI installer path.

#### Option 2: Build from Source

```bash
# Clone the repository
git clone https://github.com/SamuiRo/EAShell.git
cd EAShell

# Build with Maven
mvn clean package

# Run the application
java -jar target/EAShell-2.1.0.jar
```

#### Option 3: Run with Maven

```bash
mvn clean javafx:run
```

---

## 📖 Usage

### Creating a Script

1. Click the **[+ NEW SCRIPT]** button in the top bar
2. Fill in the script details:
    - **Name**: A descriptive name for your script
    - **Working Directory**: The folder where commands should run
    - **Group** *(optional)*: Pick an existing group or type a new one - scripts render under a
      collapsible section per group, with an "Ungrouped" section for scripts without one
    - **Commands**: One or more CLI commands to execute (one per line)
3. Click **OK** (disabled until the name is filled in, the working directory exists, and there's
   at least one command)

### Running a Script

1. Find your script in the left panel
2. Click the **[▶ RUN]** button
3. Monitor output in the console tab that opens on the right

### Managing Scripts

- **Edit**: Click the **[✎ EDIT]** button to modify a script
- **Delete**: Click the **[✖ DELETE]** button to remove a script
- **Stop**: Use **[⏹ STOP]** in the console tab or **[⏹ STOP ALL]** to terminate running scripts (this
  also stops anything the script itself spawned, like a dev server or a `node` child process)
- **Run a whole group**: Click the **[▶ RUN GROUP]** button in a group's header to run every script
  in it
- **Answer a prompt**: If a running command asks for input, type it into the field under the console
  output and press Enter

### Transferring Scripts

Scripts are stored in `eashell_data.json` under `%USERPROFILE%\.eashell\` (not the application
directory - this is independent of where you launch EAShell from). To transfer your scripts:

1. Locate `%USERPROFILE%\.eashell\eashell_data.json`
2. Copy it to your new installation's `%USERPROFILE%\.eashell\` folder
3. Replace the existing file (or merge manually if needed)

---

## 🏗️ Project Structure

```
eashell/
├── src/main/java/com/eashell/
│   ├── App.java                    # Application entry point (checks the single-instance lock)
│   ├── Launcher.java               # Packaging-only entry point (jpackage/java -jar)
│   ├── model/
│   │   ├── ScriptEntry.java        # Script data model (id, name, group, workingDir, commands)
│   │   └── ScriptRepository.java   # JSON persistence: atomic save, corrupt-file recovery, migration
│   ├── service/
│   │   └── ProcessRunner.java      # Script execution engine, incl. stdin
│   ├── ui/
│   │   ├── MainWindow.java         # Main application window
│   │   ├── components/
│   │   │   ├── OutputPanel.java    # Console output panel
│   │   │   ├── ScriptListPanel.java # Script list panel (grouped)
│   │   │   └── TopBar.java         # Top navigation bar
│   │   └── dialogs/
│   │       ├── ScriptDialog.java   # Add/Edit script dialog
│   │       └── DeleteConfirmDialog.java
│   └── util/
│       ├── Constants.java          # Application constants
│       ├── StyleManager.java       # Style-class-name constants + widget factories
│       └── SingleInstanceLock.java # Prevents two copies running at once
├── src/main/resources/styles/app.css  # All colors and CSS rules
├── scripts/package.ps1             # Builds a self-contained app-image (see docs/PACKAGING.md)
├── pom.xml                         # Maven configuration
└── %USERPROFILE%\.eashell\eashell_data.json  # Script storage (auto-generated, not in the repo)
```

---

## 🔧 Configuration

### System-Specific Execution

- **Windows**: Commands are executed via PowerShell with `-NoProfile` and `-ExecutionPolicy Bypass`,
  with output forced to UTF-8 and `NO_COLOR`/`TERM=dumb` set so ANSI escapes aren't emitted
- **Linux/macOS**: Commands are executed via `sh -c`

### Performance Settings

The application includes optimized settings for output handling (see `Constants.java`, the
authoritative source for these numbers):
- UI update interval: 100ms
- Buffer flush threshold: 4KB
- Maximum output buffer: 10,000 characters per script
- Concurrent scripts: up to 8 at once (further RUNs queue rather than piling up threads)

---

## 🛣️ Roadmap

Planned features for future releases:

- [ ] **Enhanced UI/UX** - More polished interface with better visual feedback
- [ ] **Script Timers** - Schedule scripts to run at specific times
- [ ] **Performance Optimizations** - Improved memory usage and responsiveness
- [ ] **Script Templates** - Pre-configured templates for common tasks
- [ ] **Search & Filter** - Quickly find scripts in large collections
- [ ] **Export/Import** - Advanced script sharing capabilities
- [ ] **Environment Variables** - Support for custom environment variables per script
- [ ] **Command History** - Track execution history and outcomes

---

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📝 Technical Details

### Dependencies

- **JavaFX 17.0.2** - UI framework
- **Gson 2.10.1** - JSON serialization
- **JUnit 5** - Testing framework

### Build Tools

- **Maven** - Dependency management and build automation
- **Launch4j** - Windows executable generation (requires a system JRE 17+ to run)
- **jpackage** (JDK 17+, bundled) - self-contained app-image with its own runtime, via `scripts/package.ps1`

---

## 🐛 Troubleshooting

### Application won't start
- If you're running `target/EAShell.exe` (the Launch4j build) or `java -jar ...`: ensure Java 17+ is
  installed (`java -version`) and `JAVA_HOME` is set correctly
- If you're running the standalone app-image build (`target/dist/EAShell/EAShell.exe`), it carries
  its own Java runtime and needs neither of the above
- "EAShell is already running" warning: only one instance can run at a time (by design, so two
  copies can't silently overwrite each other's data) - close the existing window first

### Scripts fail to execute
- Verify the working directory exists and is accessible
- Ensure PowerShell (Windows) or sh (Linux/macOS) is available
- Check command syntax is correct for your operating system

### JSON file corruption
- The app detects invalid JSON automatically: it renames the broken file to
  `eashell_data.json.corrupt-<timestamp>` next to the real one, resets to an empty list, and shows an
  alert explaining what happened - your scripts aren't silently lost, and the broken file is still
  there to inspect or recover data from by hand
- Still worth backing up `%USERPROFILE%\.eashell\eashell_data.json` regularly

---

## 📄 License

This project is licensed under the GNU GENERAL PUBLIC LICENSE - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

Created with ❤️ by the StarLith

---


---

<div align="center">

[Report Bug](https://github.com/SamuiRo/EAShell/issues) · [Request Feature](https://github.com/SamuiRo/EAShell/issues) · [Documentation](https://github.com/SamuiRo/EAShell/wiki)

</div>