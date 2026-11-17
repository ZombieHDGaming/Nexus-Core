package com.zombiehdgaming.nexuscore.scoreboard;

import com.zombiehdgaming.nexuscore.scoreboard.packet.SidebarPacketSender;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

/**
 * Mirror of what one client's sidebar currently shows, used to send only what changed.
 *
 * <p>Line {@code i} always uses owner {@code nx-i} and score {@code MAX_LINES - i}. Scores are fixed per slot
 * rather than derived from the line count, so adding or removing lines at the bottom never forces the lines
 * above to be re-sent. Not thread-safe: owned by the player's thread, except {@link #hide()} which only sends
 * packets.</p>
 */
public final class Sidebar {

    public static final int MAX_LINES = 15;
    private static final String[] OWNERS = new String[MAX_LINES];

    static {
        for (int i = 0; i < MAX_LINES; i++) {
            // '-' cannot appear in a player name, so these can never collide with a real score holder.
            OWNERS[i] = "nx-" + i;
        }
    }

    private final Player player;
    private final SidebarPacketSender sender;
    private final Component[] lines = new Component[MAX_LINES];
    private int lineCount;
    private @Nullable Component title;

    public Sidebar(final Player player, final SidebarPacketSender sender) {
        this.player = player;
        this.sender = sender;
    }

    public boolean isShown() {
        return title != null;
    }

    public void render(final Component newTitle, final List<Component> newLines) {
        if (title == null) {
            sender.create(player, newTitle);
            title = newTitle;
        } else if (!title.equals(newTitle)) {
            sender.updateTitle(player, newTitle);
            title = newTitle;
        }

        final int count = Math.min(newLines.size(), MAX_LINES);
        for (int i = 0; i < count; i++) {
            final Component line = newLines.get(i);
            if (i >= lineCount || !line.equals(lines[i])) {
                sender.setLine(player, OWNERS[i], MAX_LINES - i, line);
                lines[i] = line;
            }
        }
        for (int i = count; i < lineCount; i++) {
            sender.removeLine(player, OWNERS[i]);
            lines[i] = null;
        }
        lineCount = count;
    }

    public void hide() {
        if (title != null) {
            sender.remove(player);
        }
        title = null;
        lineCount = 0;
        java.util.Arrays.fill(lines, null);
    }
}
