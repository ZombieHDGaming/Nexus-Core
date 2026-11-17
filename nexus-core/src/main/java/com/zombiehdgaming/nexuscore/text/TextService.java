package com.zombiehdgaming.nexuscore.text;

import com.zombiehdgaming.nexuscore.config.ConfigException;
import com.zombiehdgaming.nexuscore.config.YamlFiles;
import com.zombiehdgaming.nexuscore.placeholder.PlaceholderRegistryImpl;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Compiles and renders configurable text for a specific player.
 *
 * <p>Rendering order for a template:</p>
 * <ol>
 *   <li>{@code %nexus_<key>%} is resolved from the internal {@link PlaceholderRegistryImpl} (keeps rich
 *       components, no PlaceholderAPI needed).</li>
 *   <li>Any other {@code %token%} goes to PlaceholderAPI when installed; legacy colour codes in the result are
 *       honoured.</li>
 *   <li>Unknown placeholders are left as literal text so typos are visible rather than silently blank.</li>
 * </ol>
 *
 * <p>Thread-safety: all state is immutable or swapped atomically, and rendering touches only the player
 * passed in, so render calls are safe from any player's owning thread concurrently (Folia).</p>
 */
public final class TextService {

    public static final String ANIMATIONS_FILE = "animations.yml";
    private static final Pattern NAME = Pattern.compile("[a-z0-9_\\-]+");
    /** Guards against animations that (directly or indirectly) contain themselves. */
    private static final int MAX_NESTING = 4;

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final TemplateCompiler compiler = new TemplateCompiler(miniMessage);
    private final PlaceholderRegistryImpl placeholders;
    private final LongSupplier ticks;
    private final Logger logger;
    private final Set<String> reportedFailures = ConcurrentHashMap.newKeySet();

    private volatile Map<String, Animation> animations = Map.of();
    private volatile @Nullable ExternalPlaceholders external;

    /**
     * @param placeholders internal placeholder registry
     * @param ticks        global tick source used to pick animation frames (see
     *                     {@link com.zombiehdgaming.nexuscore.scheduler.TickClock})
     * @param logger       logger
     */
    public TextService(final PlaceholderRegistryImpl placeholders, final LongSupplier ticks, final Logger logger) {
        this.placeholders = placeholders;
        this.ticks = ticks;
        this.logger = logger;
    }

    public MiniMessage miniMessage() {
        return miniMessage;
    }

    public void setExternalPlaceholders(final @Nullable ExternalPlaceholders external) {
        this.external = external;
    }

    public Template compile(final String source) {
        return compiler.compile(source);
    }

    public List<Template> compileAll(final Collection<String> sources) {
        final List<Template> out = new ArrayList<>(sources.size());
        for (final String source : sources) {
            out.add(compile(source));
        }
        return List.copyOf(out);
    }

    public Component render(final Template template, final Player player) {
        return render(template, player, 0);
    }

    public List<Component> renderAll(final List<Template> templates, final Player player) {
        final List<Component> out = new ArrayList<>(templates.size());
        for (final Template template : templates) {
            out.add(render(template, player, 0));
        }
        return out;
    }

    private Component render(final Template template, final Player player, final int depth) {
        final Component staticValue = template.staticValue();
        if (staticValue != null) {
            return staticValue;
        }
        try {
            return miniMessage.deserialize(template.miniMessage(), resolverFor(player, depth));
        } catch (final RuntimeException e) {
            if (reportedFailures.add(template.source())) {
                logger.warn("Failed to render text \"{}\": {}", template.source(), e.getMessage());
            }
            return Component.text(template.source());
        }
    }

    private TagResolver resolverFor(final Player player, final int depth) {
        return TagResolver.resolver(
                TagResolver.resolver(TemplateCompiler.PLACEHOLDER_TAG, (args, context) -> {
                    final String token = args.popOr("Expected a placeholder name").value();
                    return Tag.selfClosingInserting(resolvePlaceholder(player, token));
                }),
                TagResolver.resolver(TemplateCompiler.ANIMATION_TAG, (args, context) -> {
                    final String name = args.popOr("Expected an animation name").lowerValue();
                    return Tag.selfClosingInserting(renderAnimation(player, name, depth));
                })
        );
    }

