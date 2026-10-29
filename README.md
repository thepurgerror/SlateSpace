 <div align="center">

# 🗂️ SlateSpace

### Your workspace, arranged around the way you think.

*A native Android workspace where tasks, sticky notes, and quick sketches live together — with adaptive views that help you focus, plan, or explore.*

<br>

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](#)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](#)

**A flexible desk for tasks, ideas, and everything in between.**

</div>

---

## Overview

**SlateSpace** is a native Android productivity workspace built around a simple idea: planning should feel less like managing a rigid list and more like working at a desk.

It brings everyday tasks, subtasks, movable notes, freeform sketches, and reminders into one place. Instead of forcing every session into the same layout, SlateSpace can shift its emphasis between getting things done, collecting ideas, and seeing the bigger picture.

SlateSpace is designed for quick capture and low-friction organisation, with local workspace data, personalisation options, and a home-screen widget for checking off priority tasks without opening the app.

## Features

### 🧭 Adaptive Workspace

Choose the kind of workspace that fits what you are doing.

- **Focus** brings a suggested unfinished task forward and reduces competing content.
- **Explore** gives idea capture and the notes canvas more room.
- **Plan** emphasises task lists and overall completion.
- **Auto** selects a workspace emphasis using transparent rules based on local notes and unfinished tasks.
- Switch modes manually whenever you want; Auto is a convenience, not a requirement.

*Auto mode uses simple local rules. It is not machine learning and does not infer emotions or monitor other apps.*

### 🗒️ Spatial Notes Desk

Treat notes like objects on a desk rather than entries in a long list.

- Create and move notes around a spatial canvas.
- Keep loose ideas visible while working on tasks.
- Make quick positional changes with drag interactions.
- Keep the workspace touch-friendly and roomy.

### ✅ Tasks and Subtasks

Keep next actions clear and manageable.

- Create and complete tasks and subtasks.
- Organise work using task priorities.
- Bring the highest-priority unfinished task forward in relevant views.
- Search and group tasks without requiring every item to be displayed at once.
- Use supported task actions from the home-screen widget.

### ✍️ Freeform Sketching

Capture rough thoughts visually, right where you are planning.

- Draw quick sketches and strokes in the workspace.
- Keep saved strokes alongside the rest of the local workspace.
- Use drawing as a lightweight companion to notes and tasks.

### 🔔 Reminders and Quiet Hours

Stay aware of unfinished work without relying on a timer-driven workflow.

- Configure reminders for unfinished tasks.
- Use quiet hours to control when notifications should be muted.
- Access supported notification actions.
- Reminder delivery uses Android WorkManager and may be delayed by system battery management.

### 📱 Home-Screen Widget

See what needs attention without opening SlateSpace.

- View open tasks directly from the Android home screen.
- Prioritise urgent and high-priority unfinished tasks.
- Complete the highest-priority task with a single tap.
- Refresh the widget after completion and open the task screen from the widget.
- Handle empty lists and malformed stored task data without crashing the widget.

### 🔎 Search and Quick Commands

Get to the right item with less navigation.

- Search across supported workspace content.
- Use explicit command prefixes such as `task:`, `note:`, and `search:` where supported.
- Keep command behaviour predictable by limiting unrelated actions when a command prefix is used.

### 💾 Backup and Restore

Keep control of your workspace data.

- Export a workspace backup.
- Restore supported workspace data when needed.
- Keep local notes, tasks, drawings, and strokes together with the app's existing data format.

### 🎨 Personalisation

Shape the workspace around your preferences.

- Choose supported themes and accent settings.
- Adjust haptics and animation preferences.
- Keep the core workspace flexible without adding unnecessary workflow complexity.

---

## Adaptive Modes at a Glance

| Mode | Designed for |
|---|---|
| **Focus** | Bringing a suggested unfinished task forward and reducing competing content. |
| **Explore** | Capturing thoughts and working with the notes canvas. |
| **Plan** | Reviewing task lists and overall completion. |
| **Auto** | Selecting an emphasis from local workspace content using transparent rules. |

Auto mode is intentionally simple and explainable. It does not use a remote AI service, read other apps, or autonomously change your data.

## Technology Stack

| Technology / Component | Purpose |
|---|---|
| Kotlin | Application logic |
| Jetpack Compose | Native Android UI |
| Material 3 | UI components and theming |
| Android WorkManager | Task reminders and background scheduling |
| SharedPreferences | Existing local workspace preferences and stored data |
| Android App Widgets | Home-screen task access and quick completion |
| Gradle | Build system |

<div align="center">

<img src="https://skillicons.dev/icons?i=kotlin,androidstudio&theme=dark" alt="Kotlin and Android Studio" />

</div>

## Getting Started

### Requirements

- Android Studio with a compatible Android SDK installation.
- JDK 17.
- Android Gradle Plugin 8.7.3 and Kotlin 2.0.21, as configured by the project.
- An Android device or emulator running Android 8.0 (API 26) or newer.

### Run Locally

1. Clone or download this repository.
2. Open the project root in Android Studio.
3. Allow Gradle to sync and install any required SDK components.
4. Connect an Android device or start an emulator.
5. Select the app run configuration and press **Run**.

To create an APK, use **Build → Build Bundle(s) / APK(s) → Build APK(s)** in Android Studio.

> **Updating an existing installation:** SlateSpace uses the application ID `com.slatespace.app` and preserves its existing local data keys. To update over an installed version, the signing key must match. Export a backup before installing an update; do not uninstall the existing app just to work around a signature mismatch if its local data matters.

## Compatibility and Data

- **Application ID:** `com.slatespace.app`
- **Current documented version:** `3.2` (`versionCode 7`)
- **Minimum Android version:** Android 8.0 (API 26)
- **Java:** 17
- **Workspace model:** Local notes, tasks, subtasks, drawings, strokes, reminders, and preferences.

SlateSpace is designed around local workspace data. This README does not claim cloud sync or multi-device synchronisation. Keep backups somewhere safe if the workspace data is important to you.

## Design Philosophy

SlateSpace is built around four ideas:

- **Room to think** — notes and sketches should have space alongside tasks.
- **Focus without force** — bring useful work forward without locking users into one mode.
- **Fast interactions** — keep common actions direct and touch-friendly.
- **User control** — make adaptive behaviour understandable and preserve backup and restore options.

## Project Goals

- Make daily planning feel more like a flexible workspace than a rigid checklist.
- Bring tasks, notes, and sketching together without overwhelming the main screen.
- Keep interactions responsive as the workspace grows.
- Make the home-screen widget genuinely useful for quick task completion.
- Preserve existing local data across compatible app updates.

## Build and Verification Notes

The project has undergone source-level checks for Kotlin structure, imports, XML resources, manifest declarations, and archive integrity. A full Android Gradle build and a physical-device test were not performed in the available environment, so build and exercise the included flows in Android Studio before relying on a new build.

## License

No open-source license is specified here. Unless a license is added to the repository, others should not assume they have permission to reuse, modify, or redistribute the source code.

---

<div align="center">

# SlateSpace

**Think freely. Plan clearly. Move forward.**

A flexible Android workspace for the work in front of you and the ideas beside it.

<br>

<sub>Built with Kotlin, Jetpack Compose, and Android WorkManager.</sub>

</div>
