package com.polidea.rxandroidble2.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class BleScanExceptionTest {

    @Test
    public void toStringShouldContainMessage() {
        // given
        BleScanException out = new BleScanException(BleScanException.BLUETOOTH_DISABLED);

        // expect
        assertEquals(out.toString(), "com.polidea.rxandroidble2.exceptions.BleScanException: Bluetooth disabled (code 1)");
    }
}
