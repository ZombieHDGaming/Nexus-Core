package com.zombiehdgaming.nexuscore.scoreboard.packet;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

/**
 * Low-level sidebar protocol operations for a single client-side objective.
 *
 * <p>This is the only seam between Nexus-Core and server internals for the sidebar; if the protocol changes
 * in a future Minecraft version, this is the one class to update. Implementations must be thread-safe.</p>
 */
public interface SidebarPacketSender {

    /** Creates the objective with hidden score numbers and shows it in the sidebar slot. */
    void create(Player player, Component title);

    void updateTitle(Player player, Component title);

    /** Removes the objective (and with it every line). */
    void remove(Player player);

    /**
     * Creates or updates a line.
     *
     * @param owner     unique, invisible id of the line
     * @param score     sort key; higher is shown first
     * @param text      the visible text
     * @param scoreText right-aligned text replacing the score number, or {@code null} to show nothing
     */
    void setLine(Player player, String owner, int score, Component text, @Nullable Component scoreText);

    void removeLine(Player player, String owner);
}
