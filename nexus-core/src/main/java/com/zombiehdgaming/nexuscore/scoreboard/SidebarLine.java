package com.zombiehdgaming.nexuscore.scoreboard;

import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * One rendered sidebar line.
 *
 * @param text  the left-aligned line text
 * @param score right-aligned text shown in place of the score number, or {@code null} for the board default
 *              (nothing, since the objective hides numbers)
 */
public record SidebarLine(Component text, @Nullable Component score) {

    public static SidebarLine of(final Component text) {
        return new SidebarLine(text, null);
    }
}
