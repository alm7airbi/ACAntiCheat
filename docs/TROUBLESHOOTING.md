# Troubleshooting & FAQ

## Common startup issues
- **ProtocolLib missing:** Set `integrations.mode: stub` temporarily or install ProtocolLib and restart. In Paper mode, the plugin logs a warning and remains in log-only if ProtocolLib is absent.
- **Config validation warnings:** See `/acac config` for details. UAC will back up and regenerate defaults if the file is unreadable or outdated; review `config.yml.bak-*` if you need to restore custom values.
- **Persistence backend failures:** If Mongo/SQL cannot connect, UAC falls back to flat-file and records the error timestamp. Check `/acac storage` and adjust credentials, timeouts, or retries.
- **Webhook errors:** Webhook sends are async with retry/backoff. Verify the URL, adjust severity filters, and check `/acac perf` (alerts section) for last send status.

## Reducing false positives
- Start with `mitigation.*` thresholds at WARN/ROLLBACK. Use `/acac stats <player>` and `/acac inspect <player>` to observe trust/flags before escalating to THROTTLE/RUBBERBAND/KICK.
- Adjust per-check sensitivity under `checks.*` for high-latency players. TPS and ping are baked into movement/packet thresholds, but extreme lag may still require relaxing limits.

## Performance tuning
- `/acac perf` shows average handler timings versus configured budgets (`performance.*`). If a check exceeds its budget, consider loosening thresholds or disabling that check temporarily.
- Structured logging and webhook dispatch run asynchronously; ensure disk space is sufficient for rotation/retention settings under `alerts.logging.*`.
- Persistence cache and flush cadence are configurable under `persistence.cache.*`; raise limits gradually to avoid memory pressure.

## GUI/command issues
- GUIs require in-game execution with `acac.use` and, for toggles/mitigation changes, `acac.gui.manage`. Console can use `/acac stats/inspect/history/storage/perf/config` for text output.
- If clicks are ignored, ensure you are using the Paper build (`-PrealPaper`) with ProtocolLib installed; stub mode shows GUIs but does not enforce actions.

## Self-test guidance
- `/acac selftest` exercises packet and mitigation paths for a synthetic player. Run it after configuration changes to confirm alerts and structured logging are flowing.
- For deeper checks, join a test server and intentionally trigger movement/teleport/interaction abuse in a controlled area while observing `/acac stats`, `/acac inspect`, and logs.
- Enable `experiments.enabled` during observation sessions to capture JSONL telemetry in `logs/acac-experiments.jsonl` (rotated by size). These lines can be shared with tooling/LLMs for postmortem analysis; disable afterward if you do not need the extra disk writes.

## When to file a bug
- Include server version, ProtocolLib version, `config-version`, integration mode, persistence backend, and relevant log excerpts (structured JSONL and plain logs).
- Provide steps to reproduce and whether mitigations triggered (kick/ban/rubber-band) or remained in warn-only.
