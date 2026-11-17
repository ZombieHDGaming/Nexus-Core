package com.zombiehdgaming.nexuscore.condition;

import java.util.List;
import org.bukkit.entity.Player;

/**
 * A predicate deciding whether a display profile applies to a player. Evaluated on the player's owning thread.
 */
@FunctionalInterface
public interface Condition {

    Condition ALWAYS = player -> true;
    Condition NEVER = player -> false;

    boolean test(Player player);

    static Condition all(final List<Condition> conditions) {
        return switch (conditions.size()) {
            case 0 -> ALWAYS;
            case 1 -> conditions.getFirst();
            default -> {
                final List<Condition> copy = List.copyOf(conditions);
                yield player -> {
                    for (final Condition condition : copy) {
                        if (!condition.test(player)) {
                            return false;
                        }
                    }
                    return true;
                };
            }
        };
    }
}
