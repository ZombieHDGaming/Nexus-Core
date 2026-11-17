package com.zombiehdgaming.nexuscore.display;

import com.zombiehdgaming.nexuscore.api.display.DisplayService;
import com.zombiehdgaming.nexuscore.config.ConfigException;
import com.zombiehdgaming.nexuscore.feature.Feature;
import com.zombiehdgaming.nexuscore.scheduler.PlayerTasks;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Shared engine for per-player displays that are chosen from prioritised, conditional profiles and refreshed
 * on a timer.
 *
 * <h2>Threading model</h2>
 * <p>Each online player has a {@link Session} whose refresh task runs on the player's entity scheduler: the
 * main thread on Paper, the owning region thread on Folia. All profile selection, placeholder resolution and
 * rendering therefore happens on the thread that is allowed to read that player's state. Shared state
 * ({@link #profiles}, {@link #running}) is immutable and published through volatile fields.</p>
 *
 * <p>Subclasses must make {@link #clear} safe to call from any thread, because shutdown cannot hop onto
 * every player's thread. Sending packets satisfies this.</p>
 *
 * <h2>Refresh cadence</h2>
 * <p>A shown profile refreshes every {@link DisplayProfile#refreshTicks()} ticks. With nothing to show, the
 * session idles at {@link #IDLE_REFRESH_TICKS} so permission or world changes are still picked up. World and
 * game-mode changes trigger an immediate refresh.</p>
 *
 * @param <P> profile type
 * @param <S> per-player render state (e.g. what the client currently displays, for diffing)
 */
public abstract class DisplayFeature<P extends DisplayProfile, S> implements Feature, DisplayService, Listener {

    protected static final int IDLE_REFRESH_TICKS = 20;

    protected final Plugin plugin;
    protected final Logger logger;

    private final ConcurrentMap<UUID, Session<P, S>> sessions = new ConcurrentHashMap<>();
    private volatile ProfileSet<P> profiles = ProfileSet.empty();
    private volatile boolean running;

    protected DisplayFeature(final Plugin plugin, final Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    // ------------------------------------------------------------------ subclass contract

    /** Parses the feature's config. Must not touch live state; throwing keeps the previous profiles. */
    protected abstract ProfileSet<P> loadProfiles() throws ConfigException;

    /** Creates render state for a player. Called on the player's thread. */
    protected abstract S createState(Player player);

    /** Renders a profile. Called on the player's thread. */
    protected abstract void render(Player player, P profile, S state);

    /** Removes whatever is displayed. May be called from any thread. */
    protected abstract void clear(Player player, S state);

    /** Player-level opt-out (e.g. the sidebar toggle). Called on the player's thread. */
    protected boolean shouldDisplay(final Player player) {
        return true;
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    public final boolean isRunning() {
        return running;
    }

    @Override
    public void enable() throws ConfigException {
        if (running) {
            return;
        }
        profiles = loadProfiles();
        running = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getOnlinePlayers().forEach(this::start);
        logger.info("[{}] Loaded {} profile(s): {}", id(), profiles.size(), String.join(", ", profiles.ids()));
    }

    @Override
    public void reload() throws ConfigException {
        final ProfileSet<P> loaded = loadProfiles();
        profiles = loaded;
        sessions.values().forEach(session -> PlayerTasks.run(plugin, session.player, () -> tick(session)));
        logger.info("[{}] Reloaded {} profile(s): {}", id(), loaded.size(), String.join(", ", loaded.ids()));
    }

    @Override
    public void disable() {
        if (!running) {
            return;
        }
        running = false;
        HandlerList.unregisterAll(this);
        for (final Session<P, S> session : sessions.values()) {
            session.close();
            if (session.active != null) {
                clear(session.player, session.state);
            }
        }
        sessions.clear();
        profiles = ProfileSet.empty();
    }

    // ------------------------------------------------------------------ events

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        start(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(final PlayerQuitEvent event) {
        final Session<P, S> session = sessions.remove(event.getPlayer().getUniqueId());
        if (session != null) {
            session.close();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(final PlayerChangedWorldEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(final PlayerGameModeChangeEvent event) {
        // Fired before the mode is applied, so look again on the next tick.
        final Player player = event.getPlayer();
        PlayerTasks.runLater(plugin, player, 1L, () -> tickIfPresent(player));
    }

    // ------------------------------------------------------------------ DisplayService

    @Override
    public boolean isEnabled() {
        return running;
    }

    @Override
    public Set<String> profileIds() {
        return profiles.ids();
    }

    @Override
    public Optional<String> activeProfile(final Player player) {
        final Session<P, S> session = sessions.get(player.getUniqueId());
        final P active = session == null ? null : session.active;
        return Optional.ofNullable(active).map(DisplayProfile::id);
    }

    @Override
    public void forceProfile(final Player player, final @Nullable String profileId) {
        if (profileId != null && profiles.get(profileId) == null) {
            throw new IllegalArgumentException("Unknown profile '" + profileId + "' for " + id());
        }
        final Session<P, S> session = sessions.get(player.getUniqueId());
        if (session != null) {
            session.forced = profileId;
            PlayerTasks.run(plugin, player, () -> tick(session));
        }
    }

    @Override
    public void refresh(final Player player) {
        PlayerTasks.run(plugin, player, () -> tickIfPresent(player));
    }

    protected ProfileSet<P> profiles() {
        return profiles;
    }

    // ------------------------------------------------------------------ internals

    private void start(final Player player) {
        PlayerTasks.run(plugin, player, () -> {
            if (!running || !player.isOnline()) {
                return;
            }
            final Session<P, S> session = new Session<>(player, createState(player));
            final Session<P, S> previous = sessions.put(player.getUniqueId(), session);
            if (previous != null) {
                previous.close();
            }
            tick(session);
        });
    }

    private void tickIfPresent(final Player player) {
        final Session<P, S> session = sessions.get(player.getUniqueId());
        if (session != null) {
            tick(session);
        }
    }

    /** Selects and renders the right profile. Always runs on the player's thread. */
    private void tick(final Session<P, S> session) {
        if (session.closed || !running) {
            return;
        }
        final Player player = session.player;
        try {
            final P target = select(session);
            if (target == null) {
                if (session.active != null) {
                    clear(player, session.state);
                    session.active = null;
                }
                schedule(session, IDLE_REFRESH_TICKS);
                return;
            }
            session.active = target;
            render(player, target, session.state);
            schedule(session, target.refreshTicks());
            session.failureReported = false;
        } catch (final RuntimeException e) {
            if (!session.failureReported) {
                session.failureReported = true;
                logger.error("[{}] Failed to update display for {}", id(), player.getName(), e);
            }
            schedule(session, IDLE_REFRESH_TICKS);
        }
    }

    private @Nullable P select(final Session<P, S> session) {
        if (!shouldDisplay(session.player)) {
            return null;
        }
        final ProfileSet<P> set = profiles;
        final String forced = session.forced;
        if (forced != null) {
            final P profile = set.get(forced);
            if (profile != null) {
                return profile;
            }
            session.forced = null; // removed by a reload
        }
        return set.select(session.player);
    }

    private void schedule(final Session<P, S> session, final int period) {
        final ScheduledTask current = session.task;
        if (current != null && !current.isCancelled() && session.period == period) {
            return;
        }
        if (current != null) {
            current.cancel();
        }
        session.period = period;
        session.task = session.player.getScheduler().runAtFixedRate(plugin, task -> tick(session), null, period, period);
    }

    private static final class Session<P, S> {
        final Player player;
        final S state;
        volatile @Nullable P active;
        volatile @Nullable String forced;
        volatile boolean closed;
        @Nullable ScheduledTask task;
        int period;
        boolean failureReported;

        Session(final Player player, final S state) {
            this.player = player;
            this.state = state;
        }

        void close() {
            closed = true;
            final ScheduledTask current = task;
            if (current != null) {
                current.cancel();
            }
        }
    }
}
