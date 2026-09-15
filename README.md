# Smart Home Energy Optimization Tool

Desktop **expert system** for residential energy advice. You pick a consumer profile, answer a short questionnaire, and Drools rules recommend changes such as contracted power, bi-schedule tariffs, solar panels, night-time appliances, and electric transport.

Industry-style JavaFX UI plus optional **JSON-RPC** and **MQTT** so other tools can feed live consumption and ask for the same recommendations.

## Requirements

- **JDK 17** (JavaFX 17)
- **Maven 3.8+**
- Optional: **Docker** (Mosquitto MQTT broker) and `mosquitto_pub` / `curl`

## Run the desktop app

```bash
mvn clean javafx:run
```

1. Enter consumer number **`1`** or **`2`** and click **GO**.
2. Click **Start new Analysis** and answer the pop-up questions.
3. On the last step (**See Options** / **Close**), the engine fires and explanations appear in the main text area.

**Question units**

- Distance: **km**
- Solar area: **m²**
- Contracted power: **kW** (not kWh)
- Appliance efficiency: `A+++`, `A++`, `A+`, `A`, `B`, `C`, `D` (refrigerators have no `D`)
- Yes/no questions: `yes` or `no`

If JSON-RPC is enabled (default), `http://localhost:8080/health` should respond while the app is running. If Mosquitto is not running, MQTT is skipped and file-based consumer profiles are still used.

## Optional MQTT broker

```bash
docker compose up -d
```

Or:

```bash
docker run -p 1883:1883 -v ${PWD}/docker/mosquitto.conf:/mosquitto/config/mosquitto.conf eclipse-mosquitto:2
```

Configuration lives in `src/main/resources/application.properties` (override with env vars `MQTT_BROKER`, `MQTT_ENABLED`, `RPC_PORT`, `RPC_ENABLED`).

## Quick JSON-RPC example

With the app running (`mvn javafx:run`), open another terminal.

**PowerShell** (write the body to a file so quotes stay intact):

```powershell
@"
{"jsonrpc":"2.0","method":"setConsumer","params":{"consumerId":"1"},"id":1}
"@ | Set-Content -Encoding utf8 $env:TEMP\rpc.json
curl.exe -s -X POST http://localhost:8080/rpc -H "Content-Type: application/json" --data-binary "@$env:TEMP\rpc.json"
```

Then answer questions with method `answer` (`description` = fact key from `getQuestions`, `value` = `yes`/`no`/number), and call `recommend`.

Health check: `curl.exe -s http://localhost:8080/health`

See [docs/integrations.md](docs/integrations.md) for MQTT topics, RPC methods, and Home Assistant notes.

## Documentation

| Doc | Contents |
|-----|----------|
| [docs/architecture.md](docs/architecture.md) | Packages, facts, rules, How/WhyNot |
| [docs/data-format.md](docs/data-format.md) | Consumer JSON (`*_max` / `*_mean`) |
| [docs/integrations.md](docs/integrations.md) | MQTT + JSON-RPC + sample payloads |

The `prolog/` folder is a leftover prototype. It is **not** used at runtime.

## Ideas backlog

- Show WhyNot in the JavaFX UI (engine method already exists)
- Consistent Portuguese/English question strings (`BI_SHEDULE`, mixed labels)
- Unit tests for `Calculate` and a few Drools scenarios
- Restore Excel/`Book1.xlsx` import or drop that unused path
- Home Assistant MQTT discovery and E-Redes/smart-meter CSV import
