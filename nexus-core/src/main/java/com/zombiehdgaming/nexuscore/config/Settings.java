package com.zombiehdgaming.nexuscore.config;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

/**
 * Immutable snapshot of {@code config.yml}.
 *
 * @param features             per-feature on/off switches, keyed by feature id
 * @param registerExpansion    whether to expose {@code %nexus_*%} through PlaceholderAPI
 * @param sidebarDefaultVisible whether players see the sidebar until they toggle it off
 */
public record Settings(
        Map<String, Boolean> features,
        boolean registerExpansion,
        boolean sidebarDefaultVisible
) {

    public static final String FILE = "config.yml";
    public static final int CURRENT_VERSION = 1;

    public static Settings load(final Plugin plugin, final Logger logger) throws ConfigException {
        return from(YamlFiles.load(plugin, FILE, true), logger);
    }

    public static Settings bundledDefaults(final Plugin plugin, final Logger logger) {
        return from(YamlFiles.bundled(plugin, FILE), logger);
    }

    private static Settings from(final YamlConfiguration yaml, final Logger logger) {
        final int version = yaml.getInt("config-version", CURRENT_VERSION);
        if (version != CURRENT_VERSION) {
            logger.warn("{} has config-version {} but this build expects {}. Missing keys use defaults; "
                    + "compare with the bundled file for new options.", FILE, version, CURRENT_VERSION);
        }

        final Map<String, Boolean> features = new HashMap<>();
        final ConfigurationSection section = yaml.getConfigurationSection("features");
        if (section != null) {
            for (final String key : section.getKeys(false)) {
                features.put(key.toLowerCase(Locale.ROOT), section.getBoolean(key, true));
            }
        }

        return new Settings(
                Map.copyOf(features),
                yaml.getBoolean("placeholders.register-expansion", true),
                yaml.getBoolean("scoreboard.default-visible", true)
        );
    }

    /**
     * @param featureId the feature id
     * @return whether the feature is switched on; features default to enabled
     */
    public boolean isFeatureEnabled(final String featureId) {
        return features.getOrDefault(featureId.toLowerCase(Locale.ROOT), true);
    }
}
