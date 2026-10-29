<div align="center">

SlateSpace
Your workspace, arranged around the way you think.
A native Android workspace where tasks, sticky notes, and quick sketches live together — with adaptive views that help you focus, plan, or explore.



</div>

The idea
SlateSpace is a personal workspace designed to feel more like a desk than a rigid to-do list. Keep actionable tasks next to loose thoughts, organise ideas as movable notes, and sketch directly on the workspace.

Instead of forcing one layout for every moment, SlateSpace can shift emphasis based on what your workspace needs.
What it does
- Adaptive workspace — switch between Focus, Explore, and Plan, or let Auto choose an emphasis from your local workspace content.
- Spatial notes desk — create and move notes around a roomy canvas.
- Tasks and subtasks — organise work, track completion, and prioritise unfinished tasks.
- Freeform sketching — jot down ideas with quick strokes alongside your notes.
- Reminders and quiet hours — get task reminders with notification actions; timing may be adjusted by Android's background and battery policies.
- Home-screen widget — see open tasks without opening the app.
- Backup and restore — export your workspace and restore it when needed.
- Personalisation — configure appearance and workspace preferences.
Adaptive modes
Mode	Designed for
Focus	Bring a suggested unfinished task forward and reduce competing content.
Explore	Make idea capture and the notes canvas more prominent.
Plan	Emphasise task lists and overall completion.
Auto	Choose an emphasis using transparent rules based on workspace content.


Auto mode is rule-based, not machine learning. It does not monitor other apps, infer emotions, or autonomously change your deadlines.
Built with
<p>
  <img src="https://skillicons.dev/icons?i=kotlin,androidstudio,gradle&theme=dark" alt="Kotlin, Android Studio, and Gradle" />
</p>

- Language: Kotlin
- UI: Jetpack Compose and Material 3
- Background work: AndroidX WorkManager
- Minimum Android version: Android 8.0 (API 26)
- Compile SDK: 35
- Java: 17
Build the app
1. Install Android Studio with the Android SDK and JDK 17.
2. Open the SlateSpace project folder in Android Studio.
3. Allow Gradle to sync and install any requested SDK components.
4. Run it on an Android device or emulator, or choose Build → Build Bundle(s) / APK(s) → Build APK(s).
Updating an existing installation
SlateSpace stores workspace data locally. Before replacing an existing installation, export a backup from the app. Install an update over the current app only when it is signed with the same signing key; a signature mismatch can prevent an update.
Project status
SlateSpace is an evolving personal workspace. Features and behaviour may change as the app is tested and refined. Reminders are approximate because Android can delay background work under battery-management policies.
<div align="center">

SlateSpace · Make room to think.
</div>
