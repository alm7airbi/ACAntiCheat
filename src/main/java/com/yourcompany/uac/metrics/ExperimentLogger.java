package com.yourcompany.uac.metrics;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.PlayerCheckState;
import com.yourcompany.uac.checks.context.EnvironmentSnapshot;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Writes structured JSONL experiment logs for detections/mitigations/selftests.
 * Disabled by default unless experiments.enabled is true.
 */
public class ExperimentLogger {

    private final UltimateAntiCheatPlugin plugin;
    private final boolean enabled;
    private final Path logPath;
    private final double sampleDetection;
    private final double sampleMitigation;
    private final boolean includePlayerName;
    private final boolean includeLocation;
    private final long maxBytes;
    private final String serverProfile;
    private final String pluginVersion;
    private final ExecutorService executor;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final AtomicLong lastErrorAt = new AtomicLong(0);
    private final AtomicReference<String> lastErrorMessage = new AtomicReference<>("");

    public ExperimentLogger(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
        var settings = plugin.getConfigManager().getSettings();
        this.enabled = settings.experimentsEnabled;
        this.sampleDetection = settings.experimentsSampleRateDetection;
        this.sampleMitigation = settings.experimentsSampleRateMitigation;
        this.includePlayerName = settings.experimentsIncludePlayerName;
        this.includeLocation = settings.experimentsIncludeLocation;
        this.maxBytes = Math.max(1, settings.experimentsMaxFileSizeMb) * 1_048_576L;
        this.serverProfile = plugin.getIntegrationService() != null && plugin.getIntegrationService().isUsingStub()
                ? "stub" : "realPaper";
        this.pluginVersion = resolvePluginVersion(plugin);
        Path configured = Path.of(settings.experimentsLogFile);
        this.logPath = configured.isAbsolute() ? configured : plugin.getDataFolder().toPath().resolve(configured);
        this.executor = enabled ? Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "acac-experiments");
                t.setDaemon(true);
                return t;
            }
        }) : null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Path getLogPath() {
        return logPath;
    }

    public long getLastErrorAt() {
        return lastErrorAt.get();
    }

    public String getLastErrorMessage() {
        return lastErrorMessage.get();
    }

    public void shutdown() {
        if (!enabled || executor == null) {
            return;
        }
        shuttingDown.set(true);
        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    public void logDetection(Player player,
                              EnvironmentSnapshot env,
                              String checkId,
                              String category,
                              int severity,
                              String reason,
                              double trustBefore,
                              double trustAfter,
                              double riskScore,
                              int violations,
                              boolean flagged,
                              boolean exempted,
                              PlayerCheckState.MitigationLevel mitigationLevel) {
        if (!enabled || !sample(sampleDetection)) {
            return;
        }
        Map<String, Object> json = base("DETECTION");
        appendPlayer(json, player, env);
        appendLocation(json, player);
        Map<String, Object> check = new HashMap<>();
        check.put("id", checkId);
        check.put("category", category);
        check.put("severity", severity);
        json.put("check", check);
        Map<String, Object> state = new HashMap<>();
        state.put("trustBefore", trustBefore);
        state.put("trustAfter", trustAfter);
        state.put("riskScore", riskScore);
        state.put("violationsInWindow", violations);
        json.put("state", state);
        Map<String, Object> decision = new HashMap<>();
        decision.put("flagged", flagged);
        decision.put("reason", reason);
        decision.put("exempted", exempted);
        decision.put("mitigation", mitigationLevel == null ? null : mitigationLevel.name());
        json.put("decision", decision);
        enqueue(json);
    }

    public void logMitigation(Player player,
                              EnvironmentSnapshot env,
                              PlayerCheckState.MitigationLevel before,
                              PlayerCheckState.MitigationLevel after,
                              boolean cooldownSuppressed,
                              List<String> actions,
                              String sourceCheck,
                              int recentFlags,
                              double trust,
                              double riskScore) {
        if (!enabled || !sample(sampleMitigation)) {
            return;
        }
        Map<String, Object> json = base("MITIGATION");
        appendPlayer(json, player, env);
        appendLocation(json, player);
        Map<String, Object> mit = new HashMap<>();
        mit.put("stageBefore", before == null ? null : before.name());
        mit.put("stageAfter", after == null ? null : after.name());
        mit.put("cooldownSuppressed", cooldownSuppressed);
        mit.put("actions", actions);
        json.put("mitigation", mit);
        Map<String, Object> source = new HashMap<>();
        source.put("checkId", sourceCheck);
        source.put("recentFlags", recentFlags);
        source.put("trust", trust);
        source.put("riskScore", riskScore);
        json.put("source", source);
        enqueue(json);
    }

    public void logSelfTest(String scenario, String result, String notes) {
        if (!enabled) {
            return;
        }
        Map<String, Object> json = base("SELFTEST");
        Map<String, Object> self = new HashMap<>();
        self.put("scenario", scenario);
        self.put("result", result);
        self.put("notes", notes);
        json.put("selftest", self);
        enqueue(json);
    }

    private void enqueue(Map<String, Object> json) {
        if (!enabled || executor == null || shuttingDown.get()) {
            return;
        }
        executor.execute(() -> {
            try {
                Path dir = logPath.getParent();
                if (dir == null) {
                    dir = plugin.getDataFolder().toPath();
                }
                Files.createDirectories(dir.toAbsolutePath());
                rotateIfNeeded();
                Files.writeString(logPath, toJson(json) + System.lineSeparator(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                lastErrorAt.set(System.currentTimeMillis());
                lastErrorMessage.set(e.getMessage());
            }
        });
    }

    private boolean sample(double rate) {
        if (rate >= 1.0) {
            return true;
        }
        if (rate <= 0) {
            return false;
        }
        return ThreadLocalRandom.current().nextDouble() <= rate;
    }

    private Map<String, Object> base(String eventType) {
        Map<String, Object> json = new HashMap<>();
        json.put("ts", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        json.put("eventType", eventType);
        Map<String, Object> server = new HashMap<>();
        server.put("profile", serverProfile);
        server.put("pluginVersion", pluginVersion);
        json.put("server", server);
        return json;
    }

    private void appendPlayer(Map<String, Object> json, Player player, EnvironmentSnapshot env) {
        Map<String, Object> playerMap = new HashMap<>();
        playerMap.put("uuid", player.getUniqueId().toString());
        if (includePlayerName) {
            playerMap.put("name", player.getName());
        }
        if (env != null) {
            playerMap.put("ping", env.ping());
            playerMap.put("tps", env.serverTps());
            playerMap.put("protocol", env.protocolVersion());
        }
        json.put("player", playerMap);
    }

    private void appendLocation(Map<String, Object> json, Player player) {
        if (!includeLocation) {
            return;
        }
        Location loc = player.getLocation();
        if (loc == null) {
            return;
        }
        Map<String, Object> context = new HashMap<>();
        context.put("world", resolveWorldName(loc));
        context.put("x", loc.getX());
        context.put("y", loc.getY());
        context.put("z", loc.getZ());
        context.put("dimension", resolveDimension(loc));
        json.put("context", context);
    }

    private String resolveWorldName(Location loc) {
        if (loc.getWorld() == null) {
            return "unknown";
        }
        try {
            var method = loc.getWorld().getClass().getMethod("getName");
            Object value = method.invoke(loc.getWorld());
            if (value != null) {
                return value.toString();
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    private String resolveDimension(Location loc) {
        if (loc.getWorld() == null) {
            return "UNKNOWN";
        }
        try {
            var method = loc.getWorld().getClass().getMethod("getEnvironment");
            Object env = method.invoke(loc.getWorld());
            return env == null ? "UNKNOWN" : env.toString();
        } catch (Exception ignored) {
        }
        return "UNKNOWN";
    }

    private void rotateIfNeeded() throws IOException {
        if (maxBytes <= 0) {
            return;
        }
        if (Files.exists(logPath) && Files.size(logPath) >= maxBytes) {
            String rotatedName = logPath.getFileName().toString() + "." + System.currentTimeMillis();
            Files.move(logPath, logPath.resolveSibling(rotatedName), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String toJson(Map<String, ?> json) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : json.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(entry.getKey()).append('"').append(':');
            sb.append(encode(entry.getValue()));
        }
        sb.append('}');
        return sb.toString();
    }

    private String encode(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> coerced = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                coerced.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return toJson(coerced);
        }
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object o : list) {
                if (!first) sb.append(',');
                first = false;
                sb.append(encode(o));
            }
            sb.append(']');
            return sb.toString();
        }
        String str = value.toString().replace("\\", "\\\\").replace("\"", "\\\"");
        return '"' + str + '"';
    }

    private String resolvePluginVersion(UltimateAntiCheatPlugin plugin) {
        try {
            var descMethod = plugin.getClass().getMethod("getDescription");
            Object desc = descMethod.invoke(plugin);
            if (desc != null) {
                var versionMethod = desc.getClass().getMethod("getVersion");
                Object version = versionMethod.invoke(desc);
                if (version != null) {
                    return version.toString();
                }
            }
        } catch (Exception ignored) {
        }
        String impl = plugin.getClass().getPackage() == null ? null : plugin.getClass().getPackage().getImplementationVersion();
        if (impl != null) {
            return impl;
        }
        return "dev";
    }
}
