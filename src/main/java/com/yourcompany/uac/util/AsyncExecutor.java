package com.yourcompany.uac.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shared executor for heavy operations (NBT parsing, persistence) to avoid
 * blocking the main server thread.
 */
public class AsyncExecutor {

    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public void execute(Runnable task) {
        executor.execute(task);
    }

    public void shutdown() {
        executor.shutdown();
    }
}
