# UltimateAntiCheat (UAC)

A modular Paper/Spigot anti-exploit framework focused on hardened packet handling, crash mitigation, and staff-friendly tooling.

- Comprehensive coverage: packet/Netty crashers, server crashers, smart packet limiting, invalid items/NBT, anti-cheat disablers, entity overload, redstone exploits, invalid actions/placements/signs, duping, console spam, and more.
- Ecosystem-first: ProtocolLib-first bridges with ViaVersion protocol awareness, optional permission lookups via LuckPerms, and Discord/webhook alerts (webhook + DiscordSRV relay). PacketEvents remains intentionally unsupported in this release.
- Staff experience: `/acac stats <player>` with inventory GUIs, chat summaries, and configurable alert sinks.

## Getting Started
- Requires Java 17+ and Paper/Spigot 1.20+ (API) with ProtocolLib recommended.
- Build with Gradle: `./gradlew build` (uses the locally available Gradle install to support offline builds on Gradle 8.x in this environment). The build auto-detects the highest installed JDK (17+) and still emits Java 17 bytecode via `--release 17` so Paper servers on Java 17 remain supported.
- Copy the assembled jar to your `plugins/` folder and configure `config.yml`.
- Gradle 9 preparation: the build currently passes on Gradle 8.x with no deprecations; set aside time to validate the upgrade
  path to Gradle 9.0 soon.

### Building offline vs. real Paper/ProtocolLib
- **Offline/stub jar (default here):** `./gradlew clean build` will package a jar that compiles against the bundled Bukkit/ProtocolLib stubs and runs in this restricted environment. Set `integrations.mode: stub` to force stub bridges.
- **Real Paper/ProtocolLib jar:** on a normal network, build with `./gradlew clean build -PrealPaper` to pull the real Paper + ProtocolLib APIs (stubs are excluded automatically) and set `integrations.mode: paper` or `auto`. The runtime will choose the Paper bridges, register real ProtocolLib packet listeners (movement, interactions, payloads) and Paper event bindings, and execute mitigations (kicks, cancels, rollbacks, rubber-bands) instead of log-only stubs.
- On startup the plugin logs whether stub or real integrations are active and warns if ProtocolLib is missing while Paper mode is requested.
- CI runs both stub (`./gradlew clean lint build`) and real Paper (`./gradlew clean lint build -PrealPaper`) profiles with the stub-only self-test harness, lightweight lint pass, and artifact uploads for each mode.

### Building for Paper / Production
- Real Paper builds require Paper 1.20.x and ProtocolLib on the server; build with `./gradlew clean build -PrealPaper` so the real APIs are used instead of the stubs.
- Stub/CI builds stay on the default `./gradlew clean build` path (with lint + selfTest) and ship the lightweight Bukkit/ProtocolLib shims for offline environments.
- The produced jars are classifier-tagged (`build/libs/UltimateAntiCheat-0.1.0-stub.jar` or `build/libs/UltimateAntiCheat-0.1.0-paper.jar`); drop the desired one into `plugins/` alongside ProtocolLib, choose `integrations.mode: paper/auto`, and restart.
- Persistence defaults to flat-file in this offline build with schema-versioned snapshots, in-memory caching, and async flush. Enable Mongo by setting `storage.use-database: true` with a valid URI/credentials, or opt into SQL by setting `storage.use-sql-database: true` with a valid JDBC URL/credentials. Connection failures log a warning and fall back to flat-file without crashing. `/acac perf` and `/acac storage` report the active persistence backend, cache health, migrations, and last error timestamps.
- Logs are written under `plugins/ACAntiCheat/logs` (flags, mitigations, trust changes) in both modes so staff can review trends.
- Experiments/telemetry: enable `experiments.enabled` to capture structured JSONL lines (default `logs/acac-experiments.jsonl`) for detections/mitigations/self-tests, sampled via `experiments.sample-rate.*`. These files are safe to feed into Codex/LLM tooling for post-session analysis; rotation triggers at `max-file-size-mb` with timestamped rollovers.
- Use `/acac selftest` to verify ProtocolLib/Paper hooks on a live server and `/acac debug` to print extra mitigation context while testing.

