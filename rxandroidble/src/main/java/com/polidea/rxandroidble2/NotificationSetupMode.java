package com.polidea.rxandroidble2;

public enum NotificationSetupMode {
    /**
     * Configures notifications according to the standard. First by enabling the notificaiton for a selected characteristic, then
     * setting up the descriptor to enable notifications.
     */
    DEFAULT,
    /**
     * Compatibility mode for some devices that does not contain CLIENT_CHARACTERISTIC_CONFIG
     */
    COMPAT
}
