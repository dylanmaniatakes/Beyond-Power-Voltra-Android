# Voltra Controller Wishlist Status

This file is now the active backlog, not the full historical brainstorm.
Finished alpha features are summarized once and then removed from the day-to-day list so the remaining work is easier to steer.

Status key:

- Shipped: live in the current alpha and usable.
- Polish: implemented, but still needs UX cleanup or more hardware confidence.
- Needs capture: blocked on cleaner official-app or device evidence.
- Planned: practical next work after current polish items settle.
- Future: larger follow-on project after the local controller is stable.

## Shipped In Alpha 1.3

- Voltra Controller branding and cleaned deployment docs
- Theme picker with multiple color options
- Saved-device reconnect flow
- Top-right load / weight off chip on the control screens
- Tap-to-enter weight plus +/- controls with hold-to-cycle increments
- Instant-apply default preference
- Tablet-aware layout
- Battery display on Home
- Rep counting and phase display
- MQTT Sensor in More
- HTTP Gateway in More
- Home launcher tiles for Weight Training, Resistance Band, Damper, Isokinetic, Isometric Test, and Custom Curve

## Active Backlog

| Feature | State | What is needed from you |
| --- | --- | --- |
| Exit workout behavior across all modes | Polish | Keep testing the left-side exit flow on Weight Training, Resistance Band, Damper, Isokinetic, and Isometric. Export diagnostics if the device stays in the workout screen or lands in the wrong mode. |
| Weight control feel | Polish | The current slider + tap entry flow works, but we should keep sanding down any jitter or awkward taps. Note the exact screen and control when something feels sticky. |
| kg parity across all workout profiles | Polish | Strength mode is farther along than Resistance Band and the advanced load-profile editors. Keep noting where kg still falls back to lb behavior. |
| Set tracking hardening | Polish | Rep tracking is good. Keep watching whether sets stay aligned with the unit during longer workouts or mode switches. |
| Isometric live telemetry | Polish | Load/unload is working, but the live metrics still need better decoding. The highest-value captures are short, isolated Isometric pulls with clear notes. |
| Assist mode validation | Polish | The toggle is wired in; keep checking whether the app state, device UI, and real feel all agree. |
| Chains live state sync | Polish | The regular Chains path is close, but keep noting any cases where the toggle state or amount lags the device. |
| Inverse Chains independence | Needs capture | This still needs stronger proof that the on/off state and amount are fully independent from regular Chains. Clean official-app captures are still the key blocker. |
| Eccentric overload edge cases | Polish | Positive and negative ranges are exposed, but we still want more confidence near the device clamps and minimum return load behavior. |
| Resistance Band advanced settings parity | Polish | Experience, Standard/Inverse, Progressive Length, Curve, and Band Length are all in play now; keep reporting anything that reads correctly but does not actually set on the unit. |
| Custom Curve apply/edit flow | Alpha | Alpha 1.4.5 adds a four-point graph/sliders builder with local save/load presets. Keep testing how edited curves feel against the VOLTRA. |
| Local workout history | Planned | The app now has enough live state to start recording local sessions once the telemetry edges are a little calmer. |
| Workout export as CSV | Planned | This should land right after local history so the export columns can match the stored session shape. |
| Weight presets | Planned | No capture blocker. This is mostly a product-shape decision once the weight/settings editors feel final. |

## Device, User, And Companion Features

| Feature | State | What is needed from you |
| --- | --- | --- |
| Fully local user profiles | Planned | Need to settle what belongs in a profile: name, units, default presets, MQTT identity, and maybe notes. |
| Config and data backup | Planned | Need a decision on local-file-only backup vs cloud-provider options. |
| Android watch companion | Future | Good fit once phone control is calmer. The crown-driven weight selector still makes sense. |
| Paired companion support for two VOLTRAs | Future | Save this until single-device control feels boringly reliable. |
| Android widget | Future | Probably only worth doing after connection state and command safety are extremely stable. |
| Boot intro / custom logo selection | Future | Needs cleaner official-app evidence and likely a file-transfer path. |

## Integrations

| Feature | State | What is needed from you |
| --- | --- | --- |
| Home Assistant path | Planned | MQTT Sensor and HTTP Gateway are the first pieces. Next step is deciding whether we want a local integration, a lightweight bridge, or both. |
| Local automation workflows | Planned | The new HTTP gateway makes this more practical. Next step is deciding which control/state flows should be easiest to script first. |
| In-app voice load/unload | Future | Needs careful safety design before it should move closer. |
| Google Health Connect | Future | Only makes sense once local workout history is stable and honest enough to export. |
| Fitbit integration | Future | Needs a real use case before it should compete with core control work. |
| Hevy integration | Future | Interesting once presets and workout history exist, but not a near-term blocker. |

## Developer / Under Development

- Custom Curve is active in alpha form; treat the four-point graph builder as experimental until more official-app editor captures are mapped.
- Rowing should stay hidden behind developer-only paths until we have clean captures and a safe control model.
- Protocol expansion should continue to follow captured evidence, not guessed commands.

## Highest-Value Next Captures

1. Isometric: arm/load, one short pull, unload, with a note about what the iPad shows for current force, peak force, and timer.
2. Inverse Chains: clean off/on and amount changes with regular Chains left at zero, then one repeat with regular Chains nonzero.
3. Custom Curve: compare an edited Android curve against the iPad curve editor, then export immediately after applying/loading.
4. kg mode outside Weight Training: a clean official-app pass in Resistance Band and one advanced editor while the app is set to kg.
5. Any mismatch between app state and device state: export right after the mismatch while the screen is still visible.
