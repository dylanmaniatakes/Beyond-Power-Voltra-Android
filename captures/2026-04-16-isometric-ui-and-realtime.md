# 2026-04-16 Isometric UI and Realtime Behavior

## Inputs

- `sysdiagnose_2026.04.16_20-09-27-0400_iPhone-OS_iPad_22F76`
- matching iPad screen recording of entering Isometric Test, loading weight, completing one pull, and unloading

## Confirmed behavior

- The official app opens a dedicated `Isometric` screen rather than reusing the standard workout card layout.
- The visible flow is:
  1. Enter Isometric Test
  2. Tap `Load Weight`
  3. Pull against the VOLTRA
  4. Tap `Finish` to end the attempt / unload
- The screen shows a realtime force graph plus:
  - Current Force
  - Time
  - Peak Force
  - RFD 0-100ms
  - Time To Peak
  - Impulse 0-100ms

## Android interpretation

- The live force samples already recovered from the Isometric stream are sufficient to compute the visible derived metrics locally.
- Android Alpha 1.3.2+ should therefore treat Isometric as a dedicated test screen:
  - live graph from streamed force samples
  - derived metrics computed locally from the same sample window
  - clear `Load Weight` / `Finish` actions instead of a generic weight-control layout

## Remaining unknowns

- No separate BLE packet has been confirmed yet for a richer post-test summary object.
- No editable Isometric settings have been confirmed beyond the observed force limit and duration reads.
