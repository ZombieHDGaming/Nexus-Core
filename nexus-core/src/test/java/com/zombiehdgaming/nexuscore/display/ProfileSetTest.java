package com.zombiehdgaming.nexuscore.display;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.zombiehdgaming.nexuscore.condition.Condition;
import java.util.List;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class ProfileSetTest {

    private record Profile(String id, int priority, Condition condition) implements DisplayProfile {
        @Override
        public int refreshTicks() {
            return 20;
        }
    }

    private final Player player = mock(Player.class);

    @Test
    void highestMatchingPriorityWins() {
        final ProfileSet<Profile> set = new ProfileSet<>(List.of(
                new Profile("default", 0, Condition.ALWAYS),
                new Profile("vip", 50, Condition.ALWAYS),
                new Profile("staff", 100, Condition.NEVER)
        ));

        assertThat(set.select(player)).extracting(Profile::id).isEqualTo("vip");
    }

    @Test
    void tiesAreBrokenByIdDeterministically() {
        final ProfileSet<Profile> set = new ProfileSet<>(List.of(
                new Profile("zeta", 5, Condition.ALWAYS),
                new Profile("alpha", 5, Condition.ALWAYS)
        ));

        assertThat(set.select(player)).extracting(Profile::id).isEqualTo("alpha");
    }

    @Test
    void nothingMatchesReturnsNull() {
        final ProfileSet<Profile> set = new ProfileSet<>(List.of(new Profile("a", 0, Condition.NEVER)));
        assertThat(set.select(player)).isNull();
        assertThat(ProfileSet.<Profile>empty().select(player)).isNull();
    }
}
