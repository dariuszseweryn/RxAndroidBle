package com.polidea.rxandroidble2.sample.example5_rssi_periodic;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import android.widget.Button;
import android.widget.TextView;

import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.sample.DeviceActivity;
import com.polidea.rxandroidble2.sample.R;
import com.polidea.rxandroidble2.sample.SampleApplication;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;

import static java.util.concurrent.TimeUnit.SECONDS;

public class RssiPeriodicExampleActivity extends AppCompatActivity {

    @BindView(R.id.connection_state)
    TextView connectionStateView;
    @BindView(R.id.rssi)
    TextView rssiView;
    @BindView(R.id.connect_toggle)
    Button connectButton;
    private RxBleDevice bleDevice;
    private Disposable connectionDisposable;
    private Disposable stateDisposable;

    @OnClick(R.id.connect_toggle)
    public void onConnectToggleClick() {

        if (isConnected()) {
            triggerDisconnect();
        } else {
            connectionDisposable = bleDevice.establishConnection(false)
                    .doFinally(this::clearSubscription)
                    .flatMap(rxBleConnection -> // Set desired interval.
                            Observable.interval(2, SECONDS).flatMapSingle(sequence -> rxBleConnection.readRssi()))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::updateRssi, this::onConnectionFailure);
        }
    }

    private void updateRssi(int rssiValue) {
        rssiView.setText(getString(R.string.read_rssi, rssiValue));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example5);
        ButterKnife.bind(this);
        String macAddress = getIntent().getStringExtra(DeviceActivity.EXTRA_MAC_ADDRESS);
        setTitle(getString(R.string.mac_address, macAddress));
        bleDevice = SampleApplication.getRxBleClient(this).getBleDevice(macAddress);

        // How to listen for connection state changes
        stateDisposable = bleDevice.observeConnectionStateChanges()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onConnectionStateChange);
    }

    private boolean isConnected() {
        return bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    private void onConnectionFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(android.R.id.content), "Connection error: " + throwable, Snackbar.LENGTH_SHORT).show();
    }

    private void onConnectionStateChange(RxBleConnection.RxBleConnectionState newState) {
        connectionStateView.setText(newState.toString());
        updateUI();
    }

    private void clearSubscription() {
        connectionDisposable = null;
        updateUI();
    }

    private void triggerDisconnect() {

        if (connectionDisposable != null) {
            connectionDisposable.dispose();
        }
    }

    private void updateUI() {
        final boolean connected = isConnected();
        connectButton.setText(connected ? R.string.disconnect : R.string.connect);
    }

    @Override
    protected void onPause() {
        super.onPause();

        triggerDisconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (stateDisposable != null) {
            stateDisposable.dispose();
        }
    }
}
