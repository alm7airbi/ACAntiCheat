# Release Notes (0.1.x)

## Highlights
- ProtocolLib-first anti-exploit coverage: packet/movement validation, invalid items/NBT, placement/redstone/crash protections, and command abuse detection with TPS/ping-aware thresholds.
- Mitigation ladder: warn → rollback → throttle → rubber-band → kick/ban with cooldowns, audit logging, and optional external punishment commands.
- Pluggable integrations: stub/offline builds for CI and real Paper/ProtocolLib bridges with ViaVersion/LuckPerms/DiscordSRV soft hooks.
- Persistence: flat-file, Mongo (reflection), and SQL/JDBC backends with schema-versioned snapshots, caching, async flush, and `/acac storage` diagnostics.
- Staff UX: `/acac gui|stats|inspect|history|perf|storage|config|selftest` with permissions, cooldowns, pagination, high-risk filters, and live backend/webhook indicators.
- Alerts: console/staff channels, structured JSONL with rotation/retention, and async Discord webhooks with retry/backoff and embed payloads.
- Config: versioned `config.yml` with validation, backup/regeneration, and live reload propagation.
- Experiment telemetry: optional JSONL experiment logging (`logs/acac-experiments.jsonl`) with sampling, size rotation, and optional player/location fields for detections/mitigations/self-tests.

## Known limitations
- PacketEvents remains unsupported; ProtocolLib is required for packet interception in paper/auto modes.
- Marketplace packaging (icons/listings) is not included in this repository.
- `alerts.notify-permission` remains reserved for a future notification routing change; it is not used in 0.1.x.

## Upgrade guidance
- Use `/acac config` to verify config-version after upgrading; backups are created automatically when migrating older configs.
- Validate persistence and alert sinks via `/acac storage` and `/acac perf` after deploy.
- Keep mitigations at WARN/ROLLBACK until thresholds are tuned on your server.
