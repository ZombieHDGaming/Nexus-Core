package com.zombiehdgaming.nexuscore.display;

import com.zombiehdgaming.nexuscore.condition.Condition;
import com.zombiehdgaming.nexuscore.condition.ConditionParser;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The settings every profile shares, parsed from a profile's config section.
 *
 * @param id             lower-case profile id
 * @param priority       selection priority
 * @param updateInterval configured refresh interval in ticks
 * @param condition      combined conditions
 */
public record ProfileSettings(String id, int priority, int updateInterval, Condition condition) {

    private static final Pattern ID = Pattern.compile("[a-z0-9_\\-]+");
    private static final int DEFAULT_INTERVAL = 20;

    /**
     * @return the parsed settings, or {@code null} if the id is invalid (already reported)
     */
    public static @Nullable ProfileSettings parse(
            final String key,
            final ConfigurationSection section,
            final String path,
            final ConditionParser conditions,
            final Logger logger
    ) {
        final String id = key.toLowerCase(Locale.ROOT);
        if (!ID.matcher(id).matches()) {
            logger.warn("{}: id may only contain a-z, 0-9, '_' and '-'; skipped", path);
            return null;
        }
        int interval = section.getInt("update-interval", DEFAULT_INTERVAL);
        if (interval < 1) {
            logger.warn("{}.update-interval must be at least 1 tick; using {}", path, DEFAULT_INTERVAL);
            interval = DEFAULT_INTERVAL;
        }
        return new ProfileSettings(
                id,
                section.getInt("priority", 0),
                interval,
                conditions.parse(section.getConfigurationSection("conditions"), path)
        );
    }

    /**
     * Reads a value that may be written as a single string or a list of strings.
     */
    public static List<String> lines(final ConfigurationSection section, final String key) {
        if (section.isList(key)) {
            return section.getStringList(key);
        }
        final String single = section.getString(key);
        return single == null ? List.of() : List.of(single);
    }
}
