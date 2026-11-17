package com.zombiehdgaming.nexuscore.feature;

import com.zombiehdgaming.nexuscore.config.ConfigException;
import com.zombiehdgaming.nexuscore.config.Settings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

/**
 * Owns all features and reconciles their running state with {@code config.yml}.
 */
public final class FeatureManager {

    private final Map<String, Feature> features = new LinkedHashMap<>();
    private final Logger logger;

    public FeatureManager(final Logger logger) {
        this.logger = logger;
    }

    public <F extends Feature> F register(final F feature) {
        if (features.putIfAbsent(feature.id(), feature) != null) {
            throw new IllegalStateException("Duplicate feature id " + feature.id());
        }
        return feature;
    }

    public List<Feature> features() {
        return List.copyOf(features.values());
    }

    /**
     * Brings every feature in line with the settings: enables, reloads or disables as needed. A failure in one
     * feature is logged and reported but never prevents the others from loading.
     *
     * @param settings current settings
     * @return human-readable failures, empty on full success
     */
    public List<String> apply(final Settings settings) {
        final List<String> failures = new ArrayList<>();
        for (final Feature feature : features.values()) {
            final boolean wanted = settings.isFeatureEnabled(feature.id());
            try {
                if (wanted && feature.isRunning()) {
                    feature.reload();
                } else if (wanted) {
                    feature.enable();
                    logger.info("Enabled feature '{}'", feature.id());
                } else if (feature.isRunning()) {
                    feature.disable();
                    logger.info("Disabled feature '{}'", feature.id());
                }
            } catch (final ConfigException e) {
                logger.error("Feature '{}' could not load: {}", feature.id(), e.getMessage());
                failures.add(feature.id() + ": " + e.getMessage());
            } catch (final RuntimeException e) {
                logger.error("Feature '{}' failed unexpectedly", feature.id(), e);
                failures.add(feature.id() + ": " + e);
            }
        }
        return failures;
    }

    public void disableAll() {
        // Reverse order so later features (which may build on earlier ones) shut down first.
        final List<Feature> ordered = new ArrayList<>(features.values());
        java.util.Collections.reverse(ordered);
        for (final Feature feature : ordered) {
            if (feature.isRunning()) {
                try {
                    feature.disable();
                } catch (final RuntimeException e) {
                    logger.error("Feature '{}' failed to disable cleanly", feature.id(), e);
                }
            }
        }
    }
}
