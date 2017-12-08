package com.polidea.rxandroidble.setup;


import com.polidea.rxandroidble.internal.operations.TimeoutConfiguration;

public class SetupConnection {

    public TimeoutConfiguration timeoutConfiguration;
    /* Ideas:
    Clear discovered services cache at connection time and/or after disconnect
    Suppress characteristic property validation
    Set defaults for SetupWrite/SetupNotification
    */
}
