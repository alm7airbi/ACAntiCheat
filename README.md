# UltimateAntiCheat (UAC)

A modular Paper/Spigot anti-exploit framework focused on hardened packet handling, crash mitigation, and staff-friendly tooling.

- Comprehensive coverage: packet/Netty crashers, server crashers, smart packet limiting, invalid items/NBT, anti-cheat disablers, entity overload, redstone exploits, invalid actions/placements/signs, duping, console spam, and more.
- Ecosystem-first: hooks for ProtocolLib/PacketEvents, ViaVersion awareness, permission integration via LuckPerms, Discord/webhook alerts, and optional dashboard API.
- Staff experience: `/acac stats <player>` with inventory GUIs, chat summaries, and configurable alert sinks.

## Getting Started
- Requires Java 17+ and Paper/Spigot 1.20+ (API) with ProtocolLib recommended.
- Build with Gradle: `./gradlew build` (uses the locally available Gradle install to support offline builds on Gradle 8.x in this environment). The Gradle toolchain prefers the available JDK (21 in this environment) while emitting Java 17 bytecode via `--release 17` for Gradle 9 compatibility.
- Copy the assembled jar to your `plugins/` folder and configure `config.yml`.
- Gradle 9 preparation: the build currently passes on Gradle 8.x with no deprecations; set aside time to validate the upgrade
  path to Gradle 9.0 soon.

### Detection surface (stub-friendly)
- Packet pacing: short-window (1s/5s) packet rate tracking with mitigation flags and `/acac stats <player>` visibility.
- Movement sanity: invalid packet/teleport detection for NaN/INF coordinates and impossible jumps; TODO hooks to cancel/rollback in a live Paper environment.
- Inventory & items: invalid item stack sizes/NBT, suspicious slot spam/dupes, and inventory interaction bursts configurable under `checks.invalid-item` and `checks.inventory-exploit`.
- World actions: impossible/rapid block placements, oversized sign/payload packets, and redstone update spikes (`checks.invalid-placement`, `checks.sign-payload`, `checks.redstone-exploit`).
- Network anomalies: anti-cheat disabler/silence detection built on packet pacing (`checks.disabler`).
- Entity/log spam: entity overload and console spam counters to spot abuse; TODO throttling when wired to real server events.
- Player state: per-player trust score (starts at 100, recovers slowly) and per-check flag tallies exposed via `/acac stats <player>` and `/acac inspect <player>`.

### Commands for staff
- `/acac stats <player>`: trust, packets/sec, and per-check flag counts with LOW/MED/HIGH risk hints.
- `/acac inspect <player>`: verbose breakdown for console/moderators including mitigation state and last flag reasons.

### Building and packaging
1. Run `./gradlew clean build` from the repository root (pass `-PusePublishedDependencies=true` if you want to resolve the real Paper/ProtocolLib/MongoDB artifacts instead of the bundled stubs used for offline CI).
2. The compiled plugin jar will be written to `build/libs/UltimateAntiCheat-0.1.0.jar`.
3. Drop the jar into your server's `plugins/` directory and adjust `config.yml` as needed.

## Upgrade to Gradle 9
- Done: removed deprecated Convention API usage and rely on the `java` extension with toolchains targeting Java 17 bytecode (via `--release 17`).
- Done: validated the build with `./gradlew build --warning-mode all` to ensure no deprecations remain on Gradle 8.x.
- Pending: upgrading the wrapper to Gradle 9.x must be performed by contributors on a normal network using `./gradlew wrapper --gradle-version 9.x`.
- Note: this Codex environment cannot download Gradle distributions (403), so the wrapper remains on the working Gradle 8.x version while code stays forward-compatible.
- Next up: enable configuration cache and rerun CI to confirm compatibility, then standardize on Gradle 9 for local and CI builds once the wrapper is bumped externally.

## Configuration quick reference
- `checks.invalid-item`: toggle, severity, and max-stack-size for item/NBT validation.
- `checks.inventory-exploit`: window, max-actions, max-slot-index for dupe/slot spam detection.
- `checks.invalid-placement`: window, max-placements, build-height guardrails for impossible block placement.
- `checks.sign-payload`: max sign characters/payload bytes to prevent oversized packets.
- `checks.redstone-exploit`: updates-per-tick thresholds for lag machines.
- `checks.disabler`: thresholds for packet silence after high activity.
- `checks.entity-overload`, `checks.packet-rate-limit`, `checks.console-spam`, `checks.invalid-packet`, `checks.invalid-teleport` remain as before with trust/mitigation hooks.

## Documentation
See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full architecture, roadmap, module specifications, performance strategy, and testing plan.
