package com.zombiehdgaming.nexuscore.api.placeholder;

import java.util.Set;
import org.bukkit.plugin.Plugin;

/**
 * Registry for Nexus-Core's own placeholders, written as {@code %nexus_<key>%} in any Nexus-Core text.
 *
 * <p>These work without PlaceholderAPI. When PlaceholderAPI is installed they are also exposed through it, so
 * other plugins can use them as well.</p>
 *
 * <p>Placeholders registered by a plugin are removed automatically when that plugin disables.</p>
 */
public interface PlaceholderRegistry {

    /**
     * Registers (or replaces) a placeholder.
     *
     * @param owner    the plugin providing the placeholder
     * @param key      the key, matching {@code [a-z0-9_]+}; used as {@code %nexus_<key>%}
     * @param resolver the value resolver
     * @throws IllegalArgumentException if the key is invalid
     */
    void register(Plugin owner, String key, PlaceholderResolver resolver);

    /**
     * @param key the key to remove
     * @return whether a placeholder was removed
     */
    boolean unregister(String key);

    /**
     * Removes every placeholder registered by a plugin.
     *
     * @param owner the plugin
     */
    void unregisterAll(Plugin owner);

    /**
     * @return a snapshot of all registered keys
     */
    Set<String> keys();
}
