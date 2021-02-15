package com.polidea.rxandroidble2.sample.example4_characteristic;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.jakewharton.rx3.ReplayingShare;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.sample.DeviceActivity;
import com.polidea.rxandroidble2.sample.R;
import com.polidea.rxandroidble2.sample.SampleApplication;
import com.polidea.rxandroidble2.sample.util.HexString;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class CharacteristicOperationExampleActivity extends AppCompatActivity {

    public static final String EXTRA_CHARACTERISTIC_UUID = "extra_uuid";
    @BindView(R.id.connect)
    Button connectButton;
    @BindView(R.id.read_output)
    TextView readOutputView;
    @BindView(R.id.read_hex_output)
    TextView readHexOutputView;
    @BindView(R.id.write_input)
    TextView writeInput;
    @BindView(R.id.read)
    Button readButton;
    @BindView(R.id.write)
    Button writeButton;
    @BindView(R.id.notify)
    Button notifyButton;
    private UUID characteristicUuid;
    private PublishSubject<Boolean> disconnectTriggerSubject = PublishSubject.create();
    private Observable<RxBleConnection> connectionObservable;
    private RxBleDevice bleDevice;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    public static Intent startActivityIntent(Context context, String peripheralMacAddress, UUID characteristicUuid) {
        Intent intent = new Intent(context, CharacteristicOperationExampleActivity.class);
        intent.putExtra(DeviceActivity.EXTRA_MAC_ADDRESS, peripheralMacAddress);
        intent.putExtra(EXTRA_CHARACTERISTIC_UUID, characteristicUuid);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example4);
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
                .establishConnection(false)
                .takeUntil(disconnectTriggerSubject)
                .compose(ReplayingShare.instance());
    }

    @OnClick(R.id.connect)
    public void onConnectToggleClick() {

        if (isConnected()) {
            triggerDisconnect();
        } else {
            final Disposable connectionDisposable = connectionObservable
                    .flatMapSingle(RxBleConnection::discoverServices)
                    .flatMapSingle(rxBleDeviceServices -> rxBleDeviceServices.getCharacteristic(characteristicUuid))
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(disposable -> connectButton.setText(R.string.connecting))
                    .subscribe(
                            characteristic -> {
                                updateUI(characteristic);
                                Log.i(getClass().getSimpleName(), "Hey, connection has been established!");
                            },
                            this::onConnectionFailure,
                            this::onConnectionFinished
                    );

            compositeDisposable.add(connectionDisposable);
        }
    }

    @OnClick(R.id.read)
    public void onReadClick() {

        if (isConnected()) {
            final Disposable disposable = connectionObservable
                    .firstOrError()
                    .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUuid))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(bytes -> {
                        readOutputView.setText(new String(bytes));
                        readHexOutputView.setText(HexString.bytesToHex(bytes));
                        writeInput.setText(HexString.bytesToHex(bytes));
                    }, this::onReadFailure);

            compositeDisposable.add(disposable);
        }
    }

    @OnClick(R.id.write)
    public void onWriteClick() {

        if (isConnected()) {
            final Disposable disposable = connectionObservable
                    .firstOrError()
                    .flatMap(rxBleConnection -> rxBleConnection.writeCharacteristic(characteristicUuid, getInputBytes()))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            bytes -> onWriteSuccess(),
                            this::onWriteFailure
                    );

            compositeDisposable.add(disposable);
        }
    }

    @OnClick(R.id.notify)
    public void onNotifyClick() {

        if (isConnected()) {
            final Disposable disposable = connectionObservable
                    .flatMap(rxBleConnection -> rxBleConnection.setupNotification(characteristicUuid))
                    .doOnNext(notificationObservable -> runOnUiThread(this::notificationHasBeenSetUp))
                    .flatMap(notificationObservable -> notificationObservable)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onNotificationReceived, this::onNotificationSetupFailure);

            compositeDisposable.add(disposable);
        }
    }

    private boolean isConnected() {
        return bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    private void onConnectionFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Connection error: " + throwable, Snackbar.LENGTH_SHORT).show();
        updateUI(null);
    }

    private void onConnectionFinished() {
        updateUI(null);
    }

    private void onReadFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Read error: " + throwable, Snackbar.LENGTH_SHORT).show();
    }

    private void onWriteSuccess() {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Write success", Snackbar.LENGTH_SHORT).show();
    }

    private void onWriteFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Write error: " + throwable, Snackbar.LENGTH_SHORT).show();
    }

    private void onNotificationReceived(byte[] bytes) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Change: " + HexString.bytesToHex(bytes), Snackbar.LENGTH_SHORT).show();
    }

    private void onNotificationSetupFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Notifications error: " + throwable, Snackbar.LENGTH_SHORT).show();
    }

    private void notificationHasBeenSetUp() {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Notifications has been set up", Snackbar.LENGTH_SHORT).show();
    }

    private void triggerDisconnect() {
        disconnectTriggerSubject.onNext(true);
    }

    /**
     * This method updates the UI to a proper state.
     *
     * @param characteristic a nullable {@link BluetoothGattCharacteristic}. If it is null then UI is assuming a disconnected state.
     */
    private void updateUI(BluetoothGattCharacteristic characteristic) {
        connectButton.setText(characteristic != null ? R.string.disconnect : R.string.connect);
        readButton.setEnabled(hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_READ));
        writeButton.setEnabled(hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE));
        notifyButton.setEnabled(hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_NOTIFY));
    }

    private boolean hasProperty(BluetoothGattCharacteristic characteristic, int property) {
        return characteristic != null && (characteristic.getProperties() & property) > 0;
    }

    private byte[] getInputBytes() {
        return HexString.hexToBytes(writeInput.getText().toString());
    }

    @Override
    protected void onPause() {
        super.onPause();
        compositeDisposable.clear();
    }
}
