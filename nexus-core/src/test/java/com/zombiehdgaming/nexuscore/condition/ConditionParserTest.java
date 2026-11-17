package com.zombiehdgaming.nexuscore.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;

class ConditionParserTest {

    private final ConditionParser parser = new ConditionParser(NOPLogger.NOP_LOGGER);
    private final Player player = mock(Player.class);

    @BeforeEach
    void setUp() {
        final World world = mock(World.class);
        when(world.getName()).thenReturn("World_Nether");
        when(player.getWorld()).thenReturn(world);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(player.hasPermission("a")).thenReturn(true);
        when(player.hasPermission("b")).thenReturn(false);
    }

    private Condition parse(final Object... keyValues) {
        final ConfigurationSection section = new MemoryConfiguration();
        for (int i = 0; i < keyValues.length; i += 2) {
            section.set((String) keyValues[i], keyValues[i + 1]);
        }
        return parser.parse(section, "test");
    }

    @Test
    void missingSectionAlwaysMatches() {
        assertThat(parser.parse(null, "test").test(player)).isTrue();
    }

    @Test
    void worldsAreCaseInsensitive() {
        assertThat(parse("worlds", List.of("world_nether")).test(player)).isTrue();
        assertThat(parse("worlds", "world").test(player)).isFalse();
        assertThat(parse("exclude-worlds", List.of("world_nether")).test(player)).isFalse();
    }

    @Test
    void allPermissionsAreRequired() {
        assertThat(parse("permission", "a").test(player)).isTrue();
        assertThat(parse("permission", List.of("a", "b")).test(player)).isFalse();
    }

    @Test
    void gamemodes() {
        assertThat(parse("gamemodes", List.of("survival", "adventure")).test(player)).isTrue();
        assertThat(parse("gamemodes", List.of("creative")).test(player)).isFalse();
    }

    @Test
    void allConditionsMustMatch() {
        assertThat(parse("permission", "a", "gamemodes", List.of("creative")).test(player)).isFalse();
    }

    @Test
    void unknownOrInvalidConditionsFailClosed() {
        assertThat(parse("permision", "a").test(player)).isFalse();
        assertThat(parse("gamemodes", List.of("flying")).test(player)).isFalse();
        assertThat(parse("worlds", List.of()).test(player)).isFalse();
    }
}
