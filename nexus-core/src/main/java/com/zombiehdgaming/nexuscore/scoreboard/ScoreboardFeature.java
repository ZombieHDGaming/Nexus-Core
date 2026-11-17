package com.zombiehdgaming.nexuscore.scoreboard;

import com.zombiehdgaming.nexuscore.api.scoreboard.ScoreboardService;
import com.zombiehdgaming.nexuscore.condition.ConditionParser;
import com.zombiehdgaming.nexuscore.config.ConfigException;
import com.zombiehdgaming.nexuscore.config.Settings;
import com.zombiehdgaming.nexuscore.config.YamlFiles;
import com.zombiehdgaming.nexuscore.display.DisplayFeature;
import com.zombiehdgaming.nexuscore.display.ProfileSet;
import com.zombiehdgaming.nexuscore.display.ProfileSettings;
import com.zombiehdgaming.nexuscore.scheduler.PlayerTasks;
import com.zombiehdgaming.nexuscore.scoreboard.packet.SidebarPacketSender;
import com.zombiehdgaming.nexuscore.text.Template;
import com.zombiehdgaming.nexuscore.text.TextService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

/**
 * Per-player sidebar scoreboards configured in {@code scoreboards.yml}.
 */
public final class ScoreboardFeature extends DisplayFeature<BoardProfile, Sidebar> implements ScoreboardService {

    public static final String ID = "scoreboard";
    public static final String FILE = "scoreboards.yml";

    private final TextService text;
    private final ConditionParser conditions;
    private final SidebarPacketSender sender;
    private final Supplier<Settings> settings;
    /** Stored on the player, so the preference survives restarts with no database. */
    private final NamespacedKey visibilityKey;

    public ScoreboardFeature(
            final Plugin plugin,
            final Logger logger,
            final TextService text,
            final ConditionParser conditions,
            final SidebarPacketSender sender,
            final Supplier<Settings> settings
    ) {
        super(plugin, logger);
        this.text = text;
        this.conditions = conditions;
        this.sender = sender;
        this.settings = settings;
        this.visibilityKey = new NamespacedKey(plugin, "sidebar_visible");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    protected ProfileSet<BoardProfile> loadProfiles() throws ConfigException {
        final YamlConfiguration yaml = YamlFiles.load(plugin, FILE, false);
        final ConfigurationSection boards = yaml.getConfigurationSection("boards");
        if (boards == null) {
            throw new ConfigException(FILE + " has no 'boards' section");
        }

        final List<BoardProfile> loaded = new ArrayList<>();
        for (final String key : boards.getKeys(false)) {
            final String path = FILE + " > boards." + key;
            final ConfigurationSection section = boards.getConfigurationSection(key);
            if (section == null) {
                logger.warn("{}: expected a section; skipped", path);
                continue;
            }
            final ProfileSettings settings = ProfileSettings.parse(key, section, path, conditions, logger);
            if (settings == null) {
                continue;
            }

            final Template title = text.compile(section.getString("title", ""));
            final List<BoardLine> lines = BoardLines.parse(section.getList("lines", List.of()), path, text, logger);
            final ScoreNumbers scoreNumbers = scoreNumbers(section, path);

            final List<Template> all = BoardLines.templates(lines);
            all.add(title);
            loaded.add(new BoardProfile(
                    settings.id(),
                    settings.priority(),
                    settings.condition(),
                    text.effectiveInterval(all, settings.updateInterval()),
                    title,
                    lines,
                    scoreNumbers
            ));
        }
        if (loaded.isEmpty()) {
            logger.warn("{} defines no usable boards; nobody will see a sidebar", FILE);
        }
        return new ProfileSet<>(loaded);
    }

    private ScoreNumbers scoreNumbers(final ConfigurationSection section, final String path) {
        final ScoreNumbers fallback = settings.get().defaultScoreNumbers();
        final String raw = section.getString("score-numbers");
        if (raw == null) {
            return fallback;
        }
        final ScoreNumbers parsed = ScoreNumbers.parse(raw);
        if (parsed == null) {
            logger.warn("{}.score-numbers: '{}' is not 'hidden' or 'shown'; using {}",
                    path, raw, fallback.name().toLowerCase(java.util.Locale.ROOT));
            return fallback;
        }
        return parsed;
    }

    @Override
    protected Sidebar createState(final Player player) {
        return new Sidebar(player, sender);
    }

    @Override
    protected void render(final Player player, final BoardProfile profile, final Sidebar sidebar) {
        sidebar.render(text.render(profile.title(), player), BoardLines.render(profile, player, text));
    }

    @Override
    protected void clear(final Player player, final Sidebar sidebar) {
        sidebar.hide();
    }

    @Override
    protected boolean shouldDisplay(final Player player) {
        return isVisible(player);
    }

    // ------------------------------------------------------------------ ScoreboardService

    @Override
    public boolean isVisible(final Player player) {
        final PersistentDataContainer data = player.getPersistentDataContainer();
        final Boolean stored = data.get(visibilityKey, PersistentDataType.BOOLEAN);
        return stored != null ? stored : settings.get().sidebarDefaultVisible();
    }

    @Override
    public void setVisible(final Player player, final boolean visible) {
        PlayerTasks.run(plugin, player, () -> {
            player.getPersistentDataContainer().set(visibilityKey, PersistentDataType.BOOLEAN, visible);
            refresh(player);
        });
    }
}