### Detection surface (stub-friendly)
- Packet pacing: short-window (1s/5s) packet rate tracking with mitigation flags and `/acac stats <player>` visibility, plus Netty crash/oversized payload protection.
- Movement sanity: invalid packet/teleport detection for NaN/INF coordinates and impossible jumps; real Paper mode cancels/rolls back movement on violations.
- Inventory & items: invalid item stack sizes/NBT, suspicious slot spam/dupes, and inventory interaction bursts configurable under `checks.invalid-item` and `checks.inventory-exploit`.
- World actions: impossible/rapid block placements, chunk-hop crash protection, oversized sign/payload packets, and redstone update spikes (`checks.invalid-placement`, `checks.chunk-crash`, `checks.sign-payload`, `checks.redstone-exploit`).
- Network anomalies: anti-cheat disabler/silence detection built on packet pacing (`checks.disabler`) and command spam abuse detection (`checks.command-abuse`).
- Entity/log spam: entity overload and console spam counters to spot abuse with mitigation hooks to clear/throttle activity.
- Player state: per-player trust score (starts at 100, recovers slowly) and per-check flag tallies exposed via `/acac stats <player>` and `/acac inspect <player>`.

### Commands for staff
- `/acac gui`: opens the staff control GUI (stubbed offline, inventory-driven in Paper) with player risk summaries, pagination, and a high-risk filter toggle. Requires `acac.use` (and `acac.gui.manage` for toggles/mitigation changes).
- `/acac help`: quick list of supported commands and usage hints.
- `/acac stats <player>`: trust, packets/sec, and per-check flag counts with LOW/MED/HIGH risk hints. Shows mitigation notes such as packet rate limiting or rubber-band corrections when active.
- `/acac inspect <player>`: verbose breakdown for console/moderators including mitigation state, per-check summaries, and recent mitigations (also opens the inspect GUI when run in-game).
- `/acac history <player>`: loads persisted flag/mitigation history from disk (flat-file in this environment, swappable for Mongo/SQL later).
- `/acac storage`: shows active backend, schema version, cache size, queued writes, and last error/migration timestamps for persistence (requires `acac.admin`).
- `/acac reload`: reloads `config.yml` and re-applies thresholds without clearing player state.
- `/acac perf`: shows average per-check handler timings to help tune sensitivity vs. cost (requires `acac.admin`).
- `/acac selftest`: simulates safe and bursty traffic for a synthetic player without affecting live users.
- `/acac debug`: toggles verbose debugging for staff (admin permission recommended).

### Testing checklist (Paper)
1. Join a Paper test server with ProtocolLib installed and `integrations.mode: paper/auto`.
2. Basic sanity: move/teleport/build/break blocks and verify no false flags; `/acac stats <you>` should show low risk.
3. Packet/teleport abuse: spam movement or trigger an invalid teleport and confirm mitigation notes plus optional rubber-band/kick.
4. Inventory/dupe spike: spam container clicks to trigger inventory exploit checks and ensure rollbacks occur.
5. Entity/redstone: place rapid entities or simple redstone clocks to see redstone/entity mitigations and alerts.
6. Review `/acac gui`, `/acac inspect <player>`, `/acac history <player>`, `/acac perf`, and `/acac selftest` for live data, alert status, and persistence backend reporting.

### First-time setup on Paper
1. Drop the built jar into your Paper 1.20 `plugins/` directory alongside ProtocolLib.
2. Leave mitigation actions in WARN/ROLLBACK to start (log-only) while you validate stats and inspect outputs.
3. Gradually raise actions toward THROTTLE/RUBBERBAND/KICK once you are confident mitigations match real abuse.
4. Use `/acac perf` to watch per-check timing overhead and `/acac selftest` to sanity-check hooks without risking real players.

### Building and packaging
 1. Run `./gradlew clean build` from the repository root (add `-PrealPaper` for the Paper profile).
 2. The compiled plugin jars are classifier-tagged at `build/libs/UltimateAntiCheat-0.1.0-stub.jar` (default) or `build/libs/UltimateAntiCheat-0.1.0-paper.jar` (real Paper/ProtocolLib profile).
 3. Drop the chosen jar into your server's `plugins/` directory and adjust `config.yml` as needed. Set `integrations.mode` to `paper` (or leave `auto`) on real servers so Bukkit/ProtocolLib listeners are wired automatically. Use `stub` only when compiling/running without Paper/ProtocolLib available.