    private Component resolvePlaceholder(final Player player, final String token) {
        final String lower = token.toLowerCase(Locale.ROOT);
        if (lower.startsWith(PlaceholderRegistryImpl.PREFIX)) {
            final Component internal = placeholders.resolve(player, lower.substring(PlaceholderRegistryImpl.PREFIX.length()));
            if (internal != null) {
                return internal;
            }
        }
        final ExternalPlaceholders papi = external;
        if (papi != null) {
            final String value = papi.resolve(player, token);
            if (value != null) {
                return LegacyText.parse(value);
            }
        }
        return Component.text('%' + token + '%');
    }

    private Component renderAnimation(final Player player, final String name, final int depth) {
        final Animation animation = animations.get(name);
        if (animation == null) {
            if (reportedFailures.add("animation:" + name)) {
                logger.warn("Unknown animation '{}' referenced in a template; check {}", name, ANIMATIONS_FILE);
            }
            return Component.empty();
        }
        if (depth >= MAX_NESTING) {
            if (reportedFailures.add("nesting:" + name)) {
                logger.warn("Animation '{}' is nested too deeply (does it reference itself?)", name);
            }
            return Component.empty();
        }
        return render(animation.frameAt(ticks.getAsLong()), player, depth + 1);
    }

    /**
     * Works out how often a set of templates must be re-rendered: the configured interval, or faster if an
     * animation they use (directly or through nested animations) changes frame more often.
     *
     * @param templates           the templates of a profile
     * @param configuredInterval  the profile's {@code update-interval}
     * @return the effective refresh period in ticks
     */
    public int effectiveInterval(final Collection<Template> templates, final int configuredInterval) {
        int interval = configuredInterval;
        final Map<String, Animation> snapshot = animations;
        final Set<String> visited = new HashSet<>();
        final Deque<String> pending = new ArrayDeque<>();
        templates.forEach(t -> pending.addAll(t.animations()));
        while (!pending.isEmpty()) {
            final String name = pending.pop();
            if (!visited.add(name)) {
                continue;
            }
            final Animation animation = snapshot.get(name);
            if (animation != null) {
                interval = Math.min(interval, animation.intervalTicks());
                animation.frames().forEach(frame -> pending.addAll(frame.animations()));
            }
        }
        return Math.max(1, interval);
    }

    void setAnimations(final Map<String, Animation> loaded) {
        animations = Map.copyOf(loaded);
        reportedFailures.clear();
    }

    public Set<String> animationNames() {
        return animations.keySet();
    }

    /**
     * (Re)loads {@code animations.yml}. The previous animations stay active if the file is invalid.
     *
     * @param plugin the owning plugin
     * @throws ConfigException if the file cannot be parsed
     */
    public void loadAnimations(final Plugin plugin) throws ConfigException {
        final YamlConfiguration yaml = YamlFiles.load(plugin, ANIMATIONS_FILE, false);
        final ConfigurationSection root = yaml.getConfigurationSection("animations");
        final Map<String, Animation> loaded = new LinkedHashMap<>();
        if (root != null) {
            for (final String key : root.getKeys(false)) {
                final String name = key.toLowerCase(Locale.ROOT);
                final String path = ANIMATIONS_FILE + " > animations." + key;
                if (!NAME.matcher(name).matches()) {
                    logger.warn("{}: name may only contain a-z, 0-9, '_' and '-'; skipped", path);
                    continue;
                }
                final ConfigurationSection section = root.getConfigurationSection(key);
                if (section == null) {
                    logger.warn("{}: expected a section with 'interval' and 'frames'; skipped", path);
                    continue;
                }
                final List<String> frames = section.getStringList("frames");
                if (frames.isEmpty()) {
                    logger.warn("{}: has no frames; skipped", path);
                    continue;
                }
                final int interval = section.getInt("interval", 20);
                if (interval < 1) {
                    logger.warn("{}: interval must be at least 1 tick; skipped", path);
                    continue;
                }
                loaded.put(name, new Animation(name, interval, compileAll(frames)));
            }
        }
        setAnimations(loaded);
        logger.info("Loaded {} animation(s)", loaded.size());
    }
}
