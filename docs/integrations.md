# Integrations: JSON-RPC and MQTT

The desktop app starts two optional services (see `application.properties`):

| Service | Default | Disable |
|---------|---------|---------|
| JSON-RPC HTTP | `http://localhost:8080/rpc` | `RPC_ENABLED=false` |
| MQTT client | `tcp://localhost:1883` | `MQTT_ENABLED=false` |

If the MQTT broker is down, the app still runs with classpath JSON profiles. If port 8080 is busy, RPC is skipped and the UI still works.

## JSON-RPC 2.0 (HTTP)

`POST /rpc` with `Content-Type: application/json`.

Health check: `GET /health` → `{"status":"ok"}`.

### Methods

| Method | Params | Result |
|--------|--------|--------|
| `setConsumer` | `consumerId` (`"1"` or `"2"`) | `{ "consumerId": "1" }` |
| `getQuestions` | none | remaining categorical / numerical questions |
| `answer` | `description`, `value` | `{ "ok": true, "pending": … }` |
| `recommend` | none | `{ "consumerId", "conclusions": [ … ] }` |
| `how` | `factId` (int) | How explanation for that fact |
| `whyNot` | `expectedConclusion` e.g. `Conclusion("Change to Bi-Schedule")` | Why a conclusion did not fire |
| `upsertConsumption` | compact or full profile; optional `consumerId` | `{ "ok": true, "consumerId" }` |

### curl (PowerShell)

```powershell
curl.exe -s -X POST http://localhost:8080/rpc -H "Content-Type: application/json" --data-raw "{\"jsonrpc\":\"2.0\",\"method\":\"setConsumer\",\"params\":{\"consumerId\":\"1\"},\"id\":1}"
```

```powershell
curl.exe -s -X POST http://localhost:8080/rpc -H "Content-Type: application/json" --data-raw "{\"jsonrpc\":\"2.0\",\"method\":\"answer\",\"params\":{\"description\":\"Contracted Power is Dual day(2 Schedules)\",\"value\":\"no\"},\"id\":2}"
```

```powershell
curl.exe -s -X POST http://localhost:8080/rpc -H "Content-Type: application/json" --data-raw "{\"jsonrpc\":\"2.0\",\"method\":\"upsertConsumption\",\"params\":{\"maxKw\":4.2,\"meanKw\":1.1,\"offPeakMeanKw\":0.8},\"id\":3}"
```

```powershell
curl.exe -s -X POST http://localhost:8080/rpc -H "Content-Type: application/json" --data-raw "{\"jsonrpc\":\"2.0\",\"method\":\"recommend\",\"id\":4}"
```

Typical error body:

```json
{
  "jsonrpc": "2.0",
  "error": { "code": -32602, "message": "No consumer session. Call setConsumer first." },
  "id": 4
}
```

Call `setConsumer` then `answer` for each remaining question (or a subset) before `recommend`. Rules that need evidence will not fire until those facts exist.

## MQTT

Topics (QoS 1 subscribe):

| Topic | Direction | Payload |
|-------|-----------|---------|
| `smarthome/{consumerId}/consumption` | in | full profile JSON or compact `{ "maxKw", "meanKw", "offPeakMeanKw", … }` |
| `smarthome/{consumerId}/recommendations` | out | `{ "consumerId", "conclusions": [ … ] }` after each successful `recommend` |
| `smarthome/{consumerId}/rpc/request` | in | same JSON-RPC body as HTTP |
| `smarthome/{consumerId}/rpc/response` | out | JSON-RPC result or error |

The `{consumerId}` in the topic is used when the JSON-RPC body omits `consumerId`.

### Publish a live meter reading

```bash
mosquitto_pub -h localhost -t smarthome/1/consumption -m "{\"maxKw\":4.2,\"meanKw\":1.1,\"offPeakMeanKw\":0.8}"
```

### JSON-RPC over MQTT

```bash
mosquitto_sub -h localhost -t smarthome/1/rpc/response &
mosquitto_pub -h localhost -t smarthome/1/rpc/request -m "{\"jsonrpc\":\"2.0\",\"method\":\"recommend\",\"id\":10}"
```

Subscribe to advice:

```bash
mosquitto_sub -h localhost -t smarthome/1/recommendations
```

Consumption updates **facts only**. Call `recommend` (UI, HTTP, or MQTT RPC) to fire rules and publish conclusions.

## Home Assistant

This project is not a Home Assistant add-on. HA can still:

1. Publish meter sensors to `smarthome/1/consumption` (MQTT statestream, automation, or Node-RED).
2. Subscribe to `smarthome/1/recommendations` as an MQTT sensor.

Example MQTT sensor:

```yaml
mqtt:
  sensor:
    - name: Smart Home recommendations
      state_topic: smarthome/1/recommendations
      value_template: "{{ value_json.conclusions[0] if value_json.conclusions else 'none' }}"
      json_attributes_topic: smarthome/1/recommendations
```

## Config reference

`src/main/resources/application.properties`:

```
mqtt.enabled=true
mqtt.broker=tcp://localhost:1883
mqtt.clientId=smart-home-expert
rpc.enabled=true
rpc.port=8080
```

Environment overrides: `MQTT_ENABLED`, `MQTT_BROKER`, `MQTT_CLIENT_ID`, `RPC_ENABLED`, `RPC_PORT`.
