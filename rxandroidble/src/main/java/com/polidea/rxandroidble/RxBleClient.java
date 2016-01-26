package com.polidea.rxandroidble;

import android.bluetooth.le.ScanSettings;
import android.support.annotation.Nullable;
import java.util.UUID;
import rx.Observable;

public interface RxBleClient {

    /**
     * @throws com.polidea.rxandroidble.exceptions.BleScanException in case of error starting the scan
     * @throws com.polidea.rxandroidble.exceptions.BleNotAvailableException in case of Bluetooth not available or not enabled
     * @param filterServiceUUIDs
     * @return
     */
    Observable<RxBleScanResult> scanBleDevices(
            @Nullable UUID[] filterServiceUUIDs
    );

    RxBleDevice getBleDevice(String bluetoothAddress);
}
