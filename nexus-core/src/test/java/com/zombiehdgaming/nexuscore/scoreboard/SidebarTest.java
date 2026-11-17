package com.zombiehdgaming.nexuscore.scoreboard;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.zombiehdgaming.nexuscore.scoreboard.packet.SidebarPacketSender;
import java.util.List;
import java.util.stream.IntStream;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class SidebarTest {

    private final Player player = mock(Player.class);
    private SidebarPacketSender sender;
    private Sidebar sidebar;

    @BeforeEach
    void setUp() {
        sender = mock(SidebarPacketSender.class);
        sidebar = new Sidebar(player, sender);
    }

    private static List<SidebarLine> lines(final String... text) {
        return java.util.Arrays.stream(text).map(t -> SidebarLine.of(Component.text(t))).toList();
    }

    @Test
    void firstRenderCreatesObjectiveThenLinesTopDown() {
        sidebar.render(Component.text("T"), lines("a", "b"));

        final InOrder order = inOrder(sender);
        order.verify(sender).create(player, Component.text("T"));
        order.verify(sender).setLine(player, "nx-0", 15, Component.text("a"), null);
        order.verify(sender).setLine(player, "nx-1", 14, Component.text("b"), null);
        verifyNoMoreInteractions(sender);
    }

    @Test
    void identicalRenderSendsNothing() {
        sidebar.render(Component.text("T"), lines("a", "b"));
        clearInvocations(sender);

        sidebar.render(Component.text("T"), lines("a", "b"));

        verifyNoInteractions(sender);
    }

    @Test
    void onlyChangedLinesAreSent() {
        sidebar.render(Component.text("T"), lines("a", "b", "c"));
        clearInvocations(sender);

        sidebar.render(Component.text("T"), lines("a", "B", "c"));

        verify(sender).setLine(player, "nx-1", 14, Component.text("B"), null);
        verifyNoMoreInteractions(sender);
    }

    @Test
    void titleChangeUpdatesInPlace() {
        sidebar.render(Component.text("T"), lines("a"));
        clearInvocations(sender);

        sidebar.render(Component.text("T2"), lines("a"));

        verify(sender).updateTitle(player, Component.text("T2"));
        verify(sender, never()).create(any(), any());
        verifyNoMoreInteractions(sender);
    }

    @Test
    void shrinkingRemovesOnlyTrailingLines() {
        sidebar.render(Component.text("T"), lines("a", "b", "c"));
        clearInvocations(sender);

        sidebar.render(Component.text("T"), lines("a"));

        verify(sender).removeLine(player, "nx-1");
        verify(sender).removeLine(player, "nx-2");
        verifyNoMoreInteractions(sender);
    }

    @Test
    void neverSendsMoreThanFifteenLines() {
        final List<SidebarLine> twenty = IntStream.range(0, 20).mapToObj(i -> SidebarLine.of(Component.text(i))).toList();

        sidebar.render(Component.text("T"), twenty);

        verify(sender, org.mockito.Mockito.times(Sidebar.MAX_LINES)).setLine(any(), anyString(), anyInt(), any(), any());
    }

    @Test
    void hideRemovesObjectiveAndNextRenderRecreates() {
        sidebar.render(Component.text("T"), lines("a"));
        sidebar.hide();
        verify(sender).remove(player);
        clearInvocations(sender);

        sidebar.render(Component.text("T"), lines("a"));

        verify(sender).create(player, Component.text("T"));
        verify(sender).setLine(player, "nx-0", 15, Component.text("a"), null);
    }

    @Test
    void scoreTextIsSentAndChangesTriggerResend() {
        sidebar.render(Component.text("T"), List.of(new SidebarLine(Component.text("Ping"), Component.text("20ms"))));
        verify(sender).setLine(player, "nx-0", 15, Component.text("Ping"), Component.text("20ms"));
        clearInvocations(sender);

        sidebar.render(Component.text("T"), List.of(new SidebarLine(Component.text("Ping"), Component.text("20ms"))));
        verifyNoInteractions(sender);

        sidebar.render(Component.text("T"), List.of(new SidebarLine(Component.text("Ping"), Component.text("35ms"))));
        verify(sender).setLine(player, "nx-0", 15, Component.text("Ping"), Component.text("35ms"));

        sidebar.render(Component.text("T"), List.of(SidebarLine.of(Component.text("Ping"))));
        verify(sender).setLine(player, "nx-0", 15, Component.text("Ping"), null);
        verifyNoMoreInteractions(sender);
    }

    @Test
    void hideWhenNotShownIsNoOp() {
        sidebar.hide();
        verifyNoInteractions(sender);
    }
}
