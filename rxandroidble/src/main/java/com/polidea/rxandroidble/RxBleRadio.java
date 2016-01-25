package com.polidea.rxandroidble;

public interface RxBleRadio {

    class Action {

        Action BLE_SCAN = new Action();

        Action READ = new Action();

        Action WRITE = new Action();

        Action WRITE_RELIABLE = new Action();

        Action READ_DESCRIPTOR = new Action();

        Action WRITE_DESCRIPTOR = new Action();

        Action READ_RSSI = new Action();

        Action CONNECT = new Action();

        Action DISCONNECT = new Action();
    }

}
