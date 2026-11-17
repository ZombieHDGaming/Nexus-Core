package com.zombiehdgaming.nexuscore.display;

import com.zombiehdgaming.nexuscore.condition.Condition;

/**
 * A configured variant of a display (one scoreboard board, one tab layout...).
 */
public interface DisplayProfile {

    String id();

    /** Higher wins when several profiles match. */
    int priority();

    Condition condition();

    /** How often the profile is re-rendered while shown, in ticks. */
    int refreshTicks();
}
