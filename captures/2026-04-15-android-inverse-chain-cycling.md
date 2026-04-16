# 2026-04-15 Android Diagnostic: Inverse Chains Cycling

Source: `/Users/ticnitsi/Downloads/text-0 31.txt`

## Summary

- The Android app was connected, protocol validated, and loaded in Weight Training at 5 lb.
- The final readings included `FITNESS_INVERSE_CHAIN=true`, `BP_CHAINS_WEIGHT=0`, and `BP_ECCENTRIC_WEIGHT=0`.
- Android command logs show direct `FITNESS_INVERSE_CHAIN` off/on writes ACKing, and follow-up reads returning the flag.
- The later amount cycling in this diagnostic was still regular `BP_CHAINS_WEIGHT` traffic, so it should not be treated as proof that Inverse Chains amount uses the Chains amount command.

## Confirmed/Observed

| Item | Evidence |
| --- | --- |
| Inverse flag write on | `SET_INVERSE_CHAINS on`, `0x53B0=1` |
| Inverse flag write off | `SET_INVERSE_CHAINS off`, `0x53B0=0` |
| Inverse flag readback | Bulk read responses include `B0 53 01` while the diagnostic summary reports `Inverse chains: true` |
| Chains amount | Amount cycling remains `0x3E87 BP_CHAINS_WEIGHT`, echoed independently in async state frames |

## App Decision

- Keep Chains and Inverse Chains independent in the UI.
- Chains continues to write only `BP_CHAINS_WEIGHT`.
- Inverse Chains writes only the `FITNESS_INVERSE_CHAIN` enabled flag for now.
- Do not fake an Inverse Chains amount edit by writing `BP_CHAINS_WEIGHT`; the independent inverse amount command needs a clean capture.

## Needed Capture

1. Set regular Chains to 0.
2. Set Inverse Chains to a nonzero amount from the official app or device screen.
3. Export immediately.
4. Change only Inverse Chains amount twice.
5. Repeat once while regular Chains is also nonzero.
