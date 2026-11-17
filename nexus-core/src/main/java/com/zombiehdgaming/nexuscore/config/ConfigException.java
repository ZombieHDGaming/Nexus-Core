package com.zombiehdgaming.nexuscore.config;

import java.io.Serial;

/**
 * Raised when a configuration file cannot be read or is structurally invalid. Callers are expected to keep the
 * previously loaded state when this is thrown, so a typo during {@code /nexuscore reload} never takes a feature
 * offline.
 */
public final class ConfigException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConfigException(final String message) {
        super(message);
    }

    public ConfigException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
