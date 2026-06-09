# Release Notes

## Beta 1.3

Beta 1.3 is a protocol and beta-feature readiness release. It adds visible support for newer VOLTRA firmware behavior while keeping unconfirmed write paths capture-only.

### What's New

- Added the Cardio page with Row/Ski selection. Row remains active; Ski is visible but start is blocked until the exact stock-app selector command is captured.
- Added automatic 250 lb support when the VOLTRA reports the required overdrive capability flags.
- Added a Sled Pull placeholder on the Home screen so beta testers can see where the mode will land after selector, load, and telemetry captures are confirmed.
- Added preliminary profile/user-slot framework and diagnostics notes for newer firmware with multiple users on one VOLTRA.
- Added a GitHub release checker on the More screen for sideload users.

### Improved

- Made several protocol parsing and command-safety updates around mode entry, feature reads, max target load, device naming, and diagnostics export.
- Expanded Isometric results with additional RFD windows, peak RFD 100ms, time-to-peak, and impulse values.
- Tightened Cardio behavior so Ski selection no longer sends Row commands or relabels Row telemetry as Ski.
- Updated Direct Load wording across the hold gesture and kept normal Load buttons separate.
- Improved small-screen layout priority so primary controls stay easier to reach.

### Notes

- Sled Pull, active Ski start, and profile switching remain capture-only until stock app traffic confirms the exact safe command paths.
- 250 lb support is not forced. The app only unlocks the higher limit when the VOLTRA reports support.

## Beta 1.2

Beta 1.2 is a polish release. The app now leans harder on the VOLTRA's own live state, so the controls feel less like a separate remote and more like a companion screen that catches up cleanly when you open it mid-workout.

### What's New

- The weight dial now syncs from the VOLTRA's reported target weight when the app connects or receives a mode refresh.
- Added a clearer control-link state so the app distinguishes between connected, validating, switching modes, and ready.
- Updated the app label and build metadata to Beta 1.2.

### Improved

- Load controls now use clearer labels when a mode is ready but not loaded.
- The interface is less likely to show stale local weight values after reconnecting or switching back from another mode.
- Compared the official app's Isokinetic auto-unload behavior; it appears to be driven by configurable holding-time and target-rep settings, so it stays research-backed for now rather than being blindly enabled.

## Beta 1.0

Beta 1.0 marks the point where Voltra Controller moves from alpha experimenting into a much more complete daily-use build. The big theme is simple: more modes now behave like real first-class controls instead of protocol experiments.

### What's New

- Added Row Mode with Just Row, preset distances, live rowing stats, resistance level, simulated wear, and drive-force graphing.
- Added working startup image upload with photo selection, square crop, and transfer to the VOLTRA.
- Added Custom Curve controls with editable curve points, range of motion, resistance range, and saved presets.
- Added current-workout history for supported power/rep modes so recent efforts stay visible during the session.

### Improved

- Isometric Test now mirrors the VOLTRA's own results closely enough for real strength-test use.
- Row Mode now starts, finishes, resets, and reports metrics cleanly without breaking the older workout counters.
- Weight Training, Resistance Band, Damper, and Isokinetic counters were cleaned up after the Row Mode work.
- Device name changes remain implemented and working from the app.
- The newer mode screens have been tightened up to better match the app's established control style.

### Fixed

- Fixed startup image crashes and the missing VOLTRA-side startup image apply path.
- Fixed stale row metrics carrying across starts, finishes, and preset changes.
- Fixed rep/set/phase counter regressions after entering Row Mode.
- Fixed several BLE parsing collisions where one workout mode could accidentally consume another mode's live data.
- Reduced jitter and state churn in newer controls like rowing resistance, simulated wear, and Custom Curve settings.

### Notes

This build is still local-first and capture-driven, but it now covers the major VOLTRA features we have working captures for: core training modes, Isometric Test, Custom Curve, Row Mode, device rename, and startup images.
