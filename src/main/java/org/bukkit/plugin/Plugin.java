package org.bukkit.plugin;

import org.bukkit.Server;

import java.util.logging.Logger;

public interface Plugin {
    Server getServer();

    Logger getLogger();
}
