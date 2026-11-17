package com.zombiehdgaming.nexuscore.api.placeholder;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

/**
 * Produces the value of a placeholder for a player.
 *
 * <p>Resolvers are invoked on the player's owning thread (the main thread on Paper, the player's region thread
 * on Folia) and may be called many times per second, so they should be cheap and must not block.</p>
 */
@FunctionalInterface
public interface PlaceholderResolver {

    /**
     * @param player the player the text is being rendered for
     * @return the value, or {@code null} to leave the placeholder text untouched
     */
    @Nullable Component resolve(Player player);
}
