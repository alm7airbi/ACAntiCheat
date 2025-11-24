package org.bukkit.configuration.file;

public class FileConfiguration {
    public int getInt(String path, int def) {
        return def;
    }

    public boolean getBoolean(String path, boolean def) {
        return def;
    }

    public String getString(String path) {
        return null;
    }
}
