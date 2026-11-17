package com.zombiehdgaming.nexuscore.scoreboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.zombiehdgaming.nexuscore.condition.Condition;
import com.zombiehdgaming.nexuscore.placeholder.PlaceholderRegistryImpl;
import com.zombiehdgaming.nexuscore.text.TextService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;

class BoardLinesTest {

    private final Player player = mock(Player.class);
    private TextService text;

    @BeforeEach
    void setUp() {
        final PlaceholderRegistryImpl registry = new PlaceholderRegistryImpl(NOPLogger.NOP_LOGGER);
        registry.register(mock(Plugin.class), "ping", p -> Component.text(42));
        text = new TextService(registry, () -> 0L, NOPLogger.NOP_LOGGER);
    }

    private List<BoardLine> parse(final List<?> raw) {
        return BoardLines.parse(raw, "test", text, NOPLogger.NOP_LOGGER);
    }

    private BoardProfile board(final List<BoardLine> lines, final ScoreNumbers numbers) {
        return new BoardProfile("b", 0, Condition.ALWAYS, 20, text.compile("T"), lines, numbers);
    }

    private static String plain(final Component component) {
        return component == null ? null : PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void parsesPlainStringsAndTextScoreMaps() {
        final Map<String, Object> withScore = new LinkedHashMap<>();
        withScore.put("text", "Ping");
        withScore.put("score", "%nexus_ping%ms");

        final List<BoardLine> lines = parse(List.of("Plain", withScore, Map.of("text", "No score")));

        assertThat(lines).hasSize(3);
        assertThat(lines.get(0).score()).isNull();
        assertThat(lines.get(1).score()).isNotNull();
        assertThat(lines.get(2).score()).isNull();
    }

    @Test
    void malformedEntriesKeepTheirSlotAsBlankLines() {
        final List<BoardLine> lines = parse(java.util.Arrays.asList("a", List.of("nested"), null, "d"));

        assertThat(lines).hasSize(4);
        assertThat(lines.get(1).text().source()).isEmpty();
        assertThat(lines.get(2).text().source()).isEmpty();
    }

    @Test
    void capsAtFifteenLines() {
        assertThat(parse(Collections.nCopies(20, "x"))).hasSize(Sidebar.MAX_LINES);
    }

    @Test
    void hiddenNumbersSendNoScoreTextUnlessTheLineDefinesOne() {
        final List<BoardLine> lines = parse(List.of("a", Map.of("text", "Ping", "score", "%nexus_ping%ms")));

        final List<SidebarLine> rendered = BoardLines.render(board(lines, ScoreNumbers.HIDDEN), player, text);

        assertThat(rendered.get(0).score()).isNull();
        assertThat(plain(rendered.get(1).text())).isEqualTo("Ping");
        assertThat(plain(rendered.get(1).score())).isEqualTo("42ms");
    }

    @Test
    void shownNumbersCountDownToOneAndCustomScoresWin() {
        final List<BoardLine> lines = parse(List.of("a", Map.of("text", "b", "score", "custom"), "c"));

        final List<SidebarLine> rendered = BoardLines.render(board(lines, ScoreNumbers.SHOWN), player, text);

        assertThat(rendered.get(0).score()).isEqualTo(Component.text(3, NamedTextColor.RED));
        assertThat(plain(rendered.get(1).score())).isEqualTo("custom");
        assertThat(rendered.get(2).score()).isEqualTo(Component.text(1, NamedTextColor.RED));
    }

    @Test
    void parsesScoreNumberModes() {
        assertThat(ScoreNumbers.parse("Hidden")).isEqualTo(ScoreNumbers.HIDDEN);
        assertThat(ScoreNumbers.parse("shown")).isEqualTo(ScoreNumbers.SHOWN);
        assertThat(ScoreNumbers.parse("true")).isEqualTo(ScoreNumbers.SHOWN);
        assertThat(ScoreNumbers.parse("sometimes")).isNull();
    }
}
