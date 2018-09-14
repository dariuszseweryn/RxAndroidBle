package com.polidea.rxandroidble2;

public enum NotificationSetupMode {
    /**
     * Configures notifications according to the standard. The `Observable<byte[]>` is emitted after both the system notification is set
     * and the CLIENT_CHARACTERISTIC_CONFIG descriptor was written so the notification is fully set up. If a device starts to notify
     * right after CLIENT_CHARACTERISTIC_CONFIG is written then some early notifications may be lost — see {@link #QUICK_SETUP}
     */
    DEFAULT,
    /**
     * Compatibility mode for devices that do not contain CLIENT_CHARACTERISTIC_CONFIG
     */
    COMPAT,
    /**
     * Configures notifications according to the standard but in contrast to the {@link #DEFAULT} mode the `Observable<byte[]>` is emitted
     * before the CLIENT_CHARACTERISTIC_CONFIG is written. The CLIENT_CHARACTERISTIC_CONFIG is scheduled for write when the emitted
     * `Observable<byte[]>` is subscribed for the first time and any potential error connected with the descriptor write will be emitted
     * on the parent Observable<Observable<byte[]>> as in {@link #DEFAULT} case and `Observable<byte[]>` will complete. This mode may be
     * useful for devices that start to notify right after CLIENT_CHARACTERISTIC_CONFIG write
     */
    QUICK_SETUP
}
