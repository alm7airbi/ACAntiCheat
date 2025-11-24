# UltimateAntiCheat Architecture & Delivery Plan

## 1) High-Level Architecture
- **Core Platform**: Paper/Spigot plugin targeting Java 17 with optional ProtocolLib/PacketEvents bridges for packet access. Modular check registry built around `AbstractCheck` with per-vector subpackages.
- **Check Groups**: Packet-layer (Netty crashers, invalid/incorrect packets, packet flood limiter), Entity/World (entity overload, invalid placements, redstone mitigation, chunk/position crashers), Inventory/Items (invalid NBT, duping, container exploits), Behavior (invalid actions, teleport anomalies, anti-cheat disablers, console spam).
- **Pipelines**: Packet → `PacketInterceptor` → registered checks; Bukkit events → dedicated listeners → checks; heavy computation offloaded via `AsyncExecutor`.
- **Data & State**: `DatabaseManager` + `PlayerDataStore` abstraction for Mongo/postgres; in-memory caches for trust scores (`TrustScoreManager`), burst buffers (`BufferingManager`).
- **Configuration**: Versioned YAML (`config.yml`) mapped to `Settings`; hot-reload hooks; per-check toggles and thresholds.
- **UI**: `/uac` command routes to inventory GUI (`GuiManager`) and chat tools; alert sinks to console/Discord/webhooks; optional dashboards via external hooks.

## 2) Phase Breakdown
- **Phase 1: Core packet interception + limiter**
  - Deliverables: ProtocolLib/PacketEvents bridge, `PacketRateLimiterCheck`, basic buffering & trust score scaffolding.
  - Risks: packet overhead, compatibility with legacy client versions; mitigate via async parsing and minimal allocations.
- **Phase 2: Entity/item validation**
  - Deliverables: invalid item NBT detection, container rollback, entity spawn throttles, placement sanity checks.
  - Risks: false positives on modded clients; mitigate with thresholds and version-aware rules.
- **Phase 3: Exploit crashers**
  - Deliverables: invalid position/chunk crash guards, Netty decoder hardening, anti-console spam, redstone limiter, duping detection.
  - Risks: performance regressions on large redstone bases; mitigate with per-chunk budgets and exemptions.
- **Phase 4: UI and integrations**
  - Deliverables: staff GUIs, `/uac` subcommands, Discord/webhook relays, hooks for other AC dashboards (e.g., Plan, LiteBans alerts).
  - Risks: permission complexity; mitigate with LuckPerms nodes and granular config.
- **Phase 5: Testing/hardening/obfuscation**
  - Deliverables: exploit client regression suite, packet fuzz harness, perf dashboards, ban-wave scheduler, code obfuscation step.
  - Risks: flakiness under lag/high ping; mitigate with adaptive thresholds and trust decay.

## 3) Module Specifications (Detection + Mitigation)
- **Netty Crashers**: Detect malformed lengths/opcodes; cap decoder frame sizes; mitigation = drop packet, increment trust score, optional kick/ban.
- **Server Crashers (position/chunk)**: Validate bounds vs world border; reject >N teleports per tick; mitigation = clamp positions, cancel events.
- **Packet Limiter**: Sliding-window per-player counter; mitigation = queue/throttle/kick; false positive control via buffers and ping-weighted thresholds.
- **Invalid Items / Duping**: Check NBT size, enchant caps, stack sizes; monitor container transactions; mitigation = rollback, delete item, audit log.
- **Anti-cheat Disablers**: Watch for known disabler packet sequences and timer abuse; mitigation = temporary spectate mode + alerts.
- **Entity Overload**: Per-chunk entity budgets; spawn rate caps per UUID; mitigation = cancel spawn, temporary blacklist of spawn eggs.
- **Redstone Exploits**: Circuit tick budget per chunk; disable fast clocks; mitigation = power-cut blocks, throttle chunk tickets.
- **Invalid Actions**: Movement sanity (NaN/Inf, negative health), invalid use-interact order; mitigation = cancel + resync.
- **Console Spammers**: Rate-limit plugin/chat console outputs per source; mitigation = mute source, sample logs.
- **Invalid/Incorrect Packets**: Schema validation by version; mitigation = drop + alert.
- **Invalid Placements/Signs**: Check reachability, block support; sanitize sign text length/color codes; mitigation = cancel placement/write sanitized text.
- **False Positive Management**: Trust scores (`TrustScoreManager`), burst buffers (`BufferingManager`), stage escalation (log → alert → isolate → ban wave), exemptions for whitelisted roles.

