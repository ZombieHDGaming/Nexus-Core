package com.zombiehdgaming.nexuscore.api.display;

import java.util.Optional;
import java.util.Set;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

/**
 * Common contract for per-player displays (sidebar, tab list, ...) driven by configured <em>profiles</em>.
 *
 * <p>Each profile has a priority and a set of conditions. On every refresh the highest-priority profile whose
 * conditions match the player is shown, unless a profile has been forced via {@link #forceProfile}.</p>
 *
 * <p>Methods taking a {@link Player} may be called from any thread; work is dispatched to the player's owning
 * thread internally (this matters on Folia).</p>
 */
public interface DisplayService {

    /**
     * @return whether the feature backing this service is enabled in the configuration
     */
    boolean isEnabled();

    /**
     * @return ids of all currently loaded profiles
     */
    Set<String> profileIds();

    /**
     * @param player the player
     * @return the id of the profile currently shown to the player, if any
     */
    Optional<String> activeProfile(Player player);

    /**
     * Pins a player to a specific profile, bypassing priority and conditions. Useful for minigames or
     * temporary states. The override is held in memory only and is cleared when the player quits.
     *
     * @param player    the player
     * @param profileId the profile to force, or {@code null} to return to automatic selection
     * @throws IllegalArgumentException if {@code profileId} is not a loaded profile
     */
    void forceProfile(Player player, @Nullable String profileId);

    /**
     * Re-evaluates and re-renders the display for a player on their next owning-thread tick.
     *
     * @param player the player
     */
    void refresh(Player player);
}
