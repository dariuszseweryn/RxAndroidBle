package com.polidea.rxandroidble.sample.example2_connection;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.SwitchCompat;
import android.widget.Button;
import android.widget.TextView;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.sample.DeviceActivity;
import com.polidea.rxandroidble.sample.R;
import com.polidea.rxandroidble.sample.SampleApplication;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import static com.trello.rxlifecycle.ActivityEvent.DESTROY;
import static com.trello.rxlifecycle.ActivityEvent.PAUSE;

public class ConnectionExampleActivity extends RxAppCompatActivity {

    @Bind(R.id.connection_state)
    TextView connectionStateView;
    @Bind(R.id.connect_toggle)
    Button connectButton;
    @Bind(R.id.autoconnect)
    SwitchCompat autoConnectToggleSwitch;
    private RxBleDevice bleDevice;
    private Subscription connectionSubscription;

    @OnClick(R.id.connect_toggle)
    public void onConnectToggleClick() {

        if (isConnected()) {
            triggerDisconnect();
        } else {
            connectionSubscription = bleDevice.establishConnection(this, autoConnectToggleSwitch.isChecked())
                    .compose(bindUntilEvent(PAUSE))
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnUnsubscribe(this::setNotConnected)
                    .subscribe(this::onConnectionReceived, this::onConnectionFailure);
        }

        updateUI();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example2);
        ButterKnife.bind(this);
        String macAddress = getIntent().getStringExtra(DeviceActivity.EXTRA_MAC_ADDRESS);
        setTitle(getString(R.string.mac_address, macAddress));
        bleDevice = SampleApplication.getRxBleClient(this).getBleDevice(macAddress);

        // How to listen for connection state changes
        bleDevice.getConnectionState()
                .compose(bindUntilEvent(DESTROY))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onConnectionStateChange);
    }

    private boolean isConnected() {
        return connectionSubscription != null;
    }

    private void onConnectionFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(android.R.id.content), "Connection error: " + throwable, Snackbar.LENGTH_SHORT).show();
    }

    private void onConnectionReceived(RxBleConnection connection) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(android.R.id.content), "Connection received", Snackbar.LENGTH_SHORT).show();
    }

    private void onConnectionStateChange(RxBleConnection.RxBleConnectionState newState) {
        connectionStateView.setText(newState.toString());
    }

    private void setNotConnected() {
        connectionSubscription = null;
        updateUI();
    }

    private void triggerDisconnect() {

        if (connectionSubscription != null) {
            connectionSubscription.unsubscribe();
        }
    }

    private void updateUI() {
        final boolean connected = isConnected();
        connectButton.setText(connected ? R.string.disconnect : R.string.connect);
        autoConnectToggleSwitch.setEnabled(!connected);
    }
}
