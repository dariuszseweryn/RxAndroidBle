package com.polidea.rxandroidble.custom_operation;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

/**
 * Proxy for the {@link BluetoothGatt} class. Functions that are available on this interface and on {@link BluetoothGatt} should be called
 * on this interface only to ensure proper behaviour. If a particular method is available only on {@link BluetoothGatt} it is safe to use it
 * directly. Used by {@link com.polidea.rxandroidble.helpers.CustomOperationHelper} for managing and releasing
 * {@link com.polidea.rxandroidble.internal.RadioReleaseInterface} when appropriate.
 *
 * The {@link BluetoothGattProxy} implementation will track how many times a function was called so it will be possible to check if
 * {@link android.bluetooth.BluetoothGattCallback} was called in balanced manner and only afterwards release the
 * {@link com.polidea.rxandroidble.internal.RadioReleaseInterface}. This may be especially useful when a custom operation would
 * be completed / errored / unsubscribed in the middle of interaction with {@link BluetoothGatt}.
 */
public interface BluetoothGattProxy {

    BluetoothGatt getBluetoothGatt(); // convenience

    boolean discoverServices();

    boolean readCharacteristic(BluetoothGattCharacteristic characteristic);

    boolean writeCharacteristic(BluetoothGattCharacteristic characteristic);

    boolean readDescriptor(BluetoothGattDescriptor descriptor);

    boolean writeDescriptor(BluetoothGattDescriptor descriptor);

    // [DS] 26.07.2017 Below functions should probably be synchronized differently that the above

    boolean requestMtu(int mtu);

    boolean requestConnectionPriority(int connectionPriority);
}