4. After resolving any merge conflicts locally, rerun `./gradlew build --warning-mode all` to confirm the Java 17 toolchain is detected and the build remains free of Gradle deprecations.

### Merge/branch hygiene
- Conflicts are most likely in shared files such as `build.gradle`, `README.md`, and the check/mitigation classes. After fixing markers, ensure the Java toolchain still reports 17+ and the project builds cleanly with `./gradlew build --warning-mode all`.
- If you switch between stub and real Paper profiles, double-check that `-PrealPaper` is applied only when real dependencies are available to avoid accidental resolution drift during merges.
- In environments without a configured `origin` remote (like this offline CI), add the remote before attempting rebases or conflict resolution so `git fetch origin`/`git rebase origin/main` work as expected.
- If the upstream `main` reference is unavailable locally, create a tracking branch pointing at the intended base commit (for example, `git branch main <commit>`) before rebasing feature branches so conflict resolution can proceed without ambiguity.

## Upgrade to Gradle 9
- Done: removed deprecated Convention API usage and rely on the `java` extension with toolchains targeting Java 17 bytecode (via `--release 17`).
- Done: validated the build with `./gradlew build --warning-mode all` to ensure no deprecations remain on Gradle 8.x.
- Pending: upgrading the wrapper to Gradle 9.x must be performed by contributors on a normal network using `./gradlew wrapper --gradle-version 9.x`.
- Note: this Codex environment cannot download Gradle distributions (403), so the wrapper remains on the working Gradle 8.x version while code stays forward-compatible.
- Next up: enable configuration cache and rerun CI to confirm compatibility, then standardize on Gradle 9 for local and CI builds once the wrapper is bumped externally.

## Configuration quick reference
- `checks.*.action`: per-check mitigation mode (`log`, `soft`, `medium`, `hard`, or `auto`) to choose between warn-only vs. cancel/kick/ban actions.
- `checks.invalid-item`: toggle, severity, max-stack-size, and max-enchant-level for item/NBT validation.
- `checks.netty-crash-protection`: toggle, max-bytes, severity, and mitigation choice for oversized/raw payload guards.
- `checks.inventory-exploit`: window, max-actions, max-slot-index for dupe/slot spam detection.
- `checks.invalid-placement`: window, max-placements, build-height guardrails for impossible block placement; `checks.chunk-crash` limits chunk hops per window to resist chunk-load crash abuse.
- `checks.sign-payload`: max sign characters/payload bytes to prevent oversized packets.
- `checks.redstone-exploit` and `checks.redstone-mitigation`: updates-per-tick thresholds for lag machines plus optional mitigation toggle.
- `checks.command-abuse`: window + max commands per window to flag exploitable command spam crashers.
- `checks.disabler`: thresholds for packet silence after high activity.
- `checks.entity-overload`, `checks.packet-rate-limit`, `checks.console-spam`, `checks.invalid-packet`, `checks.invalid-teleport` remain as before with trust/mitigation hooks.
- `mitigation.*`: risk thresholds for warn/kick/ban suggestions and cooldown to avoid alert spam; GUI toggle to force log-only mode.
- `alerts.*`: enable/disable staff broadcasts, throttle window, staff permission, and channel routing (console, in-game staff, optional Discord webhook). `alerts.discord-webhook` plus `alerts.channels.discord=true` enables webhook delivery. Structured JSONL logging can be enabled under `alerts.logging.*` with size/retention controls.
- `alerts.notify-permission`: reserved for future expanded notification routing (not used in 0.1.0). Webhooks support optional retries/backoff and severity filters via `alerts.discord-*` keys.
- `experiments.*`: opt-in experiment logging to `logs/acac-experiments.jsonl` with sampling controls, optional player/location fields, and size-based rotation. Disable by setting `experiments.enabled: false` when not running observation sessions.
- `performance.*`: per-check and total budget (ms) for `/acac perf` highlighting.
- `persistence.log-*`: simple on-disk log rotation caps size and number of rotated files (legacy plain logs); `alerts.logging.*` governs structured alert JSON and rotation/retention.
- `config-version`: tracked in `config.yml` for migrations. Older versions are backed up (timestamped) and merged with new defaults on startup or `/acac reload`. Use `/acac config` to view version/validation status.
- `integrations.mode`: choose `stub` for offline builds (default here) or switch to `paper`/`auto` to bind real Paper/ProtocolLib bridges. Real mode activates the ProtocolLib packet listener (movement/teleport) and Paper mitigation hooks (cancel/rubber-band/kick).
- `persistence.*`: configure history retention, schema version, caching, and whether to flush snapshots on every flag. `persistence.cache.*` controls max cached entries and async flush cadence; `storage.mongo-*` and `storage.sql.*` tune timeouts/retries/pool size.
- `storage.use-database`/Mongo settings or `storage.use-sql-database`/SQL settings: when enabled, UAC attempts to open the configured backend; failures are logged and flat-file storage is used instead so the plugin remains online. History pruning applies in all backends.

