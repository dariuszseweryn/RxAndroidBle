package com.polidea.rxandroidble2.sample.example5_rssi_periodic

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.sample.R
import com.polidea.rxandroidble2.sample.SampleApplication
import com.polidea.rxandroidble2.sample.util.isConnected
import com.polidea.rxandroidble2.sample.util.showSnackbarShort
import com.trello.rxlifecycle2.android.ActivityEvent.DESTROY
import com.trello.rxlifecycle2.android.ActivityEvent.PAUSE
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit.SECONDS

private const val EXTRA_MAC_ADDRESS = "extra_mac_address"

internal fun Context.newRssiPeriodicExampleActivity(macAddress: String) =
    Intent(this, RssiPeriodicExampleActivity::class.java).apply {
        putExtra(EXTRA_MAC_ADDRESS, macAddress)
    }

class RssiPeriodicExampleActivity : RxAppCompatActivity() {

    @BindView(R.id.connection_state)
    internal lateinit var connectionStateView: TextView

    @BindView(R.id.rssi)
    internal lateinit var rssiView: TextView

    @BindView(R.id.connect_toggle)
    internal lateinit var connectButton: Button

    private lateinit var bleDevice: RxBleDevice

    private var connectionDisposable: Disposable? = null

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example5)
        ButterKnife.bind(this)

        val macAddress = intent.getStringExtra(EXTRA_MAC_ADDRESS)
        title = getString(R.string.mac_address, macAddress)
        bleDevice = SampleApplication.rxBleClient.getBleDevice(macAddress)

        // How to listen for connection state changes
        bleDevice.observeConnectionStateChanges()
            .compose(bindUntilEvent(DESTROY))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { onConnectionStateChange(it) }
    }

    @OnClick(R.id.connect_toggle)
    fun onConnectToggleClick() {
        if (bleDevice.isConnected) {
            triggerDisconnect()
        } else {
            bleDevice.establishConnection(false)
                .compose(bindUntilEvent(PAUSE))
                .doFinally { clearSubscription() }
                .flatMap { connection ->
                    // Set desired interval.
                    Observable.interval(2, SECONDS).flatMapSingle { connection.readRssi() }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ updateRssi(it) }, { onConnectionFailure(it) })
                .let { connectionDisposable = it }
        }
    }

    private fun updateRssi(rssiValue: Int) {
        rssiView.text = getString(R.string.read_rssi, rssiValue)
    }

    private fun onConnectionFailure(throwable: Throwable) {
        showSnackbarShort(R.id.content, "Connection error: $throwable")
    }

    private fun onConnectionStateChange(newState: RxBleConnection.RxBleConnectionState) {
        connectionStateView.text = newState.toString()
        updateUI()
    }

    private fun clearSubscription() {
        connectionDisposable = null
        updateUI()
    }

    private fun triggerDisconnect() {
        connectionDisposable?.dispose()
    }

    private fun updateUI() {
        connectButton.setText(if (bleDevice.isConnected) R.string.disconnect else R.string.connect)
    }
}
