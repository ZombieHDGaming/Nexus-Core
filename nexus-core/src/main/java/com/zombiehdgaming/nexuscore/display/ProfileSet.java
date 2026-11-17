package com.zombiehdgaming.nexuscore.display;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

/**
 * Immutable, priority-ordered collection of profiles. Replaced wholesale on reload.
 *
 * @param <P> profile type
 */
public final class ProfileSet<P extends DisplayProfile> {

    private final List<P> ordered;
    private final Map<String, P> byId;

    public ProfileSet(final List<P> profiles) {
        this.ordered = profiles.stream()
                .sorted(Comparator.comparingInt(DisplayProfile::priority).reversed()
                        .thenComparing(DisplayProfile::id))
                .toList();
        final Map<String, P> map = new LinkedHashMap<>();
        for (final P profile : ordered) {
            map.put(profile.id(), profile);
        }
        this.byId = Map.copyOf(map);
    }

    public static <P extends DisplayProfile> ProfileSet<P> empty() {
        return new ProfileSet<>(List.of());
    }

    /**
     * @param player the player
     * @return the highest-priority profile whose conditions match, ties broken by id for determinism
     */
    public @Nullable P select(final Player player) {
        for (final P profile : ordered) {
            if (profile.condition().test(player)) {
                return profile;
            }
        }
        return null;
    }

    public @Nullable P get(final String id) {
        return byId.get(id);
    }

    public Set<String> ids() {
        return byId.keySet();
    }

    public List<P> ordered() {
        return ordered;
    }

    public int size() {
        return ordered.size();
    }
}
