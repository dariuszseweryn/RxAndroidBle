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
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

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
    private Subscription connectionsSubscription;
    private Observable<RxBleConnection> connectionObservable;

    @OnClick(R.id.connect_toggle)
    public void onScanToggleClick() {

        if (isConnected()) {
            connectionsSubscription.unsubscribe();
        } else {
            connectionObservable = bleDevice.establishConnection(this, autoConnectToggleSwitch.isChecked())
                    .compose(bindUntilEvent(PAUSE))
                    .doOnUnsubscribe(this::setNotConnected)
                    .compose(new ConnectionSharingAdapter());
            connectionsSubscription = connectionObservable
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
        return connectionsSubscription != null;
    }

    private void onConnectionFailure(Throwable throwable) {
        // TODO: [PU] 21.03.2016 Handle connection failures here.
    }

    private void onConnectionStateChange(RxBleConnection.RxBleConnectionState newState) {
        connectionStateView.setText(newState.toString());
    }

    private void setNotConnected() {
        connectionsSubscription = null;
        connectionObservable = null;
    }

    private void updateDiscoveredServices(RxBleDeviceServices rxBleDeviceServices) {
        // TODO: [PU] 21.03.2016 Show discovered services here.
    }

    private void updateUI() {
        connectButton.setText(isConnected() ? R.string.disconnect : R.string.connect);
        autoConnectToggleSwitch.setEnabled(!isConnected());
    }
}
