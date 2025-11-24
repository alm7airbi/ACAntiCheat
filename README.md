# UltimateAntiCheat (UAC)

A modular Paper/Spigot anti-exploit framework focused on hardened packet handling, crash mitigation, and staff-friendly tooling.

- Comprehensive coverage: packet/Netty crashers, server crashers, smart packet limiting, invalid items/NBT, anti-cheat disablers, entity overload, redstone exploits, invalid actions/placements/signs, duping, console spam, and more.
- Ecosystem-first: hooks for ProtocolLib/PacketEvents, ViaVersion awareness, permission integration via LuckPerms, Discord/webhook alerts, and optional dashboard API.
- Staff experience: `/uac gui|stats|config` with inventory GUIs, chat summaries, and configurable alert sinks.

## Getting Started
- Requires Java 17+ and Paper/Spigot 1.21.x (API) with ProtocolLib recommended.
- Build with Gradle: `./gradlew build` (wrapper not included yet).
- Copy the assembled jar to your `plugins/` folder and configure `config.yml`.

## Documentation
See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full architecture, roadmap, module specifications, performance strategy, and testing plan.
