package com.polidea.rxandroidble.internal;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation denoting that annotated int is either one or combination of flags describing characteristic properties
 * from {@link BluetoothGattCharacteristic}.
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef(flag = true,
        value = {BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PROPERTY_BROADCAST,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE,
                BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY})
public @interface BluetoothGattCharacteristicProperty { }
