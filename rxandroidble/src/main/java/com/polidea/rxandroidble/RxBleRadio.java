package com.polidea.rxandroidble;

import rx.Observable;

public interface RxBleRadio {

    class Action {

        Action START_BLE_SCAN = new Action();
        Action STOP_BLE_SCAN = new Action();

        Action CHARACTERISTIC_READ = new Action();
        Action CHARACTERISTIC_WRITE = new Action();
        Action CHARACTEROSTOC_WRITE_RELIABLE = new Action();

        Action DESCRIPTOR_READ = new Action();
        Action DESCRIPTOR_WRITE = new Action();

        Action RSSI_READ = new Action();

        Action GATT_CONNECT = new Action();
        Action GATT_DISCONNECT = new Action();
        Action GATT_CLOSE = new Action();
    }

    <T> Observable<T> scheduleRadioObservable(Observable<T> radioBlockingObservable);
}
