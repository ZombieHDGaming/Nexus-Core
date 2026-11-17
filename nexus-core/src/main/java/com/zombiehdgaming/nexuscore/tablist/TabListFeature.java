package com.zombiehdgaming.nexuscore.tablist;

import com.zombiehdgaming.nexuscore.api.tablist.TabListService;
import com.zombiehdgaming.nexuscore.condition.ConditionParser;
import com.zombiehdgaming.nexuscore.config.ConfigException;
import com.zombiehdgaming.nexuscore.config.YamlFiles;
import com.zombiehdgaming.nexuscore.display.DisplayFeature;
import com.zombiehdgaming.nexuscore.display.ProfileSet;
import com.zombiehdgaming.nexuscore.display.ProfileSettings;
import com.zombiehdgaming.nexuscore.text.Template;
import com.zombiehdgaming.nexuscore.text.TextService;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Per-player tab list header and footer configured in {@code tablist.yml}.
 */
public final class TabListFeature extends DisplayFeature<TabLayout, TabListFeature.State> implements TabListService {

    public static final String ID = "tablist";
    public static final String FILE = "tablist.yml";

    private final TextService text;
    private final ConditionParser conditions;

    /** What the client currently shows, to skip sending identical packets. */
    static final class State {
        @Nullable Component header;
        @Nullable Component footer;
    }

    public TabListFeature(final Plugin plugin, final Logger logger, final TextService text, final ConditionParser conditions) {
        super(plugin, logger);
        this.text = text;
        this.conditions = conditions;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    protected ProfileSet<TabLayout> loadProfiles() throws ConfigException {
        final YamlConfiguration yaml = YamlFiles.load(plugin, FILE, false);
        final ConfigurationSection layouts = yaml.getConfigurationSection("layouts");
        if (layouts == null) {
            throw new ConfigException(FILE + " has no 'layouts' section");
        }

        final List<TabLayout> loaded = new ArrayList<>();
        for (final String key : layouts.getKeys(false)) {
            final String path = FILE + " > layouts." + key;
            final ConfigurationSection section = layouts.getConfigurationSection(key);
            if (section == null) {
                logger.warn("{}: expected a section; skipped", path);
                continue;
            }
            final ProfileSettings settings = ProfileSettings.parse(key, section, path, conditions, logger);
            if (settings == null) {
                continue;
            }
            final List<Template> header = text.compileAll(ProfileSettings.lines(section, "header"));
            final List<Template> footer = text.compileAll(ProfileSettings.lines(section, "footer"));
            final List<Template> all = new ArrayList<>(header);
            all.addAll(footer);
            loaded.add(new TabLayout(
                    settings.id(),
                    settings.priority(),
                    settings.condition(),
                    text.effectiveInterval(all, settings.updateInterval()),
                    header,
                    footer
            ));
        }
        return new ProfileSet<>(loaded);
    }

    @Override
    protected State createState(final Player player) {
        return new State();
    }

    @Override
    protected void render(final Player player, final TabLayout layout, final State state) {
        final Component header = joinLines(text.renderAll(layout.header(), player));
        final Component footer = joinLines(text.renderAll(layout.footer(), player));
        if (!header.equals(state.header) || !footer.equals(state.footer)) {
            player.sendPlayerListHeaderAndFooter(header, footer);
            state.header = header;
            state.footer = footer;
        }
    }

    @Override
    protected void clear(final Player player, final State state) {
        player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
        state.header = null;
        state.footer = null;
    }

    private static Component joinLines(final List<Component> lines) {
        return switch (lines.size()) {
            case 0 -> Component.empty();
            case 1 -> lines.getFirst();
            default -> Component.join(net.kyori.adventure.text.JoinConfiguration.newlines(), lines);
        };
    }
}
