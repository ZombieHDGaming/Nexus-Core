package com.zombiehdgaming.nexuscore.scoreboard;

import com.zombiehdgaming.nexuscore.condition.Condition;
import com.zombiehdgaming.nexuscore.display.DisplayProfile;
import com.zombiehdgaming.nexuscore.text.Template;
import java.util.List;

/**
 * One configured sidebar board.
 *
 * @param scoreNumbers what to show on the right of lines without their own {@code score} text
 */
public record BoardProfile(
        String id,
        int priority,
        Condition condition,
        int refreshTicks,
        Template title,
        List<BoardLine> lines,
        ScoreNumbers scoreNumbers
) implements DisplayProfile {

    public BoardProfile {
        lines = List.copyOf(lines);
    }
}
