# Release Notes

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
