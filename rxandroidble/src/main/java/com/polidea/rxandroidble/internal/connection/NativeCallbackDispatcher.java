package com.polidea.rxandroidble.internal.connection;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;

import javax.inject.Inject;

class NativeCallbackDispatcher {

    private BluetoothGattCallback nativeCallback;

    @Inject
    NativeCallbackDispatcher() {

    }

    public void notifyNativeChangedCallback(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (nativeCallback != null) {
            nativeCallback.onCharacteristicChanged(gatt, characteristic);
        }
    }

    public void notifyNativeConnectionStateCallback(BluetoothGatt gatt, int status, int newState) {
        if (nativeCallback != null) {
            nativeCallback.onConnectionStateChange(gatt, status, newState);
        }
    }

    public void notifyNativeDescriptorReadCallback(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (nativeCallback != null) {
            nativeCallback.onDescriptorRead(gatt, descriptor, status);
        }
    }

    public void notifyNativeDescriptorWriteCallback(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (nativeCallback != null) {
            nativeCallback.onDescriptorWrite(gatt, descriptor, status);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void notifyNativeMtuChangedCallback(BluetoothGatt gatt, int mtu, int status) {
        if (nativeCallback != null) {
            nativeCallback.onMtuChanged(gatt, mtu, status);
        }
    }

    public void notifyNativeReadRssiCallback(BluetoothGatt gatt, int rssi, int status) {
        if (nativeCallback != null) {
            nativeCallback.onReadRemoteRssi(gatt, rssi, status);
        }
    }

    public void notifyNativeReliableWriteCallback(BluetoothGatt gatt, int status) {
        if (nativeCallback != null) {
            nativeCallback.onReliableWriteCompleted(gatt, status);
        }
    }

    public void notifyNativeServicesDiscoveredCallback(BluetoothGatt gatt, int status) {
        if (nativeCallback != null) {
            nativeCallback.onServicesDiscovered(gatt, status);
        }
    }

    public void notifyNativeWriteCallback(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (nativeCallback != null) {
            nativeCallback.onCharacteristicWrite(gatt, characteristic, status);
        }
    }

    void setNativeCallback(BluetoothGattCallback callback) {
        this.nativeCallback = callback;
    }

    void notifyNativeReadCallback(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (nativeCallback != null) {
            nativeCallback.onCharacteristicRead(gatt, characteristic, status);
        }
    }
}
