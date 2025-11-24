# UltimateAntiCheat (UAC)

A modular Paper/Spigot anti-exploit framework focused on hardened packet handling, crash mitigation, and staff-friendly tooling.

- Comprehensive coverage: packet/Netty crashers, server crashers, smart packet limiting, invalid items/NBT, anti-cheat disablers, entity overload, redstone exploits, invalid actions/placements/signs, duping, console spam, and more.
- Ecosystem-first: hooks for ProtocolLib/PacketEvents, ViaVersion awareness, permission integration via LuckPerms, Discord/webhook alerts, and optional dashboard API.
- Staff experience: `/uac gui|stats|config` with inventory GUIs, chat summaries, and configurable alert sinks.

## Getting Started
- Requires Java 17+ and Paper/Spigot 1.21.x (API) with ProtocolLib recommended.
- Build with Gradle: `./gradlew build` (uses the locally available Gradle install to support offline builds).
- Copy the assembled jar to your `plugins/` folder and configure `config.yml`.
- Gradle 9 preparation: the build currently passes on Gradle 8.x with no deprecations; set aside time to validate the upgrade
  path to Gradle 9.0 soon.

### Building and packaging
1. Run `./gradlew clean build` from the repository root.
2. The compiled plugin jar will be written to `build/libs/UltimateAntiCheat-0.1.0.jar`.
3. Drop the jar into your server's `plugins/` directory and adjust `config.yml` as needed.

## Upgrade to Gradle 9
- Done: removed deprecated Convention API usage and rely on the `java` extension with toolchains targeting Java 17 bytecode (via `--release 17`).
- Done: validated the build with `./gradlew build --warning-mode all` to ensure no deprecations remain on Gradle 8.x.
- In progress: run the Gradle 9 wrapper (`./gradlew wrapper --gradle-version=9.0.0`) and ensure the build stays warning-free.
- Pending: restricted networks currently block downloading the Gradle 9 distribution; provide the zip internally or pre-cache it to allow wrapper-based builds.
- Next up: enable configuration cache and rerun CI to confirm compatibility, then standardize on Gradle 9 for local and CI builds.

## Documentation
See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full architecture, roadmap, module specifications, performance strategy, and testing plan.
