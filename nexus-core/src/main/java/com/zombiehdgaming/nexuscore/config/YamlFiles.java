package com.zombiehdgaming.nexuscore.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Loads YAML files from the plugin's data folder, copying the bundled default the first time.
 */
public final class YamlFiles {

    private YamlFiles() {
    }

    /**
     * Loads a configuration file.
     *
     * @param plugin       the owning plugin
     * @param name         file name relative to the data folder (and to the jar root for the default)
     * @param withDefaults when {@code true}, keys missing from the user's file fall back to the bundled values.
     *                     Use for settings-style files; leave {@code false} for user content (boards, layouts)
     *                     where merging defaults would resurrect entries the admin deleted.
     * @return the loaded configuration
     * @throws ConfigException if the file cannot be read or is not valid YAML
     */
    public static YamlConfiguration load(final Plugin plugin, final String name, final boolean withDefaults)
            throws ConfigException {
        final Path file = plugin.getDataFolder().toPath().resolve(name);
        if (Files.notExists(file)) {
            plugin.saveResource(name, false);
        }

        final YamlConfiguration config = new YamlConfiguration();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            config.load(reader);
        } catch (final IOException e) {
            throw new ConfigException("Could not read " + name + ": " + e.getMessage(), e);
        } catch (final InvalidConfigurationException e) {
            throw new ConfigException(name + " is not valid YAML: " + firstLine(e.getMessage()), e);
        }

        if (withDefaults) {
            config.setDefaults(bundled(plugin, name));
        }
        return config;
    }

    /**
     * Loads the copy of a file bundled inside the plugin jar.
     *
     * @param plugin the owning plugin
     * @param name   resource name
     * @return the bundled configuration (empty if the resource is missing)
     */
    public static YamlConfiguration bundled(final Plugin plugin, final String name) {
        final YamlConfiguration config = new YamlConfiguration();
        try (InputStream in = plugin.getResource(name)) {
            if (in != null) {
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    config.load(reader);
                }
            }
        } catch (final IOException | InvalidConfigurationException e) {
            // Our own bundled file is broken: a build problem, not a user problem.
            throw new IllegalStateException("Bundled resource " + name + " is invalid", e);
        }
        return config;
    }

    private static String firstLine(final String message) {
        if (message == null) {
            return "unknown error";
        }
        final int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }
}
