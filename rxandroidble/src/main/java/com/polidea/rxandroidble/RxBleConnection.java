package com.polidea.rxandroidble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import java.util.UUID;

import rx.Observable;

public interface RxBleConnection {

    interface Connector {

        Observable<RxBleConnection> prepareConnection(Context context, boolean autoConnect);
    }

    class RxBleConnectionState {

        public static final RxBleConnectionState CONNECTING = new RxBleConnectionState("CONNECTING");
        public static final RxBleConnectionState CONNECTED = new RxBleConnectionState("CONNECTED");
        public static final RxBleConnectionState DISCONNECTED = new RxBleConnectionState("DISCONNECTED");
        public static final RxBleConnectionState DISCONNECTING = new RxBleConnectionState("DISCONNECTING");
        private final String description;

        RxBleConnectionState(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return "RxBleConnectionState{" +
                    description +
                    '}';
        }
    }

    Observable<RxBleConnectionState> getConnectionState();

    Observable<RxBleDeviceServices> discoverServices();

    Observable<Observable<byte[]>> getNotification(UUID characteristicUuid);

    Observable<BluetoothGattCharacteristic> getCharacteristic(UUID characteristicUuid);

    Observable<byte[]> readCharacteristic(UUID characteristicUuid);

    Observable<byte[]> writeCharacteristic(UUID characteristicUuid, byte[] data);

    Observable<BluetoothGattCharacteristic> writeCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic);

    Observable<byte[]> readDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid);

    Observable<byte[]> writeDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid, byte[] data);

    Observable<Integer> readRssi();
}
