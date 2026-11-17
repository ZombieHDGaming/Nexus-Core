package com.zombiehdgaming.nexuscore.placeholder;

import com.zombiehdgaming.nexuscore.api.placeholder.PlaceholderRegistry;
import com.zombiehdgaming.nexuscore.api.placeholder.PlaceholderResolver;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class PlaceholderRegistryImpl implements PlaceholderRegistry {

    /** Prefix that routes a {@code %token%} to this registry instead of PlaceholderAPI. */
    public static final String PREFIX = "nexus_";
    private static final Pattern KEY = Pattern.compile("[a-z0-9_]+");

    private record Entry(Plugin owner, PlaceholderResolver resolver) {
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Set<String> reportedFailures = ConcurrentHashMap.newKeySet();
    private final Logger logger;

    public PlaceholderRegistryImpl(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void register(final Plugin owner, final String key, final PlaceholderResolver resolver) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(resolver, "resolver");
        if (!KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Placeholder key must match [a-z0-9_]+: " + key);
        }
        entries.put(key, new Entry(owner, resolver));
        reportedFailures.remove(key);
    }

    @Override
    public boolean unregister(final String key) {
        return entries.remove(key.toLowerCase(Locale.ROOT)) != null;
    }

    @Override
    public void unregisterAll(final Plugin owner) {
        entries.values().removeIf(entry -> entry.owner().equals(owner));
    }

    @Override
    public Set<String> keys() {
        return Set.copyOf(entries.keySet());
    }

    /**
     * Resolves a placeholder. A resolver that throws is reported once and then treated as unknown, so one
     * broken add-on cannot flood the console or break every line it appears on.
     *
     * @param player the player
     * @param key    the key without the {@value #PREFIX} prefix
     * @return the value, or {@code null} if unknown or failing
     */
    public @Nullable Component resolve(final Player player, final String key) {
        final Entry entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        try {
            return entry.resolver().resolve(player);
        } catch (final RuntimeException e) {
            if (reportedFailures.add(key)) {
                logger.warn("Placeholder %{}{}% from {} threw an exception", PREFIX, key, entry.owner().getName(), e);
            }
            return null;
        }
    }
}
