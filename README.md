# UltimateAntiCheat (UAC)

A modular Paper anti-exploit framework focused on hardened packet handling, crash mitigation, and staff-friendly tooling.

## Requirements
- Java 17
- Paper 1.20.x (API compatibility)
- ProtocolLib installed on the server (runtime only; not bundled)
- Optional: MongoDB if you enable the database storage backend

## Building
1. If `gradle/wrapper/gradle-wrapper.jar` is missing (binary files are not committed here), generate it locally with `./gradlew wrapper` using an existing Gradle installation.
2. Run `./gradlew clean build`.
3. The plugin jar is produced at `build/libs/ACAntiCheat.jar`.

## Runtime dependencies
- ProtocolLib must be installed on the Paper server. It is declared as a `compileOnly` dependency in `build.gradle` and should **not** be committed as `ProtocolLib.jar` in this repository.
- The MongoDB Java driver is packaged with the plugin to support the optional Mongo storage backend.

## Installing on Paper
1. Copy `build/libs/ACAntiCheat.jar` into your server's `plugins/` directory.
2. Ensure ProtocolLib is also installed as a plugin.
3. Restart the server and adjust `config.yml` to match your environment.

### Detection surface
- Packet pacing: short-window (1s/5s) packet rate tracking with mitigation flags and `/acac stats <player>` visibility, plus Netty crash/oversized payload protection.
- Movement sanity: invalid packet/teleport detection for NaN/INF coordinates and impossible jumps; mitigations include cancellation, rollback, and rubber-banding.
- Inventory & items: invalid item stack sizes/NBT, suspicious slot spam/dupes, and inventory interaction bursts configurable under `checks.invalid-item` and `checks.inventory-exploit`.
- World actions: impossible/rapid block placements, chunk-hop crash protection, oversized sign/payload packets, and redstone update spikes (`checks.invalid-placement`, `checks.chunk-crash`, `checks.sign-payload`, `checks.redstone-exploit`).
- Network anomalies: anti-cheat disabler/silence detection built on packet pacing (`checks.disabler`) and command spam abuse detection (`checks.command-abuse`).
- Entity/log spam: entity overload and console spam counters to spot abuse with mitigation hooks to clear/throttle activity.
- Player state: per-player trust score (starts at 100, recovers slowly) and per-check flag tallies exposed via `/acac stats <player>` and `/acac inspect <player>`.

### Commands for staff
- `/acac gui`: opens the staff control GUI with player risk summaries, pagination, and a high-risk filter toggle. Requires `acac.use` (and `acac.gui.manage` for toggles/mitigation changes).
- `/acac help`: quick list of supported commands and usage hints.
- `/acac stats <player>`: trust, packets/sec, and per-check flag counts with LOW/MED/HIGH risk hints. Shows mitigation notes such as packet rate limiting or rubber-band corrections when active.
- `/acac inspect <player>`: verbose breakdown for console/moderators including mitigation state, per-check summaries, and recent mitigations (also opens the inspect GUI when run in-game).
- `/acac history <player>`: loads persisted flag/mitigation history from disk (flat-file by default, or Mongo when enabled).
- `/acac storage`: shows active backend, schema version, cache size, queued writes, and last error/migration timestamps for persistence (requires `acac.admin`).
- `/acac reload`: reloads `config.yml` and re-applies thresholds without clearing player state.
- `/acac perf`: shows average per-check handler timings to help tune sensitivity vs. cost (requires `acac.admin`).
- `/acac selftest`: simulates safe and bursty traffic for a synthetic player without affecting live users.
- `/acac debug`: toggles verbose debugging for staff (admin permission recommended).

### Testing checklist (Paper)
1. Join a Paper test server with ProtocolLib installed.
2. Move/teleport/build/break blocks and verify no false flags; `/acac stats <you>` should show low risk.
3. Spam movement or trigger an invalid teleport and confirm mitigation notes plus optional rubber-band/kick.
4. Spam container clicks to trigger inventory exploit checks and ensure rollbacks occur.
5. Place rapid entities or simple redstone clocks to see redstone/entity mitigations and alerts.
6. Review `/acac gui`, `/acac inspect <player>`, `/acac history <player>`, `/acac perf`, and `/acac selftest` for live data and persistence/alert status.

## Documentation
- [`docs/INSTALLATION.md`](docs/INSTALLATION.md): production setup for Paper, prerequisites, first-week hardening.
- [`docs/UPGRADE_AND_MIGRATION.md`](docs/UPGRADE_AND_MIGRATION.md): config-version behavior, backups, and safe rollout steps between 0.1.x releases.
- [`docs/TROUBLESHOOTING.md`](docs/TROUBLESHOOTING.md): common startup/performance/GUI issues and tuning guidance.
- [`docs/RELEASE_NOTES_0.1.x.md`](docs/RELEASE_NOTES_0.1.x.md): highlights, known limitations, and upgrade reminders for the 0.1.x line.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md): architecture, roadmap, module specifications, performance strategy, and testing plan.

## CI & builds
- GitHub Actions runs two jobs using the Gradle wrapper on Java 17:
  - `stub`: `./gradlew clean build --warning-mode all --no-daemon`
  - `realPaper`: `./gradlew clean build -PrealPaper --warning-mode all --no-daemon`
- Both jobs upload the produced jar from `build/libs/` as artifacts for download.
- Additional security scans (CodeQL, dependency review) are temporarily disabled to keep CI green; re-enable them when ready.
- Tagged pushes (`v*`) still trigger `release.yml` to build and attach the jar to the GitHub release automatically.
