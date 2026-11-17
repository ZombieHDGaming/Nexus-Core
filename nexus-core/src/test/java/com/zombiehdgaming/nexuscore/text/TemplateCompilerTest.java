package com.zombiehdgaming.nexuscore.text;

import static org.assertj.core.api.Assertions.assertThat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.Test;

class TemplateCompilerTest {

    private final TemplateCompiler compiler = new TemplateCompiler(MiniMessage.miniMessage());

    @Test
    void rewritesPlaceholdersIntoTags() {
        assertThat(TemplateCompiler.rewritePlaceholders("<gray>Hi %player_name%, bal %vault_eco_balance%"))
                .isEqualTo("<gray>Hi <papi:'player_name'>, bal <papi:'vault_eco_balance'>");
    }

    @Test
    void leavesProseWithPercentSignsAlone() {
        final String prose = "50% off, 100% fun";
        assertThat(TemplateCompiler.rewritePlaceholders(prose)).isEqualTo(prose);
    }

    @Test
    void escapesQuotesInsideTokens() {
        assertThat(TemplateCompiler.rewritePlaceholders("%a'b%")).isEqualTo("<papi:'a\\'b'>");
    }

    @Test
    void staticTemplatesArePreRendered() {
        final Template template = compiler.compile("<red>Hello");
        assertThat(template.isDynamic()).isFalse();
        assertThat(template.staticValue()).isEqualTo(Component.text("Hello", NamedTextColor.RED));
    }

    @Test
    void placeholdersAndAnimationsMakeTemplatesDynamic() {
        assertThat(compiler.compile("%nexus_online%").isDynamic()).isTrue();
        assertThat(compiler.compile("<animation:title>").isDynamic()).isTrue();
        assertThat(compiler.compile("<papi:'x'>").isDynamic()).isTrue();
    }

    @Test
    void collectsReferencedAnimations() {
        final Template template = compiler.compile("<animation:Title> and <animation:'tips'> and <animation:\"x-y\">");
        assertThat(template.animations()).containsExactlyInAnyOrder("title", "tips", "x-y");
    }
}
