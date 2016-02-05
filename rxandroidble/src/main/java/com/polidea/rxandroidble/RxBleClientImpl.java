package com.polidea.rxandroidble;

import android.bluetooth.BluetoothAdapter;
import android.support.annotation.Nullable;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.RxBleRadioImpl;
import com.polidea.rxandroidble.internal.UUIDParser;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationScan;
import java.util.HashMap;
import java.util.UUID;
import rx.Observable;

public class RxBleClientImpl implements RxBleClient {

    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private final RxBleRadio rxBleRadio = new RxBleRadioImpl();

    private final HashMap<String, RxBleDevice> availableDevices = new HashMap<>(); // TODO: clean? don't cache?
    private final UUIDParser uuidParser = new UUIDParser();

    @Override
    public Observable<RxBleScanResult> scanBleDevices(@Nullable UUID[] filterServiceUUIDs) {
        final RxBleRadioOperationScan rxBleRadioOperationScan = new RxBleRadioOperationScan(filterServiceUUIDs, bluetoothAdapter,
                rxBleRadio, uuidParser);
        return rxBleRadio.queue(rxBleRadioOperationScan)
                .doOnUnsubscribe(rxBleRadioOperationScan::stop);
    }

    @Override
    public RxBleDevice getBleDevice(String bluetoothAddress) {
        final RxBleDevice rxBleDevice = availableDevices.get(bluetoothAddress);
        if (rxBleDevice != null) {
            return rxBleDevice;
        }

        final RxBleDeviceImpl newRxBleDevice = new RxBleDeviceImpl(bluetoothAdapter.getRemoteDevice(bluetoothAddress), rxBleRadio);
        availableDevices.put(bluetoothAddress, newRxBleDevice);
        return newRxBleDevice;
    }
}
