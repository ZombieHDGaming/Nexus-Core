package com.zombiehdgaming.nexuscore.api;

import com.zombiehdgaming.nexuscore.api.placeholder.PlaceholderRegistry;
import com.zombiehdgaming.nexuscore.api.scoreboard.ScoreboardService;
import com.zombiehdgaming.nexuscore.api.tablist.TabListService;

/**
 * Root of the Nexus-Core API. Obtain via {@link NexusCore#get()}.
 *
 * <p>Services are always non-null. When the backing feature is disabled in the configuration the service
 * reports {@code isEnabled() == false} and its mutating operations become no-ops, so callers never need to
 * null-check.</p>
 */
public interface NexusCoreApi {

    /**
     * @return the per-player sidebar scoreboard service
     */
    ScoreboardService scoreboards();

    /**
     * @return the tab list header/footer service
     */
    TabListService tabList();

    /**
     * @return the registry for {@code %nexus_<key>%} placeholders
     */
    PlaceholderRegistry placeholders();
}
