package com.zombiehdgaming.nexuscore.api.scoreboard;

import com.zombiehdgaming.nexuscore.api.display.DisplayService;
import org.bukkit.entity.Player;

/**
 * Per-player sidebar scoreboards.
 *
 * <p>Sidebars are rendered with packets, so they never replace the player's Bukkit {@link
 * org.bukkit.scoreboard.Scoreboard}; teams and objectives managed by other plugins keep working.</p>
 */
public interface ScoreboardService extends DisplayService {

    /**
     * @param player the player
     * @return whether the player has the sidebar toggled on (persisted across sessions)
     */
    boolean isVisible(Player player);

    /**
     * Sets and persists whether the player wants to see the sidebar.
     *
     * @param player  the player
     * @param visible the new state
     */
    void setVisible(Player player, boolean visible);
}
