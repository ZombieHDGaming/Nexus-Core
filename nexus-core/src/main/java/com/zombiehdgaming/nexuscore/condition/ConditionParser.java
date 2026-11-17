package com.zombiehdgaming.nexuscore.condition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Parses a {@code conditions:} section into a single {@link Condition}. All listed conditions must match.
 *
 * <p>Unknown or invalid conditions <strong>fail closed</strong> (never match) and are reported. A typo in
 * {@code permision:} should hide a staff board, not show it to everyone.</p>
 *
 * <p>New condition types are added by registering a {@link Factory}; nothing else needs to change.</p>
 */
public final class ConditionParser {

    /**
     * Builds a condition from its raw YAML value.
     */
    @FunctionalInterface
    public interface Factory {
        /**
         * @param value the raw value (string, list, number, section...)
         * @return the condition
         * @throws IllegalArgumentException with a user-facing message if the value is invalid
         */
        Condition create(Object value);
    }

    private final Map<String, Factory> factories = new LinkedHashMap<>();
    private final Logger logger;

    public ConditionParser(final Logger logger) {
        this.logger = logger;
        register("worlds", value -> {
            final Set<String> worlds = lowerSet(value);
            return player -> worlds.contains(player.getWorld().getName().toLowerCase(Locale.ROOT));
        });
        register("exclude-worlds", value -> {
            final Set<String> worlds = lowerSet(value);
            return player -> !worlds.contains(player.getWorld().getName().toLowerCase(Locale.ROOT));
        });
        register("permission", value -> {
            final List<String> permissions = stringList(value);
            return player -> {
                for (final String permission : permissions) {
                    if (!player.hasPermission(permission)) {
                        return false;
                    }
                }
                return true;
            };
        });
        register("gamemodes", value -> {
            final Set<GameMode> modes = new HashSet<>();
            for (final String raw : stringList(value)) {
                try {
                    modes.add(GameMode.valueOf(raw.toUpperCase(Locale.ROOT)));
                } catch (final IllegalArgumentException e) {
                    throw new IllegalArgumentException("unknown game mode '" + raw + "'");
                }
            }
            return player -> modes.contains(player.getGameMode());
        });
    }

    public void register(final String key, final Factory factory) {
        factories.put(key.toLowerCase(Locale.ROOT), factory);
    }

    public Set<String> keys() {
        return factories.keySet();
    }

    /**
     * @param section the {@code conditions} section, or {@code null} if absent
     * @param path    human-readable location for error messages
     * @return the combined condition ({@link Condition#ALWAYS} when there are none)
     */
    public Condition parse(final @Nullable ConfigurationSection section, final String path) {
        if (section == null) {
            return Condition.ALWAYS;
        }
        final List<Condition> conditions = new ArrayList<>();
        for (final String key : section.getKeys(false)) {
            final Factory factory = factories.get(key.toLowerCase(Locale.ROOT));
            final Object value = section.get(key);
            if (factory == null) {
                logger.warn("{}.conditions.{}: unknown condition (known: {}); this profile will never match",
                        path, key, String.join(", ", factories.keySet()));
                conditions.add(Condition.NEVER);
                continue;
            }
            try {
                conditions.add(factory.create(value == null ? "" : value));
            } catch (final IllegalArgumentException e) {
                logger.warn("{}.conditions.{}: {}; this profile will never match", path, key, e.getMessage());
                conditions.add(Condition.NEVER);
            }
        }
        return Condition.all(conditions);
    }

    private static List<String> stringList(final Object value) {
        final List<String> out = new ArrayList<>();
        if (value instanceof final List<?> list) {
            for (final Object element : list) {
                if (element != null) {
                    out.add(element.toString().trim());
                }
            }
        } else {
            out.add(value.toString().trim());
        }
        out.removeIf(String::isEmpty);
        if (out.isEmpty()) {
            throw new IllegalArgumentException("expected a value or a list of values");
        }
        return List.copyOf(out);
    }

    private static Set<String> lowerSet(final Object value) {
        final Set<String> out = new HashSet<>();
        for (final String entry : stringList(value)) {
            out.add(entry.toLowerCase(Locale.ROOT));
        }
        return Set.copyOf(out);
    }
}
