package com.zombiehdgaming.nexuscore.message;

import com.zombiehdgaming.nexuscore.config.ConfigException;
import com.zombiehdgaming.nexuscore.config.YamlFiles;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * User-facing chat messages from {@code messages.yml} (MiniMessage, with {@code <prefix>} available everywhere).
 * Missing keys fall back to the bundled defaults, so upgrading never produces blank messages.
 */
public final class Messages {

    public static final String FILE = "messages.yml";

    private final MiniMessage miniMessage;
    private volatile YamlConfiguration yaml;

    private Messages(final MiniMessage miniMessage, final YamlConfiguration yaml) {
        this.miniMessage = miniMessage;
        this.yaml = yaml;
    }

    public static Messages load(final Plugin plugin, final MiniMessage miniMessage) throws ConfigException {
        return new Messages(miniMessage, YamlFiles.load(plugin, FILE, true));
    }

    public static Messages bundled(final Plugin plugin, final MiniMessage miniMessage) {
        return new Messages(miniMessage, YamlFiles.bundled(plugin, FILE));
    }

    public void reload(final Plugin plugin) throws ConfigException {
        this.yaml = YamlFiles.load(plugin, FILE, true);
    }

    public Component get(final String key, final TagResolver... resolvers) {
        final YamlConfiguration current = yaml;
        final String raw = current.getString(key, "<red>Missing message: " + key);
        final Component prefix = miniMessage.deserialize(current.getString("prefix", ""));
        return miniMessage.deserialize(raw, TagResolver.resolver(
                TagResolver.resolver("prefix", Tag.selfClosingInserting(prefix)),
                TagResolver.resolver(resolvers)
        ));
    }

    public void send(final Audience audience, final String key, final TagResolver... resolvers) {
        audience.sendMessage(get(key, resolvers));
    }
}
