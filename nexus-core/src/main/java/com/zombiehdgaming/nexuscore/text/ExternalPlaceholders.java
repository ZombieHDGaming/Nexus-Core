package com.zombiehdgaming.nexuscore.text;

import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

/**
 * A source of string placeholders outside Nexus-Core, i.e. PlaceholderAPI. Kept behind an interface so the
 * PlaceholderAPI classes are only ever loaded when the plugin is actually installed.
 */
@FunctionalInterface
public interface ExternalPlaceholders {

    /**
     * @param player the player
     * @param token  the placeholder without surrounding percent signs, e.g. {@code vault_eco_balance}
     * @return the value, or {@code null} if the placeholder is unknown
     */
    @Nullable String resolve(Player player, String token);
}
