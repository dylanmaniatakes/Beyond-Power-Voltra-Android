# Beta 1.3 Release Notes

Beta 1.3 focuses on protocol readiness, safer beta-mode surfacing, and clearer community testing workflows.

## What's New

- Added the Cardio page with active Row/Ski selection.
- Added automatic 250 lb support when the VOLTRA reports the overdrive capability flags.
- Added a Sled Pull placeholder on the Home screen for upcoming beta captures.
- Added preliminary profile/user-slot framework and diagnostics notes for newer firmware that supports multiple users on one VOLTRA.
- Added a GitHub release checker on the More screen for sideload update checks.

## Improved

- Made various protocol parsing, mode-entry, feature-read, max-load, device-name, and diagnostics changes.
- Expanded Isometric results with RFD windows, peak RFD 100ms, time-to-peak, and impulse.
- Recovered the stock Cardio activity selector (`0x54F5`) and now write Row (`0`) or Ski (`1`) before Cardio entry/start.
- Tightened Cardio safety so Ski selection preserves Ski labels and uses the shared Cardio telemetry stream without falling back to Row.
- Updated Direct Load wording and behavior so hold gestures clearly say `Hold to Direct Load`.
- Improved phone layout priority for important controls.

## Safety Notes

- Sled Pull is a placeholder only until selector, load, and telemetry captures are confirmed.
- Profile switching is preliminary and capture-only.
- 250 lb support is automatic only when the VOLTRA reports support; the app does not force it.
