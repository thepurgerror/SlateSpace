# SlateSpace 3.0 — Release Notes

## Product direction
A personal command desk: fast capture first, organize second, and keep the same items accessible from spatial and structured views.

## User-facing changes
- New command desk home screen rather than a metrics-first dashboard.
- Local command bar with note, task, and search prefixes.
- One-tap completion for the currently suggested task on the home screen.
- Task-specific reminder notification with a direct route into the task list.
- Mark the featured task done from its notification.
- No reminder during quiet hours or while the main app is foregrounded.
- Legacy preference keys and package identity retained.

## Limitations in this release
- Command parsing is intentionally deterministic and offline. This is not an AI agent or open-ended natural-language planner.
- Reminders depend on WorkManager and Android scheduling; they are not exact alarms and may be delayed by the system.
- A complete Android build and device test remain required because the build toolchain is not available in the source-generation environment.
