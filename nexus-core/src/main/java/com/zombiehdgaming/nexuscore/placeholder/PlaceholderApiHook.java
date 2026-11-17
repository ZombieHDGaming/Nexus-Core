package com.zombiehdgaming.nexuscore.placeholder;

import com.zombiehdgaming.nexuscore.text.ExternalPlaceholders;
import com.zombiehdgaming.nexuscore.text.LegacyText;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

/**
 * All PlaceholderAPI-specific code lives here. Never reference this class unless PlaceholderAPI is enabled,
 * otherwise class loading fails.
 */
public final class PlaceholderApiHook implements ExternalPlaceholders {

    public static final String PLUGIN_NAME = "PlaceholderAPI";

    private final Plugin plugin;
    private final PlaceholderRegistryImpl registry;
    private @Nullable Expansion expansion;

    public PlaceholderApiHook(final Plugin plugin, final PlaceholderRegistryImpl registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @Override
    public @Nullable String resolve(final Player player, final String token) {
        final String raw = '%' + token + '%';
        final String value = PlaceholderAPI.setPlaceholders(player, raw);
        // PlaceholderAPI returns the input unchanged for unknown placeholders.
        return value.equals(raw) ? null : value;
    }

    /** Exposes {@code %nexus_*%} to every other PlaceholderAPI consumer. */
    public void registerExpansion() {
        if (expansion == null) {
            expansion = new Expansion();
            expansion.register();
        }
    }

    public void unregisterExpansion() {
        if (expansion != null) {
            expansion.unregister();
            expansion = null;
        }
    }

    private final class Expansion extends PlaceholderExpansion {

        @Override
        public String getIdentifier() {
            return "nexus";
        }

        @Override
        public String getAuthor() {
            return String.join(", ", plugin.getPluginMeta().getAuthors());
        }

        @Override
        public String getVersion() {
            return plugin.getPluginMeta().getVersion();
        }

        @Override
        public boolean persist() {
            // Owned by Nexus-Core's lifecycle, so survive /papi reload.
            return true;
        }

        @Override
        public @Nullable String onRequest(final @Nullable OfflinePlayer offline, final String params) {
            if (offline == null || !(offline.getPlayer() instanceof final Player player)) {
                return null;
            }
            final Component value = registry.resolve(player, params.toLowerCase(java.util.Locale.ROOT));
            return value == null ? null : LegacyText.serialize(value);
        }
    }
}
