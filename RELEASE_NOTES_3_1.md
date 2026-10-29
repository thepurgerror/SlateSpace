# SlateSpace 3.1 — Release Notes

## Changes
- Fixed the conflicting Compose graphics `Brush` and material icon `Brush` imports.
- Home next-action selection now sorts by ascending priority rank (Urgent → High → Normal → Low), not the reverse.
- WorkManager interval is clamped to 15–20 minutes before building a periodic work request.
- Workspace note drags no longer serialize and write every pointer update; final coordinates are saved on drag end/cancel.
- Workspace note rows now use stable keys and skip per-note entrance coroutine/animation setup; drag scale/rotation/elevation feedback remains.
- Drawing preview uses an incremental mutable `Path` plus a mutable point buffer; saved stroke paths are cached and regenerated only when saved strokes change.
- Task and search-result screens use `LazyColumn` with stable item keys; task grouping is memoized.
- Command prefixes are parsed as explicit actions (`task:`, `note:`, `search:`); prefixes are removed from content and conflicting actions are disabled.
- Imported Urgent priority values survive local parsing and are ranked/displayed as high urgency.

## Data and reminders
- The package ID `com.slatespace.app`, SharedPreferences file `slatespace`, and established storage keys remain unchanged.
- Reminder notification behaviour remains quiet-hours-aware and names actual unfinished tasks; notification action and widget source are included unchanged.
- Version code increased from 5 to 6.

## QA performed
- Source-level parser/structure scan: no Kotlin `expecting` / unexpected-token diagnostics on the final source. (A standalone compiler run cannot type-check Android APIs without the AndroidX and Android SDK classpaths.)
- Duplicate simple import name scan: passed after aliasing the graphics Brush.
- XML well-formedness and manifest resource reference scan: passed.
- Manifest, Gradle identity/version settings, call-site signature scan and ZIP integrity: passed.
- Full Gradle compilation, APK install, notification delivery, runtime performance profiling and physical Moto G35 test: not run; Gradle and Android SDK are not available in this environment.
