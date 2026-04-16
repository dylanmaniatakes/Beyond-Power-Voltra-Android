# 2026-04-15 Android Diagnostic: Dynamic Chains And Eccentric Limits

Source: `/Users/ticnitsi/Downloads/text-0 23.txt`

Follow-up sources: `/Users/ticnitsi/Downloads/text-0 24.txt`, `/Users/ticnitsi/Downloads/text-0 25.txt`

## Summary

- Android connected to `Dylan Voltra 1` with the validated command protocol.
- Parsed readings showed base weight `172 lb`, Chains `28 lb`, Eccentric `0 lb`, and `Strength ready, session active`.
- The base-plus-chains total was exactly `200 lb`, confirming Chains should not use the first captured `0..30 lb` range.
- Follow-up Android diagnostics showed the device also caps Chains to the current base load. At `45 lb`, attempted Chains values above `45 lb` were acknowledged but echoed back as `45 lb`.
- Follow-up Android diagnostics showed Eccentric clamps to the VOLTRA's 5 lb minimum return load. At `45 lb`, an attempted `-45 lb` value was acknowledged but echoed back as `-40 lb`.

## Confirmed Or Promoted Behavior

| Feature | Behavior |
| --- | --- |
| Chains | Direct `BP_CHAINS_WEIGHT` writes remain valid; Android caps the UI/write value to `0..min(baseWeightLb, 200 - baseWeightLb)`. |
| Eccentric | Direct signed `BP_ECCENTRIC_WEIGHT` writes remain valid. Android now exposes the signed range `-baseWeightLb..+baseWeightLb` so positive overload can be tested. |
| Inverse Chains | `FITNESS_INVERSE_CHAIN=0` is confirmed from iPad traffic. Android exposes an independent experimental toggle that writes `1` for on, but the device has not echoed this param in Android diagnostics yet. |

## Representative Frames

- Full state with base `172`, chains `28`, eccentric `-58`, loaded mode:
  `552E04A710AA4F052000100900863EAC00873E1C00883EC6FF893E0500025103035109B04F01E14E012451004585`
- Eccentric reset to `0`:
  `5513040310AA53052000100100883E00003243`
- Full state with base `172`, chains `28`, eccentric `0`, loaded mode:
  `552E04A710AA54052000100900863EAC00873E1C00883E0000893E0500025103035109B04F01E14E01245100D75C`
- Attempted Chains `64 lb` at base `45 lb`, device echoing `45 lb`:
  write `55130403AA100A002000110100873E4000A776`, notify `5513040310AA6C062000100100873E2D006A99`
- Attempted Eccentric `-45 lb` at base `45 lb`, device echoing `-40 lb`:
  write `55130403AA1011002000110100883ED3FF4FCB`, notify `5513040310AA9D062000100100883ED8FF1BBF`

## Remaining Capture Needs

- Clean inverse chains off/on from the official app, including one load/unload for each state.
- A clean official-app capture that changes regular Chains and Inverse Chains independently, because Android currently knows the Inverse Chains toggle but not a separate inverse amount parameter.
- Chains at a low base, a mid base, and a near-limit base to confirm `min(base, 200 - base)` across the range.
- Eccentric at a low base, a mid base, and a near-limit base to confirm negative reduction and positive overload behavior.
- Resistance Band in a clean, separate official-app capture, because the previous sysdiagnose mixed it with Weight Training feature changes.
