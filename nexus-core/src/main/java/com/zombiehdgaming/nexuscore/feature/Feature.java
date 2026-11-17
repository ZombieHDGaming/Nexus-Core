package com.zombiehdgaming.nexuscore.feature;

import com.zombiehdgaming.nexuscore.config.ConfigException;

/**
 * A self-contained, independently switchable piece of functionality (sidebar, tab list, and future modules
 * such as chat formatting or join messages).
 *
 * <p>Lifecycle is driven by {@link FeatureManager}: {@code enable} when switched on, {@code reload} on
 * {@code /nexuscore reload} while running, {@code disable} when switched off or on shutdown. Implementations
 * must leave their previous state intact if {@code enable}/{@code reload} throws.</p>
 */
public interface Feature {

    /**
     * @return stable id, also the key under {@code features:} in {@code config.yml}
     */
    String id();

    boolean isRunning();

    void enable() throws ConfigException;

    void reload() throws ConfigException;

    void disable();
}
