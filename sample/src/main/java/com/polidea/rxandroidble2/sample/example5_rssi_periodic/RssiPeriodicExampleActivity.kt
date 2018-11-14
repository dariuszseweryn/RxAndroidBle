package com.polidea.rxandroidble2.sample.example5_rssi_periodic

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.View
import android.widget.Button
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleConnection.RxBleConnectionState
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.sample.DeviceActivity
import com.polidea.rxandroidble2.sample.R
import com.polidea.rxandroidble2.sample.SampleApplication
import com.trello.rxlifecycle2.android.ActivityEvent.DESTROY
import com.trello.rxlifecycle2.android.ActivityEvent.PAUSE
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit.SECONDS

class RssiPeriodicExampleActivity : RxAppCompatActivity() {

    @BindView(R.id.connection_state)
    internal var connectionStateView: TextView? = null
    @BindView(R.id.rssi)
    internal var rssiView: TextView? = null
    @BindView(R.id.connect_toggle)
    internal var connectButton: Button? = null
    private var bleDevice: RxBleDevice? = null
    private var connectionDisposable: Disposable? = null
    private var stateDisposable: Disposable? = null

    private val isConnected: Boolean
        get() = bleDevice!!.connectionState == RxBleConnection.RxBleConnectionState.CONNECTED

    @OnClick(R.id.connect_toggle)
    fun onConnectToggleClick() {

        if (isConnected) {
            triggerDisconnect()
        } else {
            connectionDisposable = bleDevice!!.establishConnection(false)
                .compose(bindUntilEvent(PAUSE))
                .doFinally { clearSubscription() }
                .flatMap { rxBleConnection ->
                    // Set desired interval.
                    Observable.interval(2, SECONDS).flatMapSingle { sequence -> rxBleConnection.readRssi() }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { updateRssi(it) },
                    { onConnectionFailure(it) })
        }
    }

    private fun updateRssi(rssiValue: Int) {
        rssiView!!.text = getString(R.string.read_rssi, rssiValue)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example5)
        ButterKnife.bind(this)
        val macAddress = intent.getStringExtra(DeviceActivity.EXTRA_MAC_ADDRESS)
        title = getString(R.string.mac_address, macAddress)
        bleDevice = SampleApplication.rxBleClient.getBleDevice(macAddress)

        // How to listen for connection state changes
        stateDisposable = bleDevice!!.observeConnectionStateChanges()
            .compose<RxBleConnectionState>(bindUntilEvent<RxBleConnectionState>(DESTROY))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { onConnectionStateChange(it) }
    }

    private fun onConnectionFailure(throwable: Throwable) {

        Snackbar.make(findViewById<View>(android.R.id.content), "Connection error: $throwable", Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun onConnectionStateChange(newState: RxBleConnection.RxBleConnectionState) {
        connectionStateView!!.text = newState.toString()
        updateUI()
    }

    private fun clearSubscription() {
        connectionDisposable = null
        updateUI()
    }

    private fun triggerDisconnect() {

        if (connectionDisposable != null) {
            connectionDisposable!!.dispose()
        }
    }

    private fun updateUI() {
        val connected = isConnected
        connectButton!!.setText(if (connected) R.string.disconnect else R.string.connect)
    }

    override fun onPause() {
        super.onPause()

        triggerDisconnect()
        if (stateDisposable != null) {
            stateDisposable!!.dispose()
        }
    }
}
