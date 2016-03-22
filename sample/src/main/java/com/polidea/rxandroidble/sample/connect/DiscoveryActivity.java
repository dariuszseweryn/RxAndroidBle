package com.polidea.rxandroidble.sample.connect;

import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.widget.Button;
import android.widget.TextView;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.sample.R;
import com.polidea.rxandroidble.sample.SampleApplication;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;

import static com.trello.rxlifecycle.ActivityEvent.PAUSE;

public class DiscoveryActivity extends RxAppCompatActivity {

    public static final String EXTRA_MAC_ADDRESS = "extra_mac_address";
    @Bind(R.id.mac_address)
    TextView macAddressView;
    @Bind(R.id.connection_state)
    TextView connectionStateView;
    @Bind(R.id.connect_toggle)
    Button connectButton;
    @Bind(R.id.autoconnect)
    SwitchCompat autoConnectToggleSwitch;
    private String macAddress;
    private RxBleDevice bleDevice;
    private Observable<RxBleConnection> connectionObservable;
    private PublishSubject<Void> subjectTriggeringDisconnect = PublishSubject.create();

    @OnClick(R.id.connect_toggle)
    public void onScanToggleClick() {

        if (isConnected()) {
            triggerDisconnect();
        } else {
            connectionObservable = bleDevice.establishConnection(this, autoConnectToggleSwitch.isChecked())
                    .takeUntil(subjectTriggeringDisconnect)
                    .compose(bindUntilEvent(PAUSE))
                    .doOnUnsubscribe(this::setNotConnected)
                    .compose(new ConnectionSharingAdapter());
            connectionObservable
                    .flatMap(RxBleConnection::discoverServices)
                    .subscribe(this::updateDiscoveredServices, this::onConnectionFailure);
        }

        updateUI();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discovery);
        ButterKnife.bind(this);
        RxBleClient rxBleClient = SampleApplication.getRxBleClient(this);
        macAddress = getIntent().getStringExtra(EXTRA_MAC_ADDRESS);
        bleDevice = rxBleClient.getBleDevice(macAddress);
        macAddressView.setText(getString(R.string.mac_address, macAddress));
        bleDevice.getConnectionState()
                .compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onConnectionStateChange);
    }

    private boolean isConnected() {
        return connectionObservable != null;
    }

    private void onConnectionFailure(Throwable throwable) {
        // TODO: [PU] 21.03.2016 Handle connection failures here.
    }

    private void onConnectionStateChange(RxBleConnection.RxBleConnectionState newState) {
        connectionStateView.setText(newState.toString());
    }

    private void setNotConnected() {
        connectionObservable = null;
        updateUI();
    }

    private void triggerDisconnect() {
        subjectTriggeringDisconnect.onNext(null);
    }

    private void updateDiscoveredServices(RxBleDeviceServices rxBleDeviceServices) {
        // TODO: [PU] 21.03.2016 Show discovered services here.
    }

    private void updateUI() {
        runOnUiThread(() -> {
            final boolean connected = isConnected();
            connectButton.setText(connected ? R.string.disconnect : R.string.connect);
            autoConnectToggleSwitch.setEnabled(!connected);
        });
    }
}
