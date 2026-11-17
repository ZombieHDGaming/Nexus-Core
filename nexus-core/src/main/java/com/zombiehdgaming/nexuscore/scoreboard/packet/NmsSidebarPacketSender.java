package com.zombiehdgaming.nexuscore.scoreboard.packet;

import io.papermc.paper.adventure.PaperAdventure;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

/**
 * Sends sidebar packets directly. The objective exists only on the client; the server-side scoreboards are
 * never touched, so this coexists with plugins that manage teams/objectives on the main scoreboard and works
 * on Folia, where the Bukkit per-player scoreboard API is unsupported.
 *
 * <p>Lines use per-score display overrides (1.20.3+), so each line is a full component with no 40-character
 * team prefix/suffix limits. The objective uses {@link BlankFormat} so nothing shows on the right by default;
 * lines that want right-aligned text (custom text or visible numbers) carry their own {@link FixedFormat}.</p>
 */
public final class NmsSidebarPacketSender implements SidebarPacketSender {

    private static final String OBJECTIVE_NAME = "nexus_sidebar";
    /** Detached scoreboard used only to construct packet payloads; never ticked or sent to anyone. */
    private static final Scoreboard DETACHED = new Scoreboard();

    @Override
    public void create(final Player player, final Component title) {
        final Objective objective = objective(title);
        send(player, new ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_ADD));
        send(player, new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, objective));
    }

    @Override
    public void updateTitle(final Player player, final Component title) {
        send(player, new ClientboundSetObjectivePacket(objective(title), ClientboundSetObjectivePacket.METHOD_CHANGE));
    }

    @Override
    public void remove(final Player player) {
        send(player, new ClientboundSetObjectivePacket(objective(Component.empty()), ClientboundSetObjectivePacket.METHOD_REMOVE));
    }

    @Override
    public void setLine(final Player player, final String owner, final int score, final Component text,
                        final @Nullable Component scoreText) {
        // A per-score FixedFormat overrides the objective's BlankFormat for just this line.
        final Optional<NumberFormat> format = scoreText == null
                ? Optional.empty()
                : Optional.of(new FixedFormat(PaperAdventure.asVanilla(scoreText)));
        send(player, new ClientboundSetScorePacket(
                owner, OBJECTIVE_NAME, score, Optional.of(PaperAdventure.asVanilla(text)), format));
    }

    @Override
    public void removeLine(final Player player, final String owner) {
        send(player, new ClientboundResetScorePacket(owner, OBJECTIVE_NAME));
    }

    private static Objective objective(final Component title) {
        return new Objective(
                DETACHED,
                OBJECTIVE_NAME,
                ObjectiveCriteria.DUMMY,
                PaperAdventure.asVanilla(title),
                ObjectiveCriteria.RenderType.INTEGER,
                false,
                BlankFormat.INSTANCE
        );
    }

    private static void send(final Player player, final Packet<?> packet) {
        final ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
        if (connection != null) {
            connection.send(packet);
        }
    }
}
