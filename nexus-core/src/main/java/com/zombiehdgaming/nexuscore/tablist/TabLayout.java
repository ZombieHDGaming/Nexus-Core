package com.zombiehdgaming.nexuscore.tablist;

import com.zombiehdgaming.nexuscore.condition.Condition;
import com.zombiehdgaming.nexuscore.display.DisplayProfile;
import com.zombiehdgaming.nexuscore.text.Template;
import java.util.List;

/**
 * One configured tab list header/footer layout.
 */
public record TabLayout(
        String id,
        int priority,
        Condition condition,
        int refreshTicks,
        List<Template> header,
        List<Template> footer
) implements DisplayProfile {

    public TabLayout {
        header = List.copyOf(header);
        footer = List.copyOf(footer);
    }
}
