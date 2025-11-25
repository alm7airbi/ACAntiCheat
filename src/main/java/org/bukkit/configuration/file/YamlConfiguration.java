package org.bukkit.configuration.file;

import java.io.File;
import java.io.Reader;
import java.util.Map;
import java.util.Set;

/**
 * Minimal stub for Bukkit's YamlConfiguration so migration/validation logic can
 * run in offline builds. In Paper this is provided by the server.
 */
public class YamlConfiguration extends FileConfiguration {

    public static YamlConfiguration loadConfiguration(File file) {
        return new YamlConfiguration();
    }

    public static YamlConfiguration loadConfiguration(Reader reader) {
        return new YamlConfiguration();
    }

    public void save(File file) {
        // no-op in stub
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        return super.getKeys(deep);
    }

    @Override
    public void set(String path, Object value) {
        super.set(path, value);
    }

    @Override
    public Object get(String path) {
        return super.get(path);
    }

    public void addDefaults(Map<String, Object> defaults) {
        if (defaults != null) {
            defaults.forEach(this::set);
        }
    }
}
