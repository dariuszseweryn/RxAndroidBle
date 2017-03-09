package com.polidea.rxandroidble.sample.example6_custom_operation;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleRadioOperationCustom;
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.ByteAssociation;
import com.polidea.rxandroidble.sample.DeviceActivity;
import com.polidea.rxandroidble.sample.R;
import com.polidea.rxandroidble.sample.SampleApplication;
import com.polidea.rxandroidble.sample.util.HexString;
import com.polidea.rxandroidble.utils.ConnectionSharingAdapter;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Emitter;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

import static com.trello.rxlifecycle.android.ActivityEvent.PAUSE;

public class CustomOperationExampleActivity extends RxAppCompatActivity {

    public static final String EXTRA_CHARACTERISTIC_UUID = "extra_uuid";
    @BindView(R.id.connect)
    Button connectButton;
    @BindView(R.id.custom_output)
    TextView customOutputView;
    @BindView(R.id.custom_hex_output)
    TextView customHexOutputView;
    @BindView(R.id.run_custom)
    Button runCustomButton;
    private UUID characteristicUuid;
    private PublishSubject<Void> disconnectTriggerSubject = PublishSubject.create();
    private Observable<RxBleConnection> connectionObservable;
    private RxBleDevice bleDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example6);
        ButterKnife.bind(this);
        String macAddress = getIntent().getStringExtra(DeviceActivity.EXTRA_MAC_ADDRESS);
        characteristicUuid = (UUID) getIntent().getSerializableExtra(EXTRA_CHARACTERISTIC_UUID);
        bleDevice = SampleApplication.getRxBleClient(this).getBleDevice(macAddress);
        connectionObservable = prepareConnectionObservable();
        //noinspection ConstantConditions
        getSupportActionBar().setSubtitle(getString(R.string.mac_address, macAddress));
    }

    private Observable<RxBleConnection> prepareConnectionObservable() {
        return bleDevice
                .establishConnection(this, false)
                .takeUntil(disconnectTriggerSubject)
                .compose(bindUntilEvent(PAUSE))
                .doOnUnsubscribe(this::clearSubscription)
                .compose(new ConnectionSharingAdapter());
    }

    @OnClick(R.id.connect)
    public void onConnectToggleClick() {

        if (isConnected()) {
            triggerDisconnect();
        } else {
            connectionObservable.subscribe(rxBleConnection -> {
                Log.d(getClass().getSimpleName(), "Hey, connection has been established!");
                runOnUiThread(this::updateUI);
            }, this::onConnectionFailure);
        }
    }

    @OnClick(R.id.run_custom)
    public void onRunCustomClick() {

        if (isConnected()) {
            connectionObservable
                    .flatMap(rxBleConnection -> rxBleConnection.queue(new CustomReadOperation(rxBleConnection, characteristicUuid)))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(bytes -> {
                        customOutputView.setText(new String(bytes));
                        customHexOutputView.setText(HexString.bytesToHex(bytes));
                    }, this::onRunCustomFailure);
        }
    }

    private boolean isConnected() {
        return bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    private void onConnectionFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Connection error: " + throwable, Snackbar.LENGTH_SHORT).show();
    }

    private void onRunCustomFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Run custom error: " + throwable, Snackbar.LENGTH_SHORT).show();
    }

    private void clearSubscription() {
        updateUI();
    }

    private void triggerDisconnect() {
        disconnectTriggerSubject.onNext(null);
    }

    private void updateUI() {
        connectButton.setText(isConnected() ? getString(R.string.disconnect) : getString(R.string.connect));
        runCustomButton.setEnabled(isConnected());
    }

    private static class CustomReadOperation implements RxBleRadioOperationCustom<byte[]> {

        private RxBleConnection connection;
        private UUID characteristicUuid;

        CustomReadOperation(RxBleConnection connection, UUID characteristicUuid) {
            this.connection = connection;
            this.characteristicUuid = characteristicUuid;
        }

        /**
         * Reads a characteristic 5 times with a 250ms delay between each. This is easily achieve without
         * a custom operation. The gain here is that only one operation goes into the RxBleRadio queue
         * eliminating the overhead of going on & out of the operation queue.
         */
        @NonNull
        @Override
        public Observable<byte[]> asObservable(BluetoothGatt bluetoothGatt,
                                               RxBleGattCallback rxBleGattCallback,
                                               Scheduler scheduler) throws Throwable {
            return connection.getCharacteristic(characteristicUuid)
                    .flatMap(characteristic -> readAndObserve(characteristic, bluetoothGatt, rxBleGattCallback))
                    .subscribeOn(scheduler)
                    .takeFirst(readResponseForMatchingCharacteristic())
                    .map(byteAssociation -> byteAssociation.second)
                    .repeatWhen(notificationHandler -> notificationHandler.take(5).delay(250, TimeUnit.MILLISECONDS));
        }

        @NonNull
        private Observable<ByteAssociation<UUID>> readAndObserve(BluetoothGattCharacteristic characteristic,
                                                                 BluetoothGatt bluetoothGatt,
                                                                 RxBleGattCallback rxBleGattCallback) {
            Observable<ByteAssociation<UUID>> onCharacteristicRead = rxBleGattCallback.getOnCharacteristicRead();

            return Observable.fromEmitter(emitter -> {
                Subscription subscription = onCharacteristicRead.subscribe(emitter);
                emitter.setCancellation(subscription::unsubscribe);

                try {
                    final boolean success = bluetoothGatt.readCharacteristic(characteristic);
                    if (!success) {
                        throw new BleGattCannotStartException(bluetoothGatt, BleGattOperationType.CHARACTERISTIC_READ);
                    }
                } catch (Throwable throwable) {
                    emitter.onError(throwable);
                }
            }, Emitter.BackpressureMode.BUFFER);
        }

        private Func1<ByteAssociation<UUID>, Boolean> readResponseForMatchingCharacteristic() {
            return uuidByteAssociation -> uuidByteAssociation.first.equals(characteristicUuid);
        }
    }
}