PacketEvents is explicitly unsupported today; keep `ProtocolLib` installed for packet interception. ViaVersion, LuckPerms, and DiscordSRV are optional soft hooks that are used when detected but never required to run the plugin.

## Running on a real Paper/ProtocolLib server
- This repository ships with offline stub implementations for Bukkit/ProtocolLib so it compiles without network access.
- In a normal environment, set `integrations.mode: paper`/`auto` and ensure Paper + ProtocolLib are on the classpath to use the real packet bridge, inventory accessors, and mitigation hooks.
- Uncomment the Paper/ProtocolLib `compileOnly` lines in `build.gradle`, build, and drop the jar into your Paper `plugins/` folder alongside ProtocolLib.
- The Paper bridge powers cancel/rollback/rubber-band/kick/ban actions; stub mode stays log-only. Discord/webhook alerts, GUI toggles, and mitigation ladders are active in both modes but only enforce actions when Paper APIs are available.

### Known limitations / future work
- PacketEvents remains unsupported; ProtocolLib is required for packet interception.
- Webhook dispatch now supports retry/backoff and embeds but still targets generic Discord webhooks; add per-platform payloads as needed.
- External ban plugin hooks are optional; built-in temp/perma ban fallbacks are used otherwise.
- Stub Bukkit/ProtocolLib classes ship for offline compilation; real Paper builds should use `-PrealPaper` so the genuine APIs are on the classpath.

### Local Paper test plan (real mode)
1. Uncomment the Paper + ProtocolLib `compileOnly` entries in `build.gradle` and run `./gradlew clean build` on a networked machine.
2. Place the built jar in a Paper 1.20.x server with ProtocolLib installed; set `integrations.mode: auto` or `paper` in `config.yml`.
3. Join the server and spam movement to trigger packet rate limiting; verify `/acac stats <player>` shows a mitigation note and the player is rubber-banded/warned.
4. Force an invalid teleport (e.g., via command or client modification) and confirm the mitigation log, cancellation, and rubber-band messaging.
5. Check `/acac inspect <player>` and `/acac history <player>` to confirm flags and mitigations are persisted and visible in GUI/chat.

## Documentation
- [`docs/INSTALLATION.md`](docs/INSTALLATION.md): production setup for Paper, prerequisites, first-week hardening.
- [`docs/UPGRADE_AND_MIGRATION.md`](docs/UPGRADE_AND_MIGRATION.md): config-version behavior, backups, and safe rollout steps between 0.1.x releases.
- [`docs/TROUBLESHOOTING.md`](docs/TROUBLESHOOTING.md): common startup/performance/GUI issues and tuning guidance.
- [`docs/RELEASE_NOTES_0.1.x.md`](docs/RELEASE_NOTES_0.1.x.md): highlights, known limitations, and upgrade reminders for the 0.1.x line.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md): architecture, roadmap, module specifications, performance strategy, and testing plan.

## CI, lint, and releases
- GitHub Actions runs stub and realPaper matrices with `./gradlew clean lint build` (selfTest executes in stub mode only) and uploads classifier-tagged artifacts from each profile.
- Dependency Review and CodeQL workflows run on pull requests to surface supply-chain and static analysis findings.
- Tagged pushes (`v*`) trigger `release.yml`, building both profiles and attaching the jars to the GitHub release automatically.
