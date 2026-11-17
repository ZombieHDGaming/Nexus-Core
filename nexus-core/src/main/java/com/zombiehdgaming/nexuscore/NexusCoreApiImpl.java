package com.zombiehdgaming.nexuscore;

import com.zombiehdgaming.nexuscore.api.NexusCoreApi;
import com.zombiehdgaming.nexuscore.api.placeholder.PlaceholderRegistry;
import com.zombiehdgaming.nexuscore.api.scoreboard.ScoreboardService;
import com.zombiehdgaming.nexuscore.api.tablist.TabListService;

record NexusCoreApiImpl(
        ScoreboardService scoreboards,
        TabListService tabList,
        PlaceholderRegistry placeholders
) implements NexusCoreApi {
}
