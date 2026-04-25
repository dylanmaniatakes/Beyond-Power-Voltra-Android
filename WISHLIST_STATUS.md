# Voltra Controller Wishlist Status

This is the active post-Beta backlog. Completed alpha-era items have been removed so this file only tracks meaningful remaining work.

Status key:

- Polish: implemented, but still worth testing across more real workouts.
- Planned: practical next work for the app.
- Future: larger follow-on work after the local controller is stable.
- Needs capture: blocked on cleaner official-app or device evidence.

## Shipped Through Beta 1.0

- Core Bluetooth scan, connect, reconnect, and command flow
- Weight Training, Resistance Band, Damper, Isokinetic, Isometric Test, Custom Curve, and Row Mode
- Row Mode with Just Row, preset distances, resistance level, simulated wear, and live metrics
- Startup image upload with photo selection, square crop, and VOLTRA transfer
- Device rename from the app
- Custom Curve builder with editable points, resistance range, range of motion, and local presets
- Isometric Test metrics that closely match the VOLTRA result screen
- Chains, inverse chains, assist, eccentric, and the other core workout controls
- Current-session power/rep history for supported modes
- Rep, set, and phase counters across the main workout modes
- Tablet-aware layouts and mode-specific control styling
- MQTT Sensor and local HTTP Gateway
- Home Assistant addon path built from the Android app's local integration work
- Diagnostics, raw frame export, and protocol notes for ongoing reverse engineering

## Still Left

| Feature | State | What is left |
| --- | --- | --- |
| kg parity across every editor | Polish | Weight Training is strongest. Keep checking Resistance Band, Custom Curve limits, and advanced load controls for places that still think in lb. |
| Custom Curve refinement | Polish | The current builder works. Remaining work is mostly fit-and-finish: safer curve shaping, clearer preset naming, and more confidence across unusual range-of-motion settings. |
| Local workout history | Planned | Current workout-session history exists, but persistent history across app launches still needs a storage model and UI. |
| Workout export as CSV | Planned | Depends on persistent workout history so exported columns match the saved session shape. |
| Weight and workout presets | Planned | Custom Curve presets exist. Broader saved presets for common weights/settings are still worth adding. |
| Config and data backup | Planned | Decide whether this should be local-file-only, Android document picker based, or tied into a cloud provider the user chooses. |
| Local automation workflows | Planned | HTTP control is available; the next step is safer examples and clearer state/action docs. |
| Remaining unknown protocol mapping | Needs capture | Continue comparing iPad captures against Android logs when a feature behaves differently or exposes a metric Android does not decode yet. |
| Android watch companion | Future | Useful once phone control feels boringly reliable. A crown-driven selector still makes sense. |
| Paired companion support for two VOLTRAs | Future | Save this until single-device control has had more mileage. |
| Android widget | Future | Worth considering after connection state and command safety are extremely stable. |
| Health and workout app exports | Future | Google Health Connect, Hevy, Fitbit, and similar integrations make sense after persistent local history lands. |
| In-app voice load/unload | Future | Needs careful safety design before it should move closer. |

## Best Next Captures

1. Any kg mismatch outside Weight Training, especially Resistance Band or Custom Curve.
2. Longer Custom Curve workouts using unusual resistance ranges or high range-of-motion values.
3. Any VOLTRA screen metric that looks correct on the unit but missing or different in Android diagnostics.
