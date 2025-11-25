# Installation & Setup

This guide targets Paper 1.20.x with Java 17+ and ProtocolLib. Stub/offline compilation remains available for CI.

## Prerequisites
- Java 17 or later on the server.
- Paper (recommended) or Spigot 1.20.x.
- ProtocolLib installed when running in paper/auto integration mode.
- Optional soft hooks: ViaVersion for protocol awareness, LuckPerms for permission lookups, DiscordSRV for alert relays.

## Building the plugin
- Offline/stub build: `./gradlew clean build --warning-mode all --console=plain --no-daemon`
- Real Paper/ProtocolLib build: `./gradlew clean build -PrealPaper --warning-mode all --console=plain --no-daemon`
- Produced artifacts: `build/libs/UltimateAntiCheat-0.1.0-stub.jar` (default stub/CI) or `build/libs/UltimateAntiCheat-0.1.0-paper.jar` (real Paper profile)

## Deployment steps
1. Place the jar into your server's `plugins/` directory.
2. Ensure ProtocolLib is present for real integrations; set `integrations.mode: auto` (default) or `paper` in `config.yml`. Use `stub` only for offline testing.
3. (Optional) Configure persistence: keep the default flat-file, or enable Mongo (`storage.use-database`) or SQL (`storage.use-sql-database`) with valid credentials.
4. (Optional) Enable alerting sinks: in-game staff broadcasts, structured JSONL logging under `alerts.logging.*`, and Discord webhooks under `alerts.discord-*`.
5. Restart the server. The plugin logs which integration mode is active, persistence backend selection, config-version, and any validation warnings.
6. Verify with `/acac selftest` and `/acac perf` to ensure hooks and performance budgets are healthy.

## First-week hardening checklist
- Run `/acac gui` and `/acac inspect <player>` on trusted staff to see expected LOW risk outputs.
- Keep mitigation modes at WARN/ROLLBACK while validating thresholds; escalate to THROTTLE/RUBBERBAND/KICK as confidence grows.
- Monitor `plugins/ACAntiCheat/logs` and structured JSONL under `alerts.logging.directory` for early anomalies.
- Confirm `/acac storage` reports the desired backend and no recent errors.
- Confirm Discord/webhook delivery if enabled; adjust severity filters and retry/backoff as needed.
