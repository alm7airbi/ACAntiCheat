# Upgrade & Migration Guide

This guide covers safe upgrades of UltimateAntiCheat between 0.1.x releases, with config-version support and automatic backups.

## Pre-upgrade checklist
- Back up your `plugins/ACAntiCheat/` directory (configs, logs, structured JSONL, persistence flat-files).
- Note your current `config-version` reported by `/acac config` or during startup.
- For Mongo/SQL deployments, ensure the database is backed up before changing versions.

## Config migration behavior
- `config.yml` declares `config-version`. On startup or `/acac reload`, UAC compares the file's version to the expected version baked into the build.
- If the file is older, UAC:
  1. Creates a timestamped backup (e.g., `config.yml.bak-YYYYMMDD-HHmmss`).
  2. Merges new defaults while preserving existing values when possible.
  3. Logs migration results and validation warnings.
- If the config is missing or unreadable, a fresh default `config.yml` is generated and the old file is backed up if present.
- Validation failures are logged with details; the plugin continues with safe defaults but you should correct the file and rerun `/acac reload`.

## Upgrading the plugin jar
1. Build or download the new jar. For production Paper, use `./gradlew clean build -PrealPaper` on a networked machine; offline CI can keep using the stub jar.
2. Stop the server and replace the old jar in `plugins/`.
3. Start the server. Watch startup logs for migration/validation messages, backend selection (flat-file vs Mongo/SQL), and integration mode (stub vs paper).
4. Run `/acac config` and `/acac storage` to confirm:
   - Config version matches the build's expected version.
   - Migrations (if any) succeeded and validation is OK.
   - Persistence backend is healthy with no recent errors.
5. Spot-check `/acac perf` and `/acac alerts` (if added) to ensure alert sinks are running as expected.

## Troubleshooting migrations
- If migration fails repeatedly, restore from the generated backup and reapply your changes incrementally.
- Ensure `config-version` is not manually changed to a higher number; UAC only upgrades forwards.
- For Mongo/SQL, verify credentials/hostnames and increase timeouts/retries in `storage.mongo-*` or `storage.sql.*` if needed.

## Rollback plan
- Keep the previous jar and the most recent `config.yml.bak-*` so you can revert quickly.
- Downgrades are not automatically supported; when rolling back, restore the backup config and any persistence snapshots that match the prior version.
