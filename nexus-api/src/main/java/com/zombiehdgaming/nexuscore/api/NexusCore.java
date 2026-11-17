package com.zombiehdgaming.nexuscore.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Static entry point to the Nexus-Core API.
 *
 * <p>The implementation is registered with Bukkit's {@link org.bukkit.plugin.ServicesManager} when Nexus-Core
 * enables, so dependent plugins should declare a load-order dependency on {@code NexusCore} and call
 * {@link #get()} from their own {@code onEnable} (or later).</p>
 */
public final class NexusCore {

    private NexusCore() {
        throw new UnsupportedOperationException("Static accessor");
    }

    /**
     * Returns the active API instance.
     *
     * @return the API
     * @throws IllegalStateException if Nexus-Core is not installed or not yet enabled
     */
    public static NexusCoreApi get() {
        final RegisteredServiceProvider<NexusCoreApi> provider =
                Bukkit.getServicesManager().getRegistration(NexusCoreApi.class);
        if (provider == null) {
            throw new IllegalStateException("Nexus-Core is not enabled. Did you declare a dependency on it?");
        }
        return provider.getProvider();
    }

    /**
     * @return {@code true} if the API is currently available
     */
    public static boolean isAvailable() {
        return Bukkit.getServicesManager().isProvidedFor(NexusCoreApi.class);
    }
}
