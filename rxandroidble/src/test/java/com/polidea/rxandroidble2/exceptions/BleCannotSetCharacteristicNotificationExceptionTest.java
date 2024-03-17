package com.polidea.rxandroidble2.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import android.bluetooth.BluetoothGattCharacteristic;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

public class BleCannotSetCharacteristicNotificationExceptionTest {

    @Test
    public void toStringShouldContainMessage() {
        // given
        BluetoothGattCharacteristic mockCharacteristic = Mockito.mock(BluetoothGattCharacteristic.class);
        Mockito.when(mockCharacteristic.getUuid()).thenReturn(new UUID(1, 2));
        BleCannotSetCharacteristicNotificationException out = new BleCannotSetCharacteristicNotificationException(
                mockCharacteristic,
                BleCannotSetCharacteristicNotificationException.CANNOT_SET_LOCAL_NOTIFICATION,
                new Exception("because"));

        // expect
        assertEquals(out.toString(),
        "com.polidea.rxandroidble2.exceptions.BleCannotSetCharacteristicNotificationException: " +
                "Cannot set local notification (code 1) with characteristic UUID 00000000-0000-0001-0000-000000000002");
    }
}
