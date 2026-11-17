package com.zombiehdgaming.nexuscore.scoreboard;

import com.zombiehdgaming.nexuscore.text.Template;
import com.zombiehdgaming.nexuscore.text.TextService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Parsing and rendering of sidebar lines.
 *
 * <p>A line is either a plain string, or a map with {@code text} and an optional {@code score} (right-aligned
 * text shown in place of the score number):</p>
 * <pre>
 * lines:
 *   - "&lt;gray&gt;Plain line"
 *   - text: "&lt;gray&gt;Ping"
 *     score: "&lt;white&gt;%nexus_ping%ms"
 * </pre>
 */
final class BoardLines {

    private static final Set<String> KNOWN_KEYS = Set.of("text", "score");

    private BoardLines() {
    }

    /**
     * @param raw  the value of {@code lines} as returned by {@code ConfigurationSection#getList}
     * @param path location for error messages
     * @return parsed lines, at most {@link Sidebar#MAX_LINES}; malformed entries become blank lines so the
     *         positions of the remaining lines are preserved
     */
    static List<BoardLine> parse(final List<?> raw, final String path, final TextService text, final Logger logger) {
        if (raw.size() > Sidebar.MAX_LINES) {
            logger.warn("{}.lines: has {} lines but the sidebar shows at most {}; extra lines are ignored",
                    path, raw.size(), Sidebar.MAX_LINES);
        }
        final int count = Math.min(raw.size(), Sidebar.MAX_LINES);
        final List<BoardLine> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add(parseLine(raw.get(i), path + ".lines[" + (i + 1) + "]", text, logger));
        }
        return List.copyOf(lines);
    }

    private static BoardLine parseLine(final @Nullable Object entry, final String path, final TextService text,
                                       final Logger logger) {
        if (entry instanceof final Map<?, ?> map) {
            for (final Object key : map.keySet()) {
                if (!KNOWN_KEYS.contains(String.valueOf(key))) {
                    logger.warn("{}: unknown key '{}' (expected 'text' and optionally 'score')", path, key);
                }
            }
            final Object rawText = map.get("text");
            final Object rawScore = map.get("score");
            final Template score = rawScore == null ? null : text.compile(String.valueOf(rawScore));
            return new BoardLine(text.compile(rawText == null ? "" : String.valueOf(rawText)), score);
        }
        if (entry instanceof String || entry instanceof Number || entry instanceof Boolean) {
            return new BoardLine(text.compile(String.valueOf(entry)), null);
        }
        logger.warn("{}: expected a string or a map with 'text'/'score'; shown as a blank line", path);
        return new BoardLine(text.compile(""), null);
    }

    /**
     * @return every template on the board, for working out the refresh interval
     */
    static List<Template> templates(final List<BoardLine> lines) {
        final List<Template> out = new ArrayList<>(lines.size() * 2);
        for (final BoardLine line : lines) {
            out.add(line.text());
            if (line.score() != null) {
                out.add(line.score());
            }
        }
        return out;
    }

    static List<SidebarLine> render(final BoardProfile profile, final Player player, final TextService text) {
        final List<BoardLine> lines = profile.lines();
        final int count = lines.size();
        final List<SidebarLine> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final BoardLine line = lines.get(i);
            final Template scoreTemplate = line.score();
            final Component score;
            if (scoreTemplate != null) {
                score = text.render(scoreTemplate, player);
            } else if (profile.scoreNumbers() == ScoreNumbers.SHOWN) {
                // Vanilla look: red numbers counting down to 1 at the bottom.
                score = Component.text(count - i, NamedTextColor.RED);
            } else {
                score = null;
            }
            out.add(new SidebarLine(text.render(line.text(), player), score));
        }
        return out;
    }
}
