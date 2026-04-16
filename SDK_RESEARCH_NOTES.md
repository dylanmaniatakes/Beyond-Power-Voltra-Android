# External SDK Research Notes

Source reviewed: [HJewkes/voltra-node-sdk](https://github.com/HJewkes/voltra-node-sdk)

These notes are for product and protocol planning only. They are not treated as confirmed device behavior unless matched against hardware captures from this project.

## Useful Ideas To Borrow

- Multi-device session management:
  - The SDK is designed to connect to more than one VOLTRA at once.
  - That is a strong reference point for future paired-companion support in this app.

- Recording lifecycle as an explicit state machine:
  - The SDK separates `prepare`, `start`, and `stop` recording.
  - That lines up with the control patterns we have already seen around load-ready vs active states, especially in Isometric and other guided modes.

- Stream-first telemetry API:
  - The SDK treats telemetry as a live frame stream instead of one-off reads.
  - That is a good model for improving local workout history, HTTP gateway streaming, and MQTT sensor publishing.

- Settings surface worth comparing against our recovered protocol:
  - The SDK exposes weight, chains, inverse chains, eccentric, battery, and workout mode together.
  - It also treats rep/set boundaries and mode confirmations as first-class events.
  - That suggests we should keep pushing toward event-driven UI updates instead of relying on slower refresh polling.

- Cross-platform abstraction:
  - The SDK targets browser, Node.js, and React Native.
  - Our `core:model` and `core:protocol` split is still the right long-term shape if we want Android, macOS, and Home Assistant support from the same protocol work.

## Concrete Follow-Ups For This Repo

1. Add a lightweight event stream on top of the existing BLE client so the HTTP gateway can expose push-style state later instead of request/response only.
2. Keep local workout history as a first-class feature, using the same event stream that powers MQTT and future HTTP subscriptions.
3. Revisit paired-companion support after the single-device alpha is stable; the external SDK is a useful reference for device-manager shape.
4. Keep validating command ranges against real captures before widening UI limits, even when another SDK claims broader ranges.

## Important Caution

The external SDK is useful as a design reference, but not as proof of protocol correctness for this app.

Before adopting any new behavior from it, confirm at least one of the following:

- a matching VOLTRA capture from our own testing
- a matching parameter definition from `paramInfo.csv`
- a matching parsed notification or write path already recovered in this repository
