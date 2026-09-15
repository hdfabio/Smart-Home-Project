# Consumer data format

Profiles live on the classpath as:

- `src/main/resources/org/engcia/consumer{id}_mean.json` — average load
- `src/main/resources/org/engcia/consumer{id}_max.json` — peak load

Bundled ids: **`1`**, **`2`**. Both files must exist or the UI/RPC rejects the consumer.

## JSON shape

Period `0` is off-peak (`horas_vazias`). Period `1` is peak (`horas_cheias`). Values are **kW**.

```json
{
  "Periods": {
    "0": "horas_vazias",
    "1": "horas_cheias"
  },
  "Total Consumption": { "0": 0.99, "1": 1.73 },
  "Dish washer": { "0": 0.003, "1": 0.14 },
  "Washing Machine": { "0": 0.002, "1": 0.12 },
  "Dryer": { "0": 0.0, "1": 0.05 },
  "Water heater": { "0": 0.06, "1": 0.10 },
  "TV": { "0": 0.002, "1": 0.027 },
  "Microwave": { "0": 0.022, "1": 0.045 },
  "Kettle": { "0": 0.033, "1": 0.053 },
  "Lighting": { "0": 0.008, "1": 0.054 },
  "Refrigerator": { "0": 0.031, "1": 0.038 }
}
```

Mapped by `org.engcia.model.Appliances` (`@JsonProperty` names must match).

## How the engine uses it

From **max** profile:

- `Max Consumption` = max(peak, off-peak) total
- `Washing Machine Max` / `Dish washer Max` / `Refrigerator Max` = per-appliance peak
- `Best Contracted Power` = lowest Portuguese tier in `ContractedPower` that covers max consumption

From **mean** profile:

- `Average Consumption` = mean of peak and off-peak totals
- `Average Consumption Bi-Schedule` = off-peak total (used by the bi-schedule rule)

`Total Consumption` is **simultaneous** household load, not the sum of every appliance peak (appliances do not all run at once).

## Live overrides (MQTT / RPC)

You can push either:

1. The **full profile** object above, or
2. A compact meter payload:

```json
{
  "maxKw": 4.2,
  "meanKw": 1.1,
  "offPeakMeanKw": 0.8,
  "washingMachineMax": 2.1,
  "dishWasherMax": 1.8,
  "refrigeratorMax": 0.14
}
```

Compact fields update the matching numerical facts. Missing fields keep the JSON file values (or the last live update).
