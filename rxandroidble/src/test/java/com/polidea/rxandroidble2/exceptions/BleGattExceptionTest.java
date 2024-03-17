package com.polidea.rxandroidble2.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import android.bluetooth.BluetoothGatt;

import org.junit.jupiter.api.Test;

import utils.MockUtils;

public class BleGattExceptionTest {

    @Test
    public void toStringShouldContainMessage() {
        // given
        BluetoothGatt mockBtGatt = MockUtils.bluetoothGatt("AA:BB:CC:DD:EE:FF");
        BleGattException out = new BleGattException(mockBtGatt, 10, BleGattOperationType.CONNECTION_STATE);

        // expect
        assertEquals(out.toString(),
        "com.polidea.rxandroidble2.exceptions.BleGattException: GATT exception from MAC='XX:XX:XX:XX:XX:XX', status 10 (GATT_NOT_FOUND), " +
                "type BleGattOperation{description='CONNECTION_STATE'}. " +
                "(Look up status 0x0a here " +
                "https://cs.android.com/android/platform/superproject/+/master:packages/modules/Bluetooth/system/stack/include/gatt_api.h)");
    }
}
