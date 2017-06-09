package com.polidea.rxandroidble.exceptions;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.IntDef;
import android.support.annotation.RestrictTo;
import com.polidea.rxandroidble.NotificationSetupMode;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

@SuppressWarnings("unused")
public class BleCannotSetCharacteristicNotificationException extends BleException {

    @SuppressWarnings("WeakerAccess")
    @IntDef({UNKNOWN, CANNOT_SET_LOCAL_NOTIFICATION, CANNOT_FIND_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
            CANNOT_WRITE_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Reason {

    }

    /**
     * The unknown reason (probably because someone is externally instantiating {@link BleCannotSetCharacteristicNotificationException}
     */
    @SuppressWarnings("WeakerAccess")
    @Deprecated
    public static final int UNKNOWN = -1;

    /**
     * Used when call to {@link android.bluetooth.BluetoothGatt#setCharacteristicNotification(BluetoothGattCharacteristic, boolean)}
     * returns false.
     */
    public static final int CANNOT_SET_LOCAL_NOTIFICATION = 1;

    /**
     * Used when a call to {@link BluetoothGattCharacteristic#getDescriptor(UUID)} does not return the Client Characteristic Configuration
     * Descriptor ("00002902-0000-1000-8000-00805f9b34fb"). This usually happens when there is a bug in the implementation of
     * the peripheral's firmware - according to Bluetooth Core Specification v4.2 [Vol 3, Part G] 3.3.1.1 every characteristic that has
     * {@link BluetoothGattCharacteristic#PROPERTY_NOTIFY} or {@link BluetoothGattCharacteristic#PROPERTY_INDICATE} shall contain
     * the Client Characteristic Configuration Descriptor. This is a violation of the specification and should be fixed on the firmware
     * side. As a temporary workaround
     * {@link com.polidea.rxandroidble.RxBleConnection#setupNotification(BluetoothGattCharacteristic, NotificationSetupMode)} or
     * {@link com.polidea.rxandroidble.RxBleConnection#setupIndication(UUID, NotificationSetupMode)} can be used with
     * {@link NotificationSetupMode#COMPAT} - which will make the library call only
     * {@link android.bluetooth.BluetoothGatt#setCharacteristicNotification(BluetoothGattCharacteristic, boolean)} without writing
     * the descriptor.
     */
    public static final int CANNOT_FIND_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR = 2;

    /**
     * Used when a call to writing to Client Characteristic Configuration Descriptor fails due to
     * {@link BluetoothGatt#writeDescriptor(BluetoothGattDescriptor)} returns `false` or
     * {@link android.bluetooth.BluetoothGattCallback#onDescriptorWrite(BluetoothGatt, BluetoothGattDescriptor, int)} is called with status
     * other than {@link BluetoothGatt#GATT_SUCCESS}.
     */
    public static final int CANNOT_WRITE_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR = 3;

    private final BluetoothGattCharacteristic bluetoothGattCharacteristic;

    @Reason
    private final int reason;

    // TODO [DS] 08.07.2017 Remove in 2.0.0
    @Deprecated
    public BleCannotSetCharacteristicNotificationException(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
        this.reason = UNKNOWN;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public BleCannotSetCharacteristicNotificationException(BluetoothGattCharacteristic bluetoothGattCharacteristic, @Reason int reason) {
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
        this.reason = reason;
    }

    public BluetoothGattCharacteristic getBluetoothGattCharacteristic() {
        return bluetoothGattCharacteristic;
    }

    /**
     * Should return one of {@link #CANNOT_SET_LOCAL_NOTIFICATION}, {@link #CANNOT_FIND_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR},
     * {@link #CANNOT_WRITE_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR}
     *
     * @return the reason
     */
    @Reason
    public int getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "BleCannotSetCharacteristicNotificationException{"
                + "bluetoothGattCharacteristic=" + bluetoothGattCharacteristic.getUuid()
                + ", reason=" + reasonDescription()
                + " (see Javadoc for more comment)"
                + '}';
    }

    private String reasonDescription() {
        switch (reason) {

            case CANNOT_FIND_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR:
                return "CANNOT_FIND_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR";
            case CANNOT_WRITE_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR:
                return "CANNOT_WRITE_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR";
            case CANNOT_SET_LOCAL_NOTIFICATION:
                return "CANNOT_SET_LOCAL_NOTIFICATION";
            case UNKNOWN:
            default:
                return "UNKNOWN";
        }
    }
}
