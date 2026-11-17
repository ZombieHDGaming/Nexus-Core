package com.zombiehdgaming.nexuscore.scoreboard;

import com.zombiehdgaming.nexuscore.text.Template;
import org.jspecify.annotations.Nullable;

/**
 * A configured sidebar line.
 *
 * @param text  left-aligned text
 * @param score optional right-aligned text replacing the score number
 */
public record BoardLine(Template text, @Nullable Template score) {
}