## 4) Third-Party Hooks
- **ProtocolLib / PacketEvents** for packet streams; fallback = limited Bukkit event checks.
- **ViaVersion** for multi-version packet schema awareness.
- **LuckPerms** for permission nodes and exempt roles.
- **DiscordSRV / webhooks** for alerts.
- **Other AC dashboards**: expose simple API (events + status) for external dashboards.
- **Fallback Strategy**: degrade gracefully to Bukkit listeners when packet libraries missing; disable unsupported checks with warnings.

## 5) UI / Staff Dashboard
- Commands: `/uac gui`, `/uac stats`, `/uac config`, `/uac reload`, `/uac alerts`.
- GUI: inventory menus for toggling checks, viewing violators, running ban waves; chat confirmations for destructive actions.
- Alerts: action bar/toast for online staff; Discord/webhook for remote; pluggable sink interface.
- Config editing: in-GUI toggles + YAML; hot-reload with audit trail; per-check verbosity.
- Logs: structured JSON + rotating files; database optional.

## 6) Performance / Scalability
- Packet parsing kept lightweight; heavy NBT/entity audits offloaded to `AsyncExecutor`.
- Cache player/world state; avoid reflection per packet; batch DB writes.
- Adaptive sampling when online count is high; per-chunk budgets for redstone/entity.
- High-ping handling: widen thresholds when TPS/ping degrade; trust-decay rather than instant punish.

## 7) Testing & Hardening
- Harness with popular hacked clients + packet fuzzers; simulate invalid position/chunk, oversize signs, entity spam.
- Boundary tests for packet limiter under lag; soak tests with replayed captures.
- Performance profiling with Spark/TickProfiler; alert when check cost > budget.
- Ban strategy: flag → isolation (spectator) → ban wave; manual review queue via GUI.
- Optional obfuscation (ProGuard/Zelix) and checksum self-test for tamper detection.

## 8) Configuration & Update Strategy
- Versioned `config.yml` with migration code; defaults auto-rewritten when schema bumps.
- Dynamic updates via GUI/commands writing to disk; reload without restart where safe.
- Backward compatibility: guard features by API-version checks; ViaVersion-aware packet schemas.
- Release cadence: semantic versions, changelog, and safe defaults for new checks (log-only first).

## 9) Deliverables & Timeline (example 12-week)
- Weeks 1–2: project skeleton, packet bridges, basic limiter.
- Weeks 3–5: item/entity validation, duping protections, placement/sign filters.
- Weeks 6–8: crashers (netty/position/chunk/redstone), console spam guard, anti-disabler heuristics.
- Weeks 9–10: staff GUIs, alert sinks, webhook & dashboard hooks.
- Week 11: regression + fuzz testing, perf tuning, trust-score calibration.
- Week 12: beta release, documentation, obfuscation pass.

## 10) Initial Code Stubs
- Provided in `src/main/java` with placeholder logic for key modules (`UltimateAntiCheatPlugin`, `PacketInterceptor`, `PacketRateLimiterCheck`, `InvalidItemCheck`, integration hooks, util scaffolding) and resources (`plugin.yml`, `config.yml`).
- Build tooling: Gradle 17 toolchain, ProtocolLib + PacketEvents optional deps, Mongo driver for persistence experiments.
