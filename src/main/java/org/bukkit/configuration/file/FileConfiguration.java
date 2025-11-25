package org.bukkit.configuration.file;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Set;

/**
 * Extremely small stub of Bukkit's FileConfiguration backed by a flat map so
 * unit/CI builds can compile and basic config merging logic can run without
 * the real YAML implementation. The real Paper runtime replaces this with the
 * Bukkit/YAML-backed configuration API.
 */
public class FileConfiguration extends ConfigurationSection {

    protected FileConfiguration defaults;
    private final Options options = new Options(this);

    public int getInt(String path, int def) {
        Object val = get(path);
        return val instanceof Number ? ((Number) val).intValue() : def;
    }

    public boolean getBoolean(String path, boolean def) {
        Object val = get(path);
        return val instanceof Boolean ? (Boolean) val : def;
    }

    public double getDouble(String path, double def) {
        Object val = get(path);
        return val instanceof Number ? ((Number) val).doubleValue() : def;
    }

    public String getString(String path) {
        Object val = get(path);
        return val == null ? null : val.toString();
    }

    public String getString(String path, String def) {
        String str = getString(path);
        return str == null ? def : str;
    }

    public long getLong(String path, long def) {
        Object val = get(path);
        return val instanceof Number ? ((Number) val).longValue() : def;
    }

    public Object get(String path) {
        if (data.containsKey(path)) {
            return data.get(path);
        }
        if (defaults != null) {
            return defaults.get(path);
        }
        return null;
    }

    public void set(String path, Object value) {
        data.put(path, value);
    }

    public boolean contains(String path) {
        return data.containsKey(path) || (defaults != null && defaults.contains(path));
    }

    public void setDefaults(FileConfiguration defaults) {
        this.defaults = defaults;
    }

    public FileConfiguration getDefaults() {
        return defaults;
    }

    public Options options() {
        return options;
    }

    public Set<String> getKeys(boolean deep) {
        Set<String> keys = new HashSet<>(data.keySet());
        if (defaults != null) {
            keys.addAll(defaults.getKeys(deep));
        }
        return keys;
    }

    public static class Options {
        private final FileConfiguration parent;
        private boolean copyDefaults;

        public Options(FileConfiguration parent) {
            this.parent = parent;
        }

        public Options copyDefaults(boolean value) {
            this.copyDefaults = value;
            return this;
        }

        public boolean copyDefaults() {
            return copyDefaults;
        }

        public FileConfiguration configuration() {
            return parent;
        }
    }
}
