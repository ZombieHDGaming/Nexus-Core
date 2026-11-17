package com.zombiehdgaming.nexuscore.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zombiehdgaming.nexuscore.placeholder.PlaceholderRegistryImpl;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;

class TextServiceTest {

    private final AtomicLong tick = new AtomicLong();
    private final Player player = mock(Player.class);
    private PlaceholderRegistryImpl registry;
    private TextService text;

    @BeforeEach
    void setUp() {
        registry = new PlaceholderRegistryImpl(NOPLogger.NOP_LOGGER);
        registry.register(mock(Plugin.class), "player", p -> Component.text("Steve"));
        text = new TextService(registry, tick::get, NOPLogger.NOP_LOGGER);
        when(player.getName()).thenReturn("Steve");
    }

    private String plain(final String source) {
        return PlainTextComponentSerializer.plainText().serialize(text.render(text.compile(source), player));
    }

    @Test
    void resolvesInternalPlaceholders() {
        assertThat(plain("Hi %nexus_player%!")).isEqualTo("Hi Steve!");
    }

    @Test
    void leavesUnknownPlaceholdersVisible() {
        assertThat(plain("%nexus_nope% %other_thing%")).isEqualTo("%nexus_nope% %other_thing%");
    }

    @Test
    void externalValuesCannotInjectMiniMessage() {
        text.setExternalPlaceholders((p, token) -> "<red><click:run_command:'/op me'>pwn");
        assertThat(plain("%nick%")).isEqualTo("<red><click:run_command:'/op me'>pwn");
    }

    @Test
    void externalLegacyColoursAreHonoured() {
        text.setExternalPlaceholders((p, token) -> "&cRed");
        final Component rendered = text.render(text.compile("%rank%"), player);
        assertThat(PlainTextComponentSerializer.plainText().serialize(rendered)).isEqualTo("Red");
        assertThat(LegacyText.serialize(rendered)).isEqualTo("§cRed");
    }

    @Test
    void animationFramesFollowTheTickSource() {
        text.setAnimations(Map.of("blink", new Animation("blink", 5, text.compileAll(List.of("A", "B %nexus_player%")))));
        final Template template = text.compile("<animation:blink>");

        tick.set(0);
        assertThat(PlainTextComponentSerializer.plainText().serialize(text.render(template, player))).isEqualTo("A");
        tick.set(5);
        assertThat(PlainTextComponentSerializer.plainText().serialize(text.render(template, player))).isEqualTo("B Steve");
        tick.set(10);
        assertThat(PlainTextComponentSerializer.plainText().serialize(text.render(template, player))).isEqualTo("A");
    }

    @Test
    void selfReferencingAnimationsTerminate() {
        text.setAnimations(Map.of("loop", new Animation("loop", 1, text.compileAll(List.of("x<animation:loop>")))));
        assertThat(plain("<animation:loop>")).isEqualTo("xxxx");
    }

    @Test
    void effectiveIntervalUsesFastestNestedAnimation() {
        text.setAnimations(Map.of(
                "outer", new Animation("outer", 10, text.compileAll(List.of("<animation:inner>"))),
                "inner", new Animation("inner", 3, text.compileAll(List.of("a", "b")))
        ));
        assertThat(text.effectiveInterval(List.of(text.compile("<animation:outer>")), 20)).isEqualTo(3);
        assertThat(text.effectiveInterval(List.of(text.compile("static")), 20)).isEqualTo(20);
    }

    @Test
    void failingResolverIsTreatedAsUnknown() {
        registry.register(mock(Plugin.class), "boom", p -> {
            throw new IllegalStateException("broken add-on");
        });
        assertThat(plain("%nexus_boom%")).isEqualTo("%nexus_boom%");
    }
}
