package com.polidea.rxandroidble;

import android.content.Context;
import java.util.UUID;
import rx.Observable;

public interface RxBleConnection {

    // TODO: 26.01.2016 [PU] Why not an enum?
    class RxBleConnectionState {

        public static final RxBleConnectionState DISCONNECTING = new RxBleConnectionState("DISCONNECTING");

        public static final RxBleConnectionState DISCONNECTED = new RxBleConnectionState("DISCONNECTED");

        public static final RxBleConnectionState CONNECTING = new RxBleConnectionState("CONNECTING");

        public static final RxBleConnectionState CONNECTED = new RxBleConnectionState("CONNECTED");

        private final String description;

        public RxBleConnectionState(String description) {

            this.description = description;
        }

        @Override
        public String toString() {
            return "RxBleConnectionState{" +
                    description +
                    '}';
        }
    }

    Observable<RxBleConnection> connect(Context context); // TODO: hide from the user

    Observable<RxBleDeviceServices> discoverServices();

    Observable<Observable<byte[]>> getNotification(UUID characteristicUuid);

    Observable<byte[]> readCharacteristic(UUID characteristicUuid);

    Observable<byte[]> writeCharacteristic(UUID characteristicUuid, byte[] data);

    Observable<byte[]> readDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid);

    Observable<byte[]> writeDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid, byte[] data);

    Observable<Integer> readRssi();
}
