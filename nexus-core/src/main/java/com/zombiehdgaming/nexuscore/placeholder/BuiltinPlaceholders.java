package com.zombiehdgaming.nexuscore.placeholder;

import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Placeholders that ship with Nexus-Core so a fresh install looks right without PlaceholderAPI.
 */
public final class BuiltinPlaceholders {

    private BuiltinPlaceholders() {
    }

    public static void registerAll(final Plugin plugin, final PlaceholderRegistryImpl registry) {
        registry.register(plugin, "player", player -> Component.text(player.getName()));
        registry.register(plugin, "displayname", Player::displayName);
        registry.register(plugin, "uuid", player -> Component.text(player.getUniqueId().toString()));
        registry.register(plugin, "world", player -> Component.text(player.getWorld().getName()));
        registry.register(plugin, "online", player -> Component.text(Bukkit.getOnlinePlayers().size()));
        registry.register(plugin, "max_players", player -> Component.text(Bukkit.getMaxPlayers()));
        registry.register(plugin, "ping", player -> Component.text(player.getPing()));
        registry.register(plugin, "x", player -> Component.text(player.getLocation().getBlockX()));
        registry.register(plugin, "y", player -> Component.text(player.getLocation().getBlockY()));
        registry.register(plugin, "z", player -> Component.text(player.getLocation().getBlockZ()));
        registry.register(plugin, "health", player -> Component.text(oneDecimal(player.getHealth())));
        registry.register(plugin, "food", player -> Component.text(player.getFoodLevel()));
        registry.register(plugin, "level", player -> Component.text(player.getLevel()));
        registry.register(plugin, "gamemode",
                player -> Component.text(player.getGameMode().name().toLowerCase(Locale.ROOT)));
    }

    private static String oneDecimal(final double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
