package com.zombiehdgaming.nexuscore.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.zombiehdgaming.nexuscore.NexusCorePlugin;
import com.zombiehdgaming.nexuscore.api.display.DisplayService;
import com.zombiehdgaming.nexuscore.message.Messages;
import com.zombiehdgaming.nexuscore.scoreboard.ScoreboardFeature;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Brigadier command tree.
 *
 * <pre>
 * /nexuscore reload
 * /nexuscore info
 * /nexuscore sidebar &lt;player&gt; &lt;board|auto&gt;
 * /nexuscore tablist &lt;player&gt; &lt;layout|auto&gt;
 * /sidebar [on|off|toggle]
 * </pre>
 */
public final class NexusCommands {

    public static final String PERM_ADMIN = "nexuscore.command.admin";
    public static final String PERM_SIDEBAR = "nexuscore.command.sidebar";
    private static final String AUTO = "auto";

    private final NexusCorePlugin plugin;

    public NexusCommands(final NexusCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void register(final Commands commands) {
        commands.register(adminTree(), "Nexus-Core administration", List.of("nexus"));
        commands.register(sidebarTree(), "Toggle your sidebar scoreboard", List.of("sb"));
    }

    private Messages messages() {
        return plugin.messages();
    }

    // ------------------------------------------------------------------ /nexuscore

    private LiteralCommandNode<CommandSourceStack> adminTree() {
        return Commands.literal("nexuscore")
                .requires(source -> source.getSender().hasPermission(PERM_ADMIN))
                .then(Commands.literal("reload").executes(this::reload))
                .then(Commands.literal("info").executes(this::info))
                .then(forceProfile("sidebar", plugin.scoreboard()))
                .then(forceProfile("tablist", plugin.tabList()))
                .executes(this::info)
                .build();
    }

    private int reload(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        final NexusCorePlugin.ReloadResult result = plugin.reload();
        if (result.failures().isEmpty()) {
            messages().send(sender, "reload-success",
                    Placeholder.unparsed("time", Long.toString(result.millis())));
        } else {
            messages().send(sender, "reload-partial",
                    Placeholder.unparsed("count", Integer.toString(result.failures().size())));
            result.failures().forEach(failure ->
                    messages().send(sender, "reload-failure-entry", Placeholder.unparsed("error", failure)));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int info(final CommandContext<CommandSourceStack> ctx) {
        messages().send(ctx.getSource().getSender(), "info",
                Placeholder.unparsed("version", plugin.getPluginMeta().getVersion()),
                Placeholder.unparsed("boards", describe(plugin.scoreboard())),
                Placeholder.unparsed("layouts", describe(plugin.tabList())),
                Placeholder.unparsed("papi", Boolean.toString(plugin.isPlaceholderApiHooked())));
        return Command.SINGLE_SUCCESS;
    }

    private static String describe(final DisplayService service) {
        if (!service.isEnabled()) {
            return "disabled";
        }
        return service.profileIds().isEmpty() ? "none" : String.join(", ", service.profileIds());
    }

    private LiteralArgumentBuilder<CommandSourceStack> forceProfile(final String name, final DisplayService service) {
        return Commands.literal(name)
                .then(Commands.argument("target", ArgumentTypes.player())
                        .then(Commands.argument("profile", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    final String remaining = builder.getRemainingLowerCase();
                                    service.profileIds().stream()
                                            .filter(id -> id.startsWith(remaining))
                                            .forEach(builder::suggest);
                                    if (AUTO.startsWith(remaining)) {
                                        builder.suggest(AUTO);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    final CommandSender sender = ctx.getSource().getSender();
                                    final Player target = ctx.getArgument("target", PlayerSelectorArgumentResolver.class)
                                            .resolve(ctx.getSource()).getFirst();
                                    final String profile = StringArgumentType.getString(ctx, "profile").toLowerCase(Locale.ROOT);
                                    if (!service.isEnabled()) {
                                        messages().send(sender, "feature-disabled", Placeholder.unparsed("feature", name));
                                        return 0;
                                    }
                                    if (!AUTO.equals(profile) && !service.profileIds().contains(profile)) {
                                        messages().send(sender, "unknown-profile", Placeholder.unparsed("profile", profile));
                                        return 0;
                                    }
                                    service.forceProfile(target, AUTO.equals(profile) ? null : profile);
                                    messages().send(sender, "profile-forced",
                                            Placeholder.unparsed("feature", name),
                                            Placeholder.unparsed("player", target.getName()),
                                            Placeholder.unparsed("profile", profile));
                                    return Command.SINGLE_SUCCESS;
                                })));
    }

    // ------------------------------------------------------------------ /sidebar

    private LiteralCommandNode<CommandSourceStack> sidebarTree() {
        return Commands.literal("sidebar")
                .requires(source -> source.getSender().hasPermission(PERM_SIDEBAR))
                .executes(ctx -> setSidebar(ctx, null))
                .then(Commands.literal("toggle").executes(ctx -> setSidebar(ctx, null)))
                .then(Commands.literal("on").executes(ctx -> setSidebar(ctx, true)))
                .then(Commands.literal("off").executes(ctx -> setSidebar(ctx, false)))
                .build();
    }

    private int setSidebar(final CommandContext<CommandSourceStack> ctx, final Boolean visible) throws CommandSyntaxException {
        final CommandSender sender = ctx.getSource().getSender();
        if (!(ctx.getSource().getExecutor() instanceof final Player player)) {
            messages().send(sender, "players-only");
            return 0;
        }
        final ScoreboardFeature scoreboard = plugin.scoreboard();
        if (!scoreboard.isEnabled()) {
            messages().send(sender, "feature-disabled", Placeholder.unparsed("feature", "sidebar"));
            return 0;
        }
        final boolean target = visible != null ? visible : !scoreboard.isVisible(player);
        scoreboard.setVisible(player, target);
        messages().send(player, target ? "sidebar-shown" : "sidebar-hidden");
        return Command.SINGLE_SUCCESS;
    }
}
