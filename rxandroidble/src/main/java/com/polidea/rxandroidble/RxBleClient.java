package com.polidea.rxandroidble;

import android.support.annotation.Nullable;

import java.util.UUID;

import rx.Observable;

public interface RxBleClient {

    RxBleDevice getBleDevice(String bluetoothAddress);

    /**
     * Returns an infinite observable returning BLE scan results. Scan is automatically started and stopped based on the Observable lifecycle.
     *
     * @param filterServiceUUIDs Filtering settings. Scan results are only filtered by exported services.
     * @throws com.polidea.rxandroidble.exceptions.BleScanException         in case of error starting the scan
     * @throws com.polidea.rxandroidble.exceptions.BleNotAvailableException in case of Bluetooth not available or not enabled
     */
    Observable<RxBleScanResult> scanBleDevices(
            @Nullable UUID[] filterServiceUUIDs
    );
}
