package com.polidea.rxandroidble2.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class BleDisconnectedExceptionTest {

    @SuppressWarnings("deprecation")
    @Test
    public void toStringShouldContainMessageWithUnknownStatus() {
        // given
        BleDisconnectedException out = new BleDisconnectedException("myBluetoothAddress");

        // expect
        assertEquals(out.toString(), "com.polidea.rxandroidble2.exceptions.BleDisconnectedException: Disconnected from MAC='XX:XX:XX:XX:XX:XX' with status -1 (UNKNOWN)");
    }

    @Test
    public void toStringShouldContainMessageWithStatus() {
        // given
        int expectedStatus = 129; // 0x81
        BleDisconnectedException out = new BleDisconnectedException("myBluetoothAddress", expectedStatus);

        // expect
        assertEquals(out.toString(), "com.polidea.rxandroidble2.exceptions.BleDisconnectedException: Disconnected from MAC='XX:XX:XX:XX:XX:XX' with status 129 (GATT_INTERNAL_ERROR)");
    }
}
