package com.polidea.bleplayground;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.RxBleScanResult;

import java.util.UUID;

import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private RxBleClient rxBleClient;

    public MainActivityFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        rxBleClient = RxBleClient.getInstance(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        rxBleClient.scanBleDevices(null)
                .subscribeOn(Schedulers.io())
                .doOnNext(scanResult -> Log.d("AAA", "scanned " + scanResult.getBleDevice()))
                .filter(rxBleScanResult -> rxBleScanResult.getBleDevice().getName() != null)
                .filter(rxBleScanResult -> rxBleScanResult.getBleDevice().getName().contains("8775"))
                .take(1)
                .map(RxBleScanResult::getBleDevice)
                .subscribeOn(Schedulers.newThread())
                .doOnNext(rxBleConnection -> Log.d("AAA", "got device"))
                .flatMap(rxBleDevice -> rxBleDevice.establishConnection(getContext(), false))
                .doOnNext(rxBleConnection -> Log.d("AAA", "connected"))
                .flatMap(rxBleConnection -> Observable.combineLatest(
                        rxBleConnection
                                .discoverServices()
                                .observeOn(Schedulers.io())
                                .doOnCompleted(() -> Log.d("AAA", "DISCOVERY:COMPLETED"))
                                .map(RxBleDeviceServices::getBluetoothGattServices)
                                .doOnNext(serviceList -> {
                                    for (BluetoothGattService bluetoothGattService : serviceList) {
                                        Log.d("DISCOVERED", "service: " + bluetoothGattService.getUuid().toString());
                                        for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattService.getCharacteristics()) {
                                            Log.d("DISCOVERED", "characteristic: " + bluetoothGattCharacteristic.getUuid().toString());
                                            for (BluetoothGattDescriptor bluetoothGattDescriptor : bluetoothGattCharacteristic.getDescriptors()) {
                                                Log.d("DISCOVERED", "descriptor: " + bluetoothGattDescriptor.getUuid().toString());
                                            }
                                            Log.d("DISCOVERED", "characteristic: " + bluetoothGattCharacteristic.getUuid().toString() + " has config: "
                                                    + (bluetoothGattCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))));
                                        }
                                    }
                                }),
                        rxBleConnection
                                .readRssi()
                                .doOnCompleted(() -> Log.d("AAA", "RSSI:COMPLETED"))
                                .doOnNext(integer -> Log.d("AAA", "RSSI: " + integer)),
                        rxBleConnection
                                .readCharacteristic(UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb"))
                                .doOnNext(bytes -> Log.d("AAA", "Manufacturer: " + new String(bytes))),
                        (uuidSetMap1, integer1, bytes1) -> null

                ))
                .take(1) // TODO: needed for unsubscribing from RxBleDevice.establishConnection()
                .subscribe(
                        object -> Log.d("AAA", "connection finished"),
                        throwable -> Log.e("AAA", "an error", throwable),
                        () -> Log.d("AAA", "completed")
                );
    }
}
