# Voltra Controller HTTP Gateway

Voltra Controller includes an optional local HTTP gateway so your Android device can act as a relay between your Voltra and local tools such as:

- `curl`
- browser-based dashboards
- local scripts
- Home Assistant helpers
- Node-RED or other LAN automation tools

The gateway is disabled by default and can be enabled from the app's **More** screen.

## How It Works

When enabled, the app opens a small local HTTP server on your Android device.

That server:

- exposes live Voltra state as JSON
- accepts command requests over HTTP
- relays those commands through the same Bluetooth connection the app already uses

The gateway is meant for local-network use.
It is not a public cloud API.

## Security

Every `/v1/...` endpoint requires the HTTP gateway access key.

Send it in either of these ways:

- `X-Voltra-Key: <your-key>`
- `Authorization: Bearer <your-key>`

The app shows the current key and listening URLs in **More > HTTP Gateway**.

## Health Check

No key required.

### GET `/health`

Returns a simple status response.

```bash
curl http://PHONE_IP:8788/health
```

## Gateway Info

Key required.

### GET `/v1/info`

Returns the gateway version, current URLs, and the command list.

```bash
curl -H "X-Voltra-Key: YOUR_KEY" http://PHONE_IP:8788/v1/info
```

## Live State

Key required.

### GET `/v1/state`

Returns a focused JSON snapshot of:

- connection state
- protocol status
- current device
- target load
- live reading fields
- safety state

```bash
curl -H "X-Voltra-Key: YOUR_KEY" http://PHONE_IP:8788/v1/state
```

## Command Endpoints

All command endpoints:

- use `POST`
- require the gateway key
- return a JSON wrapper with the underlying `VoltraCommandResult`

Example response shape:

```json
{
  "ok": true,
  "result": {
    "command": "SET_TARGET_LOAD",
    "status": "CONFIRMED",
    "message": "set target load",
    "timestampMillis": 1776230000000,
    "rawHex": "..."
  }
}
```

## Basic Workout Control

### POST `/v1/commands/load`

```bash
curl -X POST \
  -H "X-Voltra-Key: YOUR_KEY" \
  http://PHONE_IP:8788/v1/commands/load
```

### POST `/v1/commands/unload`

```bash
curl -X POST \
  -H "X-Voltra-Key: YOUR_KEY" \
  http://PHONE_IP:8788/v1/commands/unload
```

### POST `/v1/commands/exit`

```bash
curl -X POST \
  -H "X-Voltra-Key: YOUR_KEY" \
  http://PHONE_IP:8788/v1/commands/exit
```

## Mode Switching

### Weight Training

`POST /v1/commands/mode/weight-training`

### Resistance Band

`POST /v1/commands/mode/resistance-band`

### Damper

`POST /v1/commands/mode/damper`

### Isokinetic

`POST /v1/commands/mode/isokinetic`

### Isometric Test

`POST /v1/commands/mode/isometric`

Example:

```bash
curl -X POST \
  -H "X-Voltra-Key: YOUR_KEY" \
  http://PHONE_IP:8788/v1/commands/mode/isokinetic
```

## Weight Training Commands

### Set Target Load

`POST /v1/commands/target-load`

Body:

```json
{ "value": 25, "unit": "lb" }
```

### Assist

`POST /v1/commands/assist`

Body:

```json
{ "enabled": true }
```

### Chains

`POST /v1/commands/chains`

Body:

```json
{ "value": 10, "unit": "lb" }
```

### Eccentric

`POST /v1/commands/eccentric`

Body:

```json
{ "value": 10, "unit": "lb" }
```

### Inverse Chains

`POST /v1/commands/inverse-chains`

Body:

```json
{ "enabled": true }
```

## Resistance Band Commands

### Set Force

`POST /v1/commands/resistance-band/force`

```json
{ "value": 30, "unit": "lb" }
```

### Standard / Inverse

`POST /v1/commands/resistance-band/mode`

```json
{ "inverse": true }
```

### Power Law / Logarithm

`POST /v1/commands/resistance-band/curve`

```json
{ "logarithm": true }
```

### Progressive Length

`POST /v1/commands/resistance-band/progressive-length`

```json
{ "rom": false }
```

### Band Length

`POST /v1/commands/resistance-band/length`

```json
{ "inches": 42 }
```

## Damper Commands

### Set Damper Level

`POST /v1/commands/damper-level`

```json
{ "level": 7 }
```

## Isokinetic Commands

### Menu

`POST /v1/commands/isokinetic/menu`

```json
{ "mode": "isokinetic" }
```

or

```json
{ "mode": "constant_resistance" }
```

### Target Speed

`POST /v1/commands/isokinetic/target-speed`

```json
{ "mps": 0.5 }
```

### Speed Limit

`POST /v1/commands/isokinetic/speed-limit`

```json
{ "mps": 1.0 }
```

### Constant Resistance

`POST /v1/commands/isokinetic/constant-resistance`

```json
{ "value": 15, "unit": "lb" }
```

### Max Eccentric Load

`POST /v1/commands/isokinetic/max-eccentric-load`

```json
{ "value": 50, "unit": "lb" }
```

## Notes

- The HTTP gateway depends on the phone or tablet already being connected to the Voltra over Bluetooth.
- The gateway is intended for LAN use while the app process is alive.
- Commands still go through the app's normal BLE safety and queue handling.
- If the app is disconnected from the Voltra, command endpoints will return the underlying blocked or failed command result.
