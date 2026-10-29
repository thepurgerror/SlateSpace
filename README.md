# SlateSpace 3.1 — Performance & Reliability Update

SlateSpace is a native Android personal workspace built with Kotlin and Jetpack Compose. This is an update to the existing app identity, not a new package.

## What's improved in 3.1

### Responsiveness
- Task lists and search results use lazy lists so large collections do not compose every row at once.
- Search hits and task grouping are memoized against their data inputs.
- The spatial desk updates note positions in memory during a drag and writes the notes JSON once when the drag ends or is cancelled, rather than serializing and writing on every pointer event.
- Drawing uses an incremental live path during a stroke, appends touch points to a mutable buffer, and caches the geometry of saved strokes until the saved strokes change.
- Removed delayed entrance animations from each workspace note; drag feedback remains.

### Correctness and robustness
- Fixed a conflicting `Brush` import that prevented a clean Kotlin build.
- Fixed the home screen's next-action ordering so the highest-priority unfinished task appears first.
- Preserved imported `Urgent` priorities instead of silently converting them to `Normal`; urgent tasks sort before high priority.
- Fixed command prefix handling so `task:`, `note:`, and `search:` strip the prefix and create/search the intended content. Unrelated actions are disabled for explicit commands.
- Clamped reminder intervals to WorkManager's 15–20 minute range to avoid a crash if stored settings are invalid.
- Added stable keys to workspace notes and lazy list rows to reduce item-state reuse problems.

### Existing features retained
- Local notes, tasks, subtasks, drawings and strokes.
- WorkManager reminders for unfinished tasks, quiet hours, notification actions and home-screen widget.
- Backup and restore.
- Theme, accent, haptic, animation and workspace preferences.

## Compatibility and data safety

- Application ID: `com.slatespace.app`
- SharedPreferences file: `slatespace`
- Existing keys for notes, tasks, drawings and strokes are retained.
- Version: `3.1` / versionCode `6` (higher than the 3.0 project versionCode `5`).
- Use the same signing key to update an installed app. Do not uninstall the old app to get around a signature mismatch if local data matters. Export a backup before installing an update.

## Build and install

1. Export a SlateSpace backup before replacing an installed build.
2. Open this folder in Android Studio.
3. Let Gradle sync and install any requested SDK components. The project targets compileSdk 35, minSdk 26, Java 17, Kotlin 2.0.21 and Android Gradle Plugin 8.7.3.
4. Run the app on a connected device or emulator, or use **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. Install over the existing package only if the signing key matches.
6. On Android 13+, allow notifications for task reminders. WorkManager intervals are approximate and may be delayed by Android battery management.

## Verification limits

The source was rechecked for Kotlin parser/structure diagnostics, duplicate import names, XML well-formedness, resource references, manifest declarations, source call-site consistency, and ZIP integrity. The standalone Kotlin compiler in this environment does not have the Android/Compose dependency classpath, and this environment has no Android SDK or Gradle executable; therefore a full Android Gradle compilation, APK install, performance benchmark, and Moto G35 device test were **not** possible here. Treat the package as audited source, not a device-verified APK. Build and exercise the included flows in Android Studio before relying on it.


## SlateSpace 3.2 widget fix
The home-screen widget now prioritizes open tasks and provides a one-tap **Done ✓** action for the highlighted task. It distinguishes an empty task list from an all-complete list, updates after widget actions, and safely handles malformed local JSON. See `RELEASE_NOTES_3_2.md`.

Build verification is limited to source/XML/archive checks in this environment; build and test in Android Studio before installing. Back up local app data first and do not uninstall the current app to work around a signing-key mismatch.
