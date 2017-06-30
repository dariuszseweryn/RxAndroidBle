package com.polidea.rxandroidble;


import android.bluetooth.BluetoothGattDescriptor;
import java.util.UUID;

public class Descriptor {

    private UUID serviceUuid;

    private UUID characteristicUuid;

    private int characteristicInstanceId;

    private UUID descriptorUuid;

    private BluetoothGattDescriptor nativeDescriptor;
}
