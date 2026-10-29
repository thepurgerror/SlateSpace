# SlateSpace 3.2 — Widget Fix

## Widget improvements
- Added one-tap completion for the highest-priority unfinished task directly from the home-screen widget.
- The widget refreshes immediately after the completion action and preserves other task fields.
- Fixed the empty-state distinction between a brand-new task list and a list where all tasks are complete.
- Shows the highest-priority open tasks first (Urgent, High, Normal, Low).
- Handles malformed stored task JSON without crashing the launcher widget.
- Updates correctly on widget resize and retains tap-to-open Tasks behaviour.
- Registered the dedicated widget action receiver as non-exported.

## Compatibility
- Package ID remains `com.slatespace.app`.
- SharedPreferences file `slatespace` and `todos` schema remain unchanged.
- Version code 7, version name 3.2.

## Verification status
XML parsing, archive integrity, manifest registration, and source-level invariants were checked. A full Gradle/Android SDK build and launcher/device test were not available in this environment.
