package org.bukkit.configuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Minimal stub for Bukkit's ConfigurationSection used during config migrations.
 */
public class ConfigurationSection {

    protected final Map<String, Object> data = new HashMap<>();

    public Set<String> getKeys(boolean deep) {
        return new HashSet<>(data.keySet());
    }

    public Object get(String path) {
        return data.get(path);
    }

    public void set(String path, Object value) {
        data.put(path, value);
    }

    public boolean contains(String path) {
        return data.containsKey(path);
    }
}
