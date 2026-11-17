package com.zombiehdgaming.nexuscore.scheduler;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Helpers for running work on a player's owning thread.
 *
 * <p>Paper exposes the Folia-style entity scheduler on every server, so routing per-player work through it is
 * correct on both: on Paper it is the main thread, on Folia it is the region currently owning the player.</p>
 */
public final class PlayerTasks {

    private PlayerTasks() {
    }

    /**
     * Runs a task on the player's owning thread on the next tick. Silently dropped if the player has already
     * left (the entity is "retired"), which is the behaviour every caller wants.
     *
     * @param plugin the owning plugin
     * @param player the player
     * @param task   the work
     */
    public static void run(final Plugin plugin, final Player player, final Runnable task) {
        player.getScheduler().run(plugin, scheduled -> task.run(), null);
    }

    /**
     * Same as {@link #run} but after a delay.
     *
     * @param plugin     the owning plugin
     * @param player     the player
     * @param delayTicks delay, at least 1
     * @param task       the work
     */
    public static void runLater(final Plugin plugin, final Player player, final long delayTicks, final Runnable task) {
        player.getScheduler().runDelayed(plugin, scheduled -> task.run(), null, Math.max(1L, delayTicks));
    }
}
