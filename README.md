# CodeSprint for JetBrains IDEs

**Focus. Code. Improve.**

CodeSprint is a lightweight productivity companion for programming students and professional developers. It lives in a tool window and helps you build a consistent coding habit with a Pomodoro focus timer, daily goals, streak tracking and simple statistics.

It runs in **Android Studio** and every IntelliJ-based IDE (IntelliJ IDEA, PyCharm, WebStorm, GoLand, PhpStorm, CLion, Rider and others), version 2023.3 and newer.

Everything is stored locally. There is no account, no backend and no telemetry, and your source code is never read or transmitted.

## Features

- **Focus timer** - 25 minute focus sessions, 5 minute short breaks and 15 minute long breaks, all configurable. Start, pause, resume, reset and switch between session types.
- **Accurate and reliable** - the timer is calculated from timestamps, so it keeps counting while you switch files, close the tool window or restart the IDE. A session that finishes while the IDE is closed is credited correctly the next time it opens.
- **Daily goal** - choose a target with presets (30m, 1h, 2h, 3h) or a custom value and track completed focus time.
- **Statistics** - today's focus time, completed sessions, weekly focus time, goal completion, current streak and longest streak. Only completed focus sessions count; abandoned or reset sessions never do.
- **Weekly overview** - a seven day chart; hover a bar for the exact time.
- **Motivational reminders** - over 30 professional developer quotes shown at a configurable interval in the status bar, as a notification, or on demand.
- **Status bar timer** - always-visible countdown; click to start, pause or resume.
- **Notifications** - a balloon when a session ends, with one-click start for the suggested break or the next focus session.
- **Native look and feel** - follows the IDE theme (light, dark, high contrast) and UI scaling, with keyboard navigation and accessibility labels.

## Installation

### From JetBrains Marketplace

1. Open **Settings | Plugins | Marketplace** (in Android Studio: **File | Settings | Plugins**).
2. Search for **CodeSprint** and click **Install**.
3. Restart the IDE if prompted.

### From a ZIP file

1. Open **Settings | Plugins**.
2. Click the gear icon and choose **Install Plugin from Disk...**
3. Select `codesprint-jetbrains-1.0.0.zip`.

## Usage

1. Open the **CodeSprint** tool window from the right sidebar, or choose **Tools | CodeSprint | Open Dashboard**.
2. Press **Start** to begin a focus session. The ring and the status bar show your progress.
3. When the session ends you get a notification and the timer switches to the suggested break.
4. Set your daily target in the **Daily goal** section and watch the progress bar fill.
5. Open **Settings | Tools | CodeSprint** (or the gear icon in the dashboard header) to change durations, notifications and reminders.

Switching session type while a session is in progress asks for confirmation, because the unfinished session is discarded.

## Actions

All actions are in **Tools | CodeSprint** and can be found with **Find Action** (`Ctrl+Shift+A` / `Cmd+Shift+A`). You can assign keyboard shortcuts in **Settings | Keymap**.

| Action | Description |
| --- | --- |
| Start, Pause or Resume Timer | One action that does the right thing for the current state |
| Start Timer / Pause Timer / Resume Timer | Individual controls, shown only when applicable |
| Reset Timer | Discard the current session |
| Switch Session Type... | Choose focus, short break or long break |
| Show Motivational Quote | Show a quote immediately |
| Open Dashboard | Open the CodeSprint tool window |
| Open Settings | Open the CodeSprint settings page |
| Clear Stored Data... | Delete all sessions, statistics and streaks |

## Settings

Found under **Settings | Tools | CodeSprint**.

| Setting | Default | Range |
| --- | --- | --- |
| Focus (minutes) | 25 | 1-180 |
| Short break (minutes) | 5 | 1-60 |
| Long break (minutes) | 15 | 1-120 |
| Focus sessions before a long break | 4 | 2-10 |
| Daily focus goal (minutes) | 120 | 5-1440 |
| Notify when a session finishes | on | |
| Switch to a break after a focus session | on | |
| Motivational reminder interval (minutes) | 30 | 0-180 (0 turns it off) |
| Display reminders in | Status bar | Status bar, notification, both |

Changed durations apply to the next session; a session already in progress keeps its length. The dashboard follows the IDE theme, so there is no separate theme setting.

## Data and privacy

- Preferences are stored in `codesprint-settings.xml` and sync through the IDE's Settings Sync like any other setting.
- Timer state, history and statistics are stored in `codesprint-data.xml` in the IDE configuration folder. Roaming is disabled so progress on one machine is never merged with another.
- The plugin makes no network requests and collects no telemetry.

## Building from source

Requirements: JDK 17.

```
./gradlew test           # unit tests for the timer, statistics and storage logic
./gradlew runIde         # start a sandbox IDE with the plugin installed
./gradlew verifyPlugin   # run the JetBrains Plugin Verifier (IntelliJ IDEA and Android Studio)
./gradlew buildPlugin    # build build/distributions/codesprint-jetbrains-<version>.zip
```

### Project layout

```
src/main/kotlin/com/codesprint/ide/
  core/       Pure Kotlin: timer engine, history, statistics, messages, settings model, storage validation
  state/      IDE persistence (PersistentStateComponent) for settings and data
  service/    Application services: timer, reminders, notifications
  ui/         Tool window dashboard, custom components, status bar widget
  actions/    Tools menu actions and the session-type switcher
  settings/   Settings page (Kotlin UI DSL)
  startup/    Startup activity that restores the timer
src/main/resources/META-INF/plugin.xml
src/test/kotlin/   Unit tests for the core package
```

The `core` package has no dependency on the IntelliJ Platform, so it is covered by ordinary unit tests and can be reasoned about independently of the UI.

## Publishing

See [PUBLISHING.md](PUBLISHING.md) for the JetBrains Marketplace checklist.

## License

MIT. See [LICENSE](LICENSE).
