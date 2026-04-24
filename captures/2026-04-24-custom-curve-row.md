# 2026-04-24 Custom Curve and Row Mode

## Source Artifacts

- Android diagnostic: `/Users/ticnitsi/Downloads/text-0 2.txt`
- iPad sysdiagnose: `/Users/ticnitsi/Documents/BeyondPower-Port/Ipad Captures/sysdiagnose_2026.04.24_13-21-43-0400_iPhone-OS_iPad_22F76.tar.gz`
- iPad screen recording: `/Users/ticnitsi/Documents/BeyondPower-Port/ipad videos/ScreenRecording_04-24-2026 13-20-57_1.MP4`

## Custom Curve

- Android diagnostics confirmed the captured Custom Curve enter sequence reaches `Workout mode: Custom Curve, Ready`.
- The working flow is:
  1. `0x11 0x5183 EP_FITNESS_DATA_NOTIFY_SUBSCRIBE = F5 7B 65 00`
  2. `0xAF` bulk subscribe list
  3. `0x11 0x5182 EP_FITNESS_DATA_NOTIFY_HZ = 0x28`
  4. `0xAA 0x06` curve payload
  5. `0x11 0x4FB0 FITNESS_WORKOUT_STATE = 6`
  6. refresh mode feature state
- The `0xAA 0x06` curve payload has a 23-byte header followed by six little-endian `(x,y)` float32 graph samples split around a `0x0D` separator byte.
- Header bytes `4..5` appear to be range of motion in tenths of an inch; the captured value is `0x0492 = 117.0 in`, matching the iPad range-of-motion slider family.
- Android now preserves the UI's 118 in slider ceiling but caps the AA06 wire value to `0x0492 = 117.0 in`, because that is the highest captured iPad preset value and the hardware drifted at the uncapped high end.
- Header bytes `6..9` now decode as the iPad resistance range: `max16 + max8 + min8`. The focused capture default `64 00 64 05` is `5..100 lb`, and the iPad UI can set a low range such as `5..25 lb`.
- Android still mirrors the selected max to `0x3E86 BP_BASE_WEIGHT` and `0x5314 EP_MAX_ALLOWED_FORCE`, but those simple params did not enforce the Custom Curve limiter by themselves during hardware testing.
- The VOLTRA graph exposes four editable anchors; Android interpolates those four anchors into the six wire-format `(x,y)` samples.
- Default editable anchors: `0.0`, `0.24696325`, `0.5802966`, `1.0`, which expands back to the captured iPad default wire curve.
- Loading the mode uses the shared cable-read/load/refresh path: read `0x506A` and `0x3E82`, write `0x3E89=5`, then send `0xAA 13 01`.
- Live Custom Curve force is exposed in `0xB4` packets as mirrored first/last little-endian words in tenths of pounds. `0xAA 0x80` status packets also carry a Custom Curve force snapshot at payload offset 5 when payload starts `80 25 06 00`.

## Row Mode

- The iPad UI shows a Rowing Distance picker with `Just Row`, `50m`, `100m`, `500m`, `1000m`, `2000m`, and `5000m` options.
- The live screen shows total time, total distance meters, pace per 500m, stroke rate, average pace, kcal, projected finish, and splits.
- User-reported setting for this capture: resistance `4`.
- Row entry reuses `0x4FB0 FITNESS_WORKOUT_STATE = 8`, the same value used by Isometric Test.
- A captured Row screen-state frame reports `0x3E89=0x0085`, `0x4FB0=0x08`, `0x5011 APP_CUR_SCR_ID=0x39`, and `0x5467 FITNESS_ONGOING_UI=0x0108`.
- Older Isometric captures can report the same `0x5011` and `0x5467` values, so these fields are useful diagnostics but are not enough to safely relabel workout state `8` as Rowing.
- Live Row telemetry appears in `0xB4`, `0xAA 0x92`, and `0xAA 0x93` packets. Metric byte offsets are still unresolved.
- Candidate Row settings from `paramInfo.csv`:
  - `0x53A7 FITNESS_ROWING_DAMPER_RATIO_IDX`: likely resistance/damper index.
  - `0x53AE EP_ROW_CHAIN_GEAR`: likely simulated cable wear or chain gear.
