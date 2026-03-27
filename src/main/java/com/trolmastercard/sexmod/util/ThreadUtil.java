package com.trolmastercard.sexmod.util;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Determines whether code is executing on the server or client thread.
 * Obfuscated name: g0
 */
public class ThreadUtil {

    /**
     * Returns {@code true} if the current thread is the server thread.
     * Falls back to {@link MinecraftServer#isSameThread()} when thread-name
     * heuristics are inconclusive.
     */
    public static boolean isServerThread() {
        String name = Thread.currentThread().getName().toLowerCase();
        if (name.contains("server")) return true;
        if (name.contains("client")) return false;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;

        boolean onServer = server.isSameThread();
        Main.LOGGER.warn("couldn't clarify if is running on a server or client thread. "
                + "Came to the solution onServer=" + onServer);
        return onServer;
    }
}
