package com.zombiehdgaming.nexuscore.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

/**
 * A server-wide tick counter that is safe to read from any thread.
 *
 * <p>Driven by the global region scheduler, so it works identically on Paper and Folia (where
 * {@code Bukkit.getCurrentTick()} has no single meaning). Animations use it to pick frames, which keeps every
 * player's animation in sync regardless of which region they are ticked on.</p>
 */
public final class TickClock {

    private final AtomicLong ticks = new AtomicLong();
    private @Nullable ScheduledTask task;

    public void start(final Plugin plugin) {
        stop();
        task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> ticks.incrementAndGet(), 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public long currentTick() {
        return ticks.get();
    }
}
