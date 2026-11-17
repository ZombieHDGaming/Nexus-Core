package com.zombiehdgaming.nexuscore.text;

import java.util.Set;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * A pre-compiled line of configurable text.
 *
 * <p>Compilation rewrites {@code %placeholder%} tokens into a MiniMessage tag once, at load time, so the hot
 * render path only runs MiniMessage. Templates with nothing dynamic in them are parsed eagerly and the
 * resulting component is reused for every player and every refresh.</p>
 *
 * @param source     the text exactly as written in the config (for diagnostics)
 * @param miniMessage the MiniMessage string to render
 * @param animations names of animations referenced directly by this template
 * @param staticValue the pre-rendered component when the template has no dynamic parts, otherwise {@code null}
 */
public record Template(
        String source,
        String miniMessage,
        Set<String> animations,
        @Nullable Component staticValue
) {

    public Template {
        animations = Set.copyOf(animations);
    }

    public boolean isDynamic() {
        return staticValue == null;
    }
}
