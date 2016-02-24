package com.polidea.rxandroidble;

import java.util.UUID;

import rx.Observable;

public interface RxBleConnection {

    class RxBleConnectionState {

        public static final RxBleConnectionState CONNECTING = new RxBleConnectionState("CONNECTING", false);
        public static final RxBleConnectionState CONNECTED = new RxBleConnectionState("CONNECTED", true);
        public static final RxBleConnectionState DISCONNECTED = new RxBleConnectionState("DISCONNECTED", false);
        public static final RxBleConnectionState DISCONNECTING = new RxBleConnectionState("DISCONNECTING", false);
        private final String description;
        private final boolean isUsable;

        RxBleConnectionState(String description, boolean isUsable) {
            this.description = description;
            this.isUsable = isUsable;
        }

        public boolean isUsable() {
            return this.isUsable;
        }

        @Override
        public String toString() {
            return "RxBleConnectionState{" +
                    description +
                    '}';
        }
    }

    Observable<RxBleDeviceServices> discoverServices();

    Observable<Observable<byte[]>> getNotification(UUID characteristicUuid);

    Observable<byte[]> readCharacteristic(UUID characteristicUuid);

    Observable<byte[]> writeCharacteristic(UUID characteristicUuid, byte[] data);

    Observable<byte[]> readDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid);

    Observable<byte[]> writeDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid, byte[] data);

    Observable<Integer> readRssi();
}
