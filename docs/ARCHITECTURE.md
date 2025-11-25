# UltimateAntiCheat Architecture & Delivery Plan

## 1) High-Level Architecture
- **Core Platform**: Paper/Spigot plugin targeting Java 17 with ProtocolLib bridges for packet access (PacketEvents intentionally unsupported in 0.1.0) and soft hooks for ViaVersion, LuckPerms, and DiscordSRV when present. Modular check registry built around `AbstractCheck` with per-vector subpackages.
- **Check Groups**: Packet-layer (Netty crashers, invalid/incorrect packets, packet flood limiter), Entity/World (entity overload, invalid placements, redstone mitigation, chunk/position crashers), Inventory/Items (invalid NBT, duping, container exploits), Behavior (invalid actions, teleport anomalies, anti-cheat disablers, console spam).
- **Pipelines**: Packet → `PacketInterceptor` → registered checks; Bukkit events → dedicated listeners → checks; heavy computation offloaded via `AsyncExecutor`.
- **Data & State**: `DatabaseManager` + `PlayerDataStore` abstraction for Mongo/postgres; in-memory caches for trust scores (`TrustScoreManager`), burst buffers (`BufferingManager`).
- **Configuration**: Versioned YAML (`config.yml`) mapped to `Settings`; hot-reload hooks; per-check toggles and thresholds.
- **UI**: `/acac` command routes to inventory GUI (`GuiManager`) and chat tools; alert sinks to console/Discord/webhooks; future dashboard hooks can be added externally if needed (not included in 0.1.x).

## 2) Phase Breakdown
- **Phase 1: Core packet interception + limiter**
  - Deliverables: ProtocolLib bridge, `PacketRateLimiterCheck`, basic buffering & trust score scaffolding.
  - Risks: packet overhead, compatibility with legacy client versions; mitigate via async parsing and minimal allocations.
- **Phase 2: Entity/item validation**
  - Deliverables: invalid item NBT detection, container rollback, entity spawn throttles, placement sanity checks.
  - Risks: false positives on modded clients; mitigate with thresholds and version-aware rules.
- **Phase 3: Exploit crashers**
  - Deliverables: invalid position/chunk crash guards, Netty decoder hardening, anti-console spam, redstone limiter, duping detection.
  - Risks: performance regressions on large redstone bases; mitigate with per-chunk budgets and exemptions.
- **Phase 4: UI and integrations**
  - Deliverables: staff GUIs, `/acac` subcommands, Discord/webhook relays.
  - Risks: permission complexity; mitigate with LuckPerms nodes and granular config.
- **Phase 5: Testing/hardening/obfuscation**
  - Deliverables (backlog): expanded regression suite, packet fuzz harness, and optional obfuscation/testing artifacts beyond the lightweight self-tests shipped in 0.1.x.
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
- **ProtocolLib** for packet streams (movement, interactions, payloads); PacketEvents intentionally unsupported. Fallback = limited Bukkit event checks when stub/offline.
- **ViaVersion** for multi-version packet schema awareness.
- **LuckPerms** for permission nodes and exempt roles.
- **DiscordSRV / webhooks** for alerts.
- **Other AC dashboards**: can be integrated by external plugins; no built-in dashboard API ships in 0.1.x.
- **Fallback Strategy**: degrade gracefully to Bukkit listeners when packet libraries missing; disable unsupported checks with warnings.

## 5) UI / Staff Dashboard
- Commands: `/acac gui`, `/acac stats`, `/acac perf`, `/acac reload`, `/acac help`, `/acac history`, `/acac inspect`, `/acac selftest`, `/acac storage` with permission gating (`acac.use` baseline, `acac.gui.manage` for toggles/mitigation, `acac.admin` for admin actions).
- GUI: inventory menus for toggling checks and viewing violators; chat confirmations for destructive actions; pagination and high-risk filtering in the control GUI plus inspect GUI actions (rubber-band, trust reset) with throttling.
- Alerts: action bar/toast for online staff; Discord/webhook for remote with async retry/backoff; pluggable sink interface and structured JSONL logs with rotation/retention for SIEM export.
- Config editing: in-GUI toggles + YAML; hot-reload with audit trail; per-check verbosity.
- Logs: structured JSON + rotating files; database optional.

## 6) Performance / Scalability
- Packet parsing kept lightweight; heavy NBT/entity audits offloaded to `AsyncExecutor`.
- Cache player/world state; avoid reflection per packet; batch DB writes.
- Adaptive sampling when online count is high; per-chunk budgets for redstone/entity.
- High-ping handling: widen thresholds when TPS/ping degrade; trust-decay rather than instant punish.

## 7) Testing & Hardening
- Shipped tests: lightweight stub self-test harness and targeted unit/integration-style tests for checks, mitigation, config, storage, and a plugin smoke flow.
- Backlog: deeper packet fuzzing, replay-based soak tests under lag, and optional obfuscation/tamper checks.

## 8) Configuration & Update Strategy
- Versioned `config.yml` with migration code; defaults auto-rewritten when schema bumps.
- Versioned `config.yml` with migration code; defaults auto-rewritten when schema bumps, and the prior file is backed up (timestamped) before writing a merged version with new keys. Validation errors are surfaced via `/acac config`.
- Dynamic updates via GUI/commands writing to disk; reload without restart where safe.
- Backward compatibility: guard features by API-version checks; ViaVersion-aware packet schemas.
- Release cadence: semantic versions, changelog, and safe defaults for new checks (log-only first).

## 9) Deliverables & Timeline (backlog example)
- The original multi-week delivery sketch included dashboards, ban-wave tooling, and advanced fuzzing. For 0.1.x, focus is on the implemented packet checks, mitigations, persistence, GUIs, and alerting; backlog items (dashboards, deeper fuzzing/obfuscation) remain future work.

## 11) Release documentation set
- Installation and setup: see `docs/INSTALLATION.md` for prerequisites, build commands (stub vs realPaper), and first-week hardening steps.
- Upgrade and migration: see `docs/UPGRADE_AND_MIGRATION.md` for config-version handling, backups, and validation flow on `/acac reload` or startup.
- Troubleshooting/FAQ: see `docs/TROUBLESHOOTING.md` for startup/performance/GUI issues and tuning guidance.
- Release notes: see `docs/RELEASE_NOTES_0.1.x.md` for highlights and known limitations of the 0.1.x line.

## 10) Runtime scaffolding
- Core modules (`UltimateAntiCheatPlugin`, `PacketInterceptor`, `PacketRateLimiterCheck`, `InvalidItemCheck`, integration hooks) now ship with working logic for both stub and Paper/ProtocolLib modes. Stub classes remain only to keep offline/CI builds compiling; Paper builds swap to the real APIs via `-PrealPaper`, and the CI matrix validates both profiles.
- Build tooling: Gradle Java 17 toolchain, optional compileOnly Paper/ProtocolLib for real servers, and Mongo/SQL/flat-file persistence depending on `storage.use-database` or `storage.use-sql-database`, with schema-versioned snapshots, caching, and async flush plus automatic fallback to flat-file on database errors.
