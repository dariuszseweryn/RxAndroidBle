package com.polidea.rxandroidble;

import rx.Observable;

public interface RxBleClient {

    Observable<RxBleDeviceImpl> scanBleDevices();

    RxBleDeviceImpl getBleDevice(String bluetoothAddress);
}
