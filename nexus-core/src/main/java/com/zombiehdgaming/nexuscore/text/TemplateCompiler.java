package com.zombiehdgaming.nexuscore.text;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Turns raw config strings into {@link Template}s. Pure and side-effect free, so it is unit tested directly.
 */
public final class TemplateCompiler {

    /** Tag that resolves a placeholder token; also usable directly in configs as {@code <papi:'token'>}. */
    public static final String PLACEHOLDER_TAG = "papi";
    /** Tag that inserts the current frame of an animation: {@code <animation:name>}. */
    public static final String ANIMATION_TAG = "animation";

    /**
     * {@code %token%} where the token has no whitespace, angle brackets or percent signs. The restrictions keep
     * ordinary prose such as {@code "50% off, 100% fun"} from being treated as a placeholder.
     */
    private static final Pattern TOKEN = Pattern.compile("%([^%\\s<>]+)%");
    private static final Pattern ANIMATION_REF = Pattern.compile(
            "<" + ANIMATION_TAG + ":['\"]?([A-Za-z0-9_\\-]+)['\"]?>");

    private final MiniMessage miniMessage;

    public TemplateCompiler(final MiniMessage miniMessage) {
        this.miniMessage = miniMessage;
    }

    public Template compile(final String source) {
        final String rewritten = rewritePlaceholders(source);
        final Set<String> animations = findAnimations(rewritten);
        final boolean dynamic = !rewritten.equals(source)
                || !animations.isEmpty()
                || source.contains("<" + PLACEHOLDER_TAG + ":");
        return new Template(source, rewritten, animations, dynamic ? null : miniMessage.deserialize(source));
    }

    /**
     * Rewrites {@code %token%} into {@code <papi:'token'>}. Resolving placeholders through a tag (rather than
     * string substitution before parsing) means a value such as a player-chosen nickname containing
     * {@code <red>} is inserted as text and can never inject formatting or click events.
     */
    static String rewritePlaceholders(final String source) {
        if (source.indexOf('%') < 0) {
            return source;
        }
        final Matcher matcher = TOKEN.matcher(source);
        final StringBuilder out = new StringBuilder(source.length() + 16);
        while (matcher.find()) {
            final String token = matcher.group(1)
                    .replace("\\", "\\\\")
                    .replace("'", "\\'");
            matcher.appendReplacement(out, Matcher.quoteReplacement("<" + PLACEHOLDER_TAG + ":'" + token + "'>"));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    static Set<String> findAnimations(final String miniMessageText) {
        final Set<String> names = new LinkedHashSet<>();
        final Matcher matcher = ANIMATION_REF.matcher(miniMessageText);
        while (matcher.find()) {
            names.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }
        return names;
    }
}
