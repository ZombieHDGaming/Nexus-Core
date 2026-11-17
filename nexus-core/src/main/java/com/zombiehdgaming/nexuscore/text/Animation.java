package com.zombiehdgaming.nexuscore.text;

import java.util.List;

/**
 * A named, looping sequence of frames.
 *
 * @param name          lower-case name used in {@code <animation:name>}
 * @param intervalTicks ticks each frame is shown for
 * @param frames        the frames, never empty
 */
public record Animation(String name, int intervalTicks, List<Template> frames) {

    public Animation {
        if (intervalTicks < 1) {
            throw new IllegalArgumentException("interval must be >= 1");
        }
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("an animation needs at least one frame");
        }
        frames = List.copyOf(frames);
    }

    /**
     * @param tick the global tick (see {@link com.zombiehdgaming.nexuscore.scheduler.TickClock})
     * @return the frame visible at that tick
     */
    public Template frameAt(final long tick) {
        return frames.get((int) Math.floorMod(tick / intervalTicks, (long) frames.size()));
    }
}
