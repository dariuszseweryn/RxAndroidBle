package com.polidea.rxandroidble3.internal.connection;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import bleshadow.javax.inject.Inject;
import com.polidea.rxandroidble3.HiddenBluetoothGattCallback;

class NativeCallbackDispatcher {

    private BluetoothGattCallback nativeCallback;
    private HiddenBluetoothGattCallback nativeCallbackHidden;

    @Inject
    NativeCallbackDispatcher() {

    }

    void notifyNativeChangedCallback(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (nativeCallback != null) {
            nativeCallback.onCharacteristicChanged(gatt, characteristic);
        }
    }

    void notifyNativeConnectionStateCallback(BluetoothGatt gatt, int status, int newState) {
        if (nativeCallback != null) {
            nativeCallback.onConnectionStateChange(gatt, status, newState);
        }
    }

    void notifyNativeDescriptorReadCallback(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (nativeCallback != null) {
            nativeCallback.onDescriptorRead(gatt, descriptor, status);
        }
    }

    void notifyNativeDescriptorWriteCallback(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (nativeCallback != null) {
            nativeCallback.onDescriptorWrite(gatt, descriptor, status);
        }
    }

    @TargetApi(21 /* Build.VERSION_CODES.LOLLIPOP */)
    void notifyNativeMtuChangedCallback(BluetoothGatt gatt, int mtu, int status) {
        if (nativeCallback != null) {
            nativeCallback.onMtuChanged(gatt, mtu, status);
        }
    }

    void notifyNativeReadRssiCallback(BluetoothGatt gatt, int rssi, int status) {
        if (nativeCallback != null) {
            nativeCallback.onReadRemoteRssi(gatt, rssi, status);
        }
    }

    void notifyNativeReliableWriteCallback(BluetoothGatt gatt, int status) {
        if (nativeCallback != null) {
            nativeCallback.onReliableWriteCompleted(gatt, status);
        }
    }

    void notifyNativeServicesDiscoveredCallback(BluetoothGatt gatt, int status) {
        if (nativeCallback != null) {
            nativeCallback.onServicesDiscovered(gatt, status);
        }
    }

    void notifyNativeWriteCallback(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (nativeCallback != null) {
            nativeCallback.onCharacteristicWrite(gatt, characteristic, status);
        }
    }

    void notifyNativeParamsUpdateCallback(BluetoothGatt gatt, int interval, int latency, int timeout, int status) {
        if (nativeCallbackHidden != null) {
            nativeCallbackHidden.onConnectionUpdated(gatt, interval, latency, timeout, status);
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

    void setNativeCallbackHidden(HiddenBluetoothGattCallback callbackHidden) {
        this.nativeCallbackHidden = callbackHidden;
    }
}
