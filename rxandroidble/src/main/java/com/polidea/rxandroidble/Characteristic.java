package com.polidea.rxandroidble;


import android.bluetooth.BluetoothGattCharacteristic;
import java.util.UUID;

public class Characteristic {

    private UUID serviceUuid;

    private UUID characteristicUuid;

    private int instanceId;

    private BluetoothGattCharacteristic nativeCharacteristic;

    public static Characteristic withUuid(UUID characteristicUuid) {
        final Characteristic characteristic = new Characteristic();
        characteristic.characteristicUuid = characteristicUuid;
        return characteristic;
    }

    public static Characteristic withUuid(UUID characteristicUuid, UUID serviceUuid) {
        final Characteristic characteristic = new Characteristic();
        characteristic.serviceUuid = serviceUuid;
        characteristic.characteristicUuid = characteristicUuid;
        return characteristic;
    }

    public static Characteristic wrap(BluetoothGattCharacteristic nativeCharacteristic) {
        final Characteristic characteristic = new Characteristic();
        characteristic.nativeCharacteristic = nativeCharacteristic;
        characteristic.serviceUuid = nativeCharacteristic.getService().getUuid();
        characteristic.characteristicUuid = nativeCharacteristic.getUuid();
        characteristic.instanceId = nativeCharacteristic.getInstanceId();
        return characteristic;
    }
}
