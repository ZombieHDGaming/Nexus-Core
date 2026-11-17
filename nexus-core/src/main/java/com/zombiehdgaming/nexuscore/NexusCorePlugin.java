package com.zombiehdgaming.nexuscore;

import com.zombiehdgaming.nexuscore.api.NexusCoreApi;
import com.zombiehdgaming.nexuscore.command.NexusCommands;
import com.zombiehdgaming.nexuscore.condition.ConditionParser;
import com.zombiehdgaming.nexuscore.config.ConfigException;
import com.zombiehdgaming.nexuscore.config.Settings;
import com.zombiehdgaming.nexuscore.feature.FeatureManager;
import com.zombiehdgaming.nexuscore.message.Messages;
import com.zombiehdgaming.nexuscore.placeholder.BuiltinPlaceholders;
import com.zombiehdgaming.nexuscore.placeholder.PlaceholderApiHook;
import com.zombiehdgaming.nexuscore.placeholder.PlaceholderRegistryImpl;
import com.zombiehdgaming.nexuscore.scheduler.TickClock;
import com.zombiehdgaming.nexuscore.scoreboard.ScoreboardFeature;
import com.zombiehdgaming.nexuscore.scoreboard.packet.NmsSidebarPacketSender;
import com.zombiehdgaming.nexuscore.tablist.TabListFeature;
import com.zombiehdgaming.nexuscore.text.TextService;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Plugin entry point. Wires services together and owns the reload flow; all real behaviour lives in features.
 */
public final class NexusCorePlugin extends JavaPlugin implements Listener {

    /**
     * Outcome of {@link #reload()}.
     *
     * @param failures human-readable problems; empty on success
     * @param millis   time taken
     */
    public record ReloadResult(List<String> failures, long millis) {
    }

    private final Logger logger = getSLF4JLogger();
    private final TickClock clock = new TickClock();
    private final FeatureManager features = new FeatureManager(logger);

    private volatile Settings settings;
    private Messages messages;
    private PlaceholderRegistryImpl placeholders;
    private TextService text;
    private ScoreboardFeature scoreboard;
    private TabListFeature tabList;
    private @Nullable PlaceholderApiHook placeholderApi;

    @Override
    public void onEnable() {
        settings = loadSettingsOrDefaults();

        placeholders = new PlaceholderRegistryImpl(logger);
        BuiltinPlaceholders.registerAll(this, placeholders);

        text = new TextService(placeholders, clock::currentTick, logger);
        messages = loadMessagesOrDefaults();
        clock.start(this);
        hookPlaceholderApi();

        try {
            text.loadAnimations(this);
        } catch (final ConfigException e) {
            logger.error("{} - animations disabled until fixed and reloaded", e.getMessage());
        }

        final ConditionParser conditions = new ConditionParser(logger);
        scoreboard = features.register(new ScoreboardFeature(
                this, logger, text, conditions, new NmsSidebarPacketSender(), () -> settings));
        tabList = features.register(new TabListFeature(this, logger, text, conditions));
        features.apply(settings);

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getServicesManager().register(
                NexusCoreApi.class, new NexusCoreApiImpl(scoreboard, tabList, placeholders), this, ServicePriority.Normal);

        final NexusCommands commands = new NexusCommands(this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> commands.register(event.registrar()));
    }

    @Override
    public void onDisable() {
        Bukkit.getServicesManager().unregisterAll(this);
        features.disableAll();
        if (placeholderApi != null) {
            placeholderApi.unregisterExpansion();
            placeholderApi = null;
        }
        clock.stop();
    }

    /**
     * Re-reads every configuration file. Each part keeps its previous state if its file is broken, so a reload
     * can never leave the server worse off than before.
     */
    public ReloadResult reload() {
        final long start = System.nanoTime();
        final List<String> failures = new ArrayList<>();

        try {
            settings = Settings.load(this, logger);
        } catch (final ConfigException e) {
            failures.add(e.getMessage());
            logger.error("{} - keeping previous settings", e.getMessage());
        }
        try {
            messages.reload(this);
        } catch (final ConfigException e) {
            failures.add(e.getMessage());
            logger.error("{} - keeping previous messages", e.getMessage());
        }
        try {
            text.loadAnimations(this);
        } catch (final ConfigException e) {
            failures.add(e.getMessage());
            logger.error("{} - keeping previous animations", e.getMessage());
        }
        applyExpansionSetting();
        failures.addAll(features.apply(settings));

        return new ReloadResult(List.copyOf(failures), (System.nanoTime() - start) / 1_000_000L);
    }

    @EventHandler
    public void onPluginDisable(final PluginDisableEvent event) {
        if (event.getPlugin() != this) {
            placeholders.unregisterAll(event.getPlugin());
        }
    }

    private void hookPlaceholderApi() {
        if (!Bukkit.getPluginManager().isPluginEnabled(PlaceholderApiHook.PLUGIN_NAME)) {
            logger.info("PlaceholderAPI not found; only %nexus_*% placeholders will be available");
            return;
        }
        placeholderApi = new PlaceholderApiHook(this, placeholders);
        text.setExternalPlaceholders(placeholderApi);
        applyExpansionSetting();
        logger.info("Hooked into PlaceholderAPI");
    }

    private void applyExpansionSetting() {
        final PlaceholderApiHook hook = placeholderApi;
        if (hook == null) {
            return;
        }
        if (settings.registerExpansion()) {
            hook.registerExpansion();
        } else {
            hook.unregisterExpansion();
        }
    }

    private Settings loadSettingsOrDefaults() {
        try {
            return Settings.load(this, logger);
        } catch (final ConfigException e) {
            logger.error("{} - starting with bundled defaults until it is fixed", e.getMessage());
            return Settings.bundledDefaults(this, logger);
        }
    }

    private Messages loadMessagesOrDefaults() {
        try {
            return Messages.load(this, text.miniMessage());
        } catch (final ConfigException e) {
            logger.error("{} - using bundled messages until it is fixed", e.getMessage());
            return Messages.bundled(this, text.miniMessage());
        }
    }

    // ------------------------------------------------------------------ accessors for commands

    public Messages messages() {
        return messages;
    }

    public ScoreboardFeature scoreboard() {
        return scoreboard;
    }

    public TabListFeature tabList() {
        return tabList;
    }

    public boolean isPlaceholderApiHooked() {
        return placeholderApi != null;
    }
}
