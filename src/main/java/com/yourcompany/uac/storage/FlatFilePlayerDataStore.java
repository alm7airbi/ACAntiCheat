package com.yourcompany.uac.storage;

import com.yourcompany.uac.checks.PlayerCheckState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * Lightweight file-backed store suitable for this offline environment. Real
 * deployments can replace this with a Mongo/SQL-backed implementation.
 */
public class FlatFilePlayerDataStore implements PlayerDataStore {

    private final Path stateDir;
    private final Path historyDir;

    public FlatFilePlayerDataStore(Path baseDir) {
        this.stateDir = baseDir.resolve("state");
        this.historyDir = baseDir.resolve("history");
        try {
            Files.createDirectories(stateDir);
            Files.createDirectories(historyDir);
        } catch (IOException ignored) {
        }
    }

    @Override
    public Optional<PlayerSnapshot> load(UUID playerId) {
        Path file = stateDir.resolve(playerId.toString() + ".properties");
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        Properties props = new Properties();
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            return Optional.empty();
        }
        double trust = Double.parseDouble(props.getProperty("trust", "100"));
        int schemaVersion = Integer.parseInt(props.getProperty("schemaVersion", "0"));
        Map<String, Integer> flags = new HashMap<>();
        for (String name : props.stringPropertyNames()) {
            if (name.startsWith("flag.")) {
                flags.put(name.substring(5), Integer.parseInt(props.getProperty(name, "0")));
            }
        }
        List<String> mitigation = new ArrayList<>();
        String rawMitigation = props.getProperty("mitigations", "");
        if (!rawMitigation.isEmpty()) {
            for (String part : rawMitigation.split("\\|", -1)) {
                if (!part.isEmpty()) {
                    mitigation.add(part);
                }
            }
        }
        long lastUpdated = Long.parseLong(props.getProperty("lastUpdated", String.valueOf(System.currentTimeMillis())));
        return Optional.of(new PlayerSnapshot(schemaVersion, trust, flags, mitigation, lastUpdated));
    }

    @Override
    public void saveState(PlayerCheckState state) {
        Properties props = new Properties();
        props.setProperty("schemaVersion", String.valueOf(state.getSchemaVersion()));
        props.setProperty("trust", String.valueOf(state.getTrustScore()));
        StringBuilder mitigation = new StringBuilder();
        for (String entry : state.getMitigationHistory()) {
            if (!mitigation.isEmpty()) {
                mitigation.append("|");
            }
            mitigation.append(entry.replace("|", "/"));
        }
        props.setProperty("mitigations", mitigation.toString());
        state.getFlagCounts().forEach((check, count) -> props.setProperty("flag." + check, String.valueOf(count)));
        props.setProperty("lastUpdated", String.valueOf(System.currentTimeMillis()));
        Path file = stateDir.resolve(state.getPlayerId().toString() + ".properties");
        try (var writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            props.store(writer, "ACAntiCheat player snapshot");
        } catch (IOException ignored) {
        }
    }

    @Override
    public void appendHistory(UUID playerId, String entry, int limit) {
        Path file = historyDir.resolve(playerId.toString() + ".log");
        try {
            Files.writeString(file, entry + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            prune(file, limit);
        } catch (IOException ignored) {
        }
    }

    @Override
    public List<String> loadHistory(UUID playerId, int limit) {
        Path file = historyDir.resolve(playerId.toString() + ".log");
        if (!Files.exists(file)) {
            return List.of();
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return List.of();
        }
        if (lines.size() <= limit) {
            return lines;
        }
        return new ArrayList<>(lines.subList(Math.max(0, lines.size() - limit), lines.size()));
    }

    private void prune(Path file, int limit) throws IOException {
        if (limit <= 0 || !Files.exists(file)) {
            return;
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.size() <= limit) {
            return;
        }
        List<String> pruned = new ArrayList<>(lines.subList(lines.size() - limit, lines.size()));
        Files.write(file, pruned, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }
}
