package com.zombiehdgaming.nexuscore.scoreboard;

import com.zombiehdgaming.nexuscore.condition.Condition;
import com.zombiehdgaming.nexuscore.display.DisplayProfile;
import com.zombiehdgaming.nexuscore.text.Template;
import java.util.List;

/**
 * One configured sidebar board.
 */
public record BoardProfile(
        String id,
        int priority,
        Condition condition,
        int refreshTicks,
        Template title,
        List<Template> lines
) implements DisplayProfile {

    public BoardProfile {
        lines = List.copyOf(lines);
    }
}
