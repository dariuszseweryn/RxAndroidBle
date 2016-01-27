package com.polidea.bleplayground;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.polidea.rxandroidble.RxBleClientImpl;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleScanResult;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import rx.Observable;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private final RxBleClientImpl rxBleClient;

    public MainActivityFragment() {
        rxBleClient = new RxBleClientImpl();
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
                .doOnNext(scanResult -> Log.d("AAA", "scanned " + scanResult.getBleDevice()))
                .filter(rxBleScanResult -> rxBleScanResult.getBleDevice().getName() != null)
                .filter(rxBleScanResult -> rxBleScanResult.getBleDevice().getName().contains("8775"))
                .take(1)
                .map(RxBleScanResult::getBleDevice)
                .doOnNext(rxBleConnection -> Log.d("AAA", "got device"))
                .flatMap(rxBleDevice -> rxBleDevice.establishConnection(getContext()))
                .doOnNext(rxBleConnection -> Log.d("AAA", "connected"))
                .flatMap(rxBleConnection -> Observable.combineLatest(
                        rxBleConnection
                                .discoverServices()
                                .doOnCompleted(() -> Log.d("AAA", "DISCOVERY:COMPLETED"))
                                .doOnNext(uuidSetMap -> {
                                    for (Map.Entry<UUID, Set<UUID>> entry : uuidSetMap.entrySet()) {
                                        Log.d("AAA", "service: " + entry.getKey().toString());
                                        for (UUID characteristic : entry.getValue()) {
                                            Log.d("AAA", "characteristic: " + characteristic.toString());
                                        }
                                    }
                                }),
                        rxBleConnection
                                .readRssi()
                                .doOnCompleted(() -> Log.d("AAA", "RSSI:COMPLETED"))
                                .doOnNext(integer -> Log.d("AAA", "RSSI: " + integer)),
                        (uuidSetMap1, integer1) -> null
                ))
                .take(1) // TODO: needed for unsubscribing from RxBleDevice.establishConnection()
                .subscribe(
                        object -> Log.d("AAA", "connection finished"),
                        throwable -> Log.e("AAA", "an error", throwable),
                        () -> Log.d("AAA", "completed")
                );
    }
}
