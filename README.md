# UltimateAntiCheat (UAC)

## Requirements
- Java 17+ runtime
- Paper/Spigot 1.20.x server (api compatibility)
- ProtocolLib installed on the server at runtime (not bundled in this repo)
- Optional: MongoDB when `storage.use-database=true`

## Quick Start
1. **Setup prerequisites**
   - Install Java 17+ locally so the Gradle wrapper can launch the preferred toolchain.
   - Prepare a Paper/Spigot 1.20.x server and install ProtocolLib when real mode is required.
2. **Build locally**
   - Stub build: `./gradlew clean build`
   - Real Paper build: `./gradlew clean build -PrealPaper -PenableRemoteRepos`
3. **Deploy**
  - Drop `build/libs/UltimateAntiCheat-0.1.0-stub.jar` or `...-paper.jar` into the server's `plugins/` directory.
   - Adjust `config.yml` (set `integrations.mode=paper`/`auto` when ProtocolLib is available).
4. **Monitor**
   - Logs land under `plugins/ACAntiCheat/logs`.
   - Use `/acac selftest`, `/acac perf`, and `/acac storage` to verify hooks and storage health before onboarding players.

## Building
1. The tracked `gradle/wrapper/gradle-wrapper.jar` stays in the repo; run `./gradlew wrapper` only if it is missing.
2. Execute `./gradlew clean build` (add `-PrealPaper -PenableRemoteRepos` to pull the real Paper/ProtocolLib APIs).
3. Stub artifacts resolve to `build/libs/UltimateAntiCheat-0.1.0-stub.jar`; the real profile produces `...-paper.jar`.

## Runtime dependencies
- ProtocolLib must be installed on the Paper server; it is declared as a `compileOnly` dependency and should stay out of source control.
- The MongoDB Java driver is referenced when `storage.use-database=true`; real builds download it via Gradle when remote repos are allowed.

## Installing on Paper
1. Copy the assembled jar from `build/libs` into the Paper server's `plugins/` folder.
2. Keep ProtocolLib installed in the same server instance.
3. Restart the server, tune `config.yml`, and verify staff commands activate.

## Build Profiles
- **Stub (default)**: Bundled Bukkit/ProtocolLib shims run in offline environments. Command: `./gradlew clean lint build`. Output: `build/libs/UltimateAntiCheat-0.1.0-stub.jar`.
- **Real Paper**: Resolves Paper + ProtocolLib from remote repos, drops stub classes, and targets Java 21 bytecode for those APIs. Command: `./gradlew clean lint build -PrealPaper -PenableRemoteRepos`. Output: `build/libs/UltimateAntiCheat-0.1.0-paper.jar`.
- **CI**: GitHub Actions runs stub + realPaper pipelines (`lint`, `selfTest`, `build`) and uploads classifier-tagged artifacts while enforcing dependency review + CodeQL.

## Runtime Highlights
- Configurable checks (`checks.*`) track packet/Netty crashers, invalid teleports, chunk hops, inventory dupe spikes, redstone abuse, console spam, and disabler detection.
- Mitigations escalate through warn -> rollback -> throttle -> rubber-band -> kick -> ban ladders (`mitigation.*` thresholds + cooldowns).
- Persistence defaults to schema-versioned flat file storage; enable Mongo/SQL via `storage.*`. Failures fall back to flat file and emit warnings.
- Experiments log to `logs/acac-experiments.jsonl` when `experiments.enabled=true`, with sampling, optional player data, and rotation controls.
- Alerts route to console, staff chat, and optional Discord webhooks (`alerts.*`). Structured logging mirrors rotation settings under `alerts.logging.*`.
- PacketEvents is intentionally unsupported - ProtocolLib is required for packet interception; ViaVersion, LuckPerms, and DiscordSRV are optional soft hooks.

## Staff Commands
- `/acac gui`: paginated staff control GUI (stubbed offline). Requires `acac.use` and `acac.gui.manage` for toggles.
- `/acac stats <player>`: trust score, packet rates, flag tallies, and mitigation notes.
- `/acac inspect <player>` / `/acac history <player>`: deep dives plus persistence visibility.
- `/acac storage`: shows backend, schema version, cache size, queue depth, and last migration/error (`acac.admin`).
- `/acac perf` + `/acac selftest`: performance data and synthetic traffic validation (`acac.admin` recommended).
- `/acac reload` / `/acac debug`: apply config changes and toggle verbose logging for troubleshooting.

## Testing Checklist
1. Start a Paper 1.20+ server with ProtocolLib and set `integrations.mode=paper`/`auto`.
2. Verify `/acac stats <you>` shows LOW risk when idle.
3. Trigger packet bursts or invalid teleports to confirm rubber-band/cancel/kick mitigations and logs.
4. Spam inventory slots, observe rollbacks, and check `/acac inspect` data.
5. Ramp up redstone/entity abuse to test throttles + console spam detection.
6. Run `/acac selftest`, inspect `/acac history`, and run `/acac perf` for persistence/timing confidence.

## Configuration Snapshot
- `checks.*`: tune windows, thresholds, and mitigation actions per check (`invalid-item`, `packet-rate-limit`, `invalid-teleport`, etc.).
- `integrations.mode`: `stub` (default) for offline builds, `paper`/`auto` to bind real ProtocolLib hooks.
- `alerts.*`: routing, throttles, webhook URL + retry/backoff, structured logging via `alerts.logging.*`.
- `experiments.*`: opt-in JSONL telemetry with sampling, metadata toggles, and rotation caps.
- `persistence.*` & `storage.*`: retention, caching, schema version, and backend selection (flat file/Mongo/SQL).
- `config-version`: bumping config merges defaults and backs up old files; use `/acac config` to inspect validation/migration status.

## Documentation
- [`docs/INSTALLATION.md`](docs/INSTALLATION.md): production Paper install guide.
- [`docs/UPGRADE_AND_MIGRATION.md`](docs/UPGRADE_AND_MIGRATION.md): config version handling and rollout steps.
- [`docs/TROUBLESHOOTING.md`](docs/TROUBLESHOOTING.md): startup, performance, and GUI fixes.
- [`docs/RELEASE_NOTES_0.1.x.md`](docs/RELEASE_NOTES_0.1.x.md): release highlights, limitations, and upgrade reminders.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md): architecture, roadmap, performance plan, and testing strategy.

## CI & Releases
- GitHub Actions run stub + realPaper matrices (`lint`, `selfTest`, `build`) and upload classifier-tagged artifacts.
- Dependency Review + CodeQL guard the supply chain and static analysis on PRs.
- Tagged pushes (`v*`) trigger `release.yml`, build both profiles, and attach the jars to the GitHub release.
