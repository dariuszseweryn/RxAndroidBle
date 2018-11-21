package com.polidea.rxandroidble2.sample.example2_connection

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.SwitchCompat
import android.widget.Button
import android.widget.EditText
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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

private const val EXTRA_MAC_ADDRESS = "extra_mac_address"

internal fun Context.newConnectionExampleActivity(macAddress: String) =
    Intent(this, ConnectionExampleActivity::class.java).apply {
        putExtra(EXTRA_MAC_ADDRESS, macAddress)
    }

class ConnectionExampleActivity : RxAppCompatActivity() {

    @BindView(R.id.connection_state)
    internal lateinit var connectionStateView: TextView

    @BindView(R.id.connect_toggle)
    internal lateinit var connectButton: Button

    @BindView(R.id.newMtu)
    internal lateinit var textMtu: EditText

    @BindView(R.id.set_mtu)
    internal lateinit var setMtuButton: Button

    @BindView(R.id.autoconnect)
    internal lateinit var autoConnectToggleSwitch: SwitchCompat

    private lateinit var bleDevice: RxBleDevice

    private var connectionDisposable: Disposable? = null

    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example2)
        ButterKnife.bind(this)

        val macAddress = intent.getStringExtra(EXTRA_MAC_ADDRESS)
        title = getString(R.string.mac_address, macAddress)
        bleDevice = SampleApplication.rxBleClient.getBleDevice(macAddress)

        // How to listen for connection state changes
        bleDevice.observeConnectionStateChanges()
            .compose(bindUntilEvent(DESTROY))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { onConnectionStateChange(it) }
            .let { compositeDisposable.add(it) }
    }

    @OnClick(R.id.connect_toggle)
    fun onConnectToggleClick() {
        if (bleDevice.isConnected) {
            triggerDisconnect()
        } else {
            connectionDisposable = bleDevice.establishConnection(autoConnectToggleSwitch.isChecked)
                .compose(bindUntilEvent(PAUSE))
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { dispose() }
                .subscribe({ onConnectionReceived() }, { onConnectionFailure(it) })
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @OnClick(R.id.set_mtu)
    fun onSetMtu() {
        val disposable = bleDevice.establishConnection(false)
            .flatMapSingle { rxBleConnection -> rxBleConnection.requestMtu(72) }
            .take(1) // Disconnect automatically after discovery
            .compose(bindUntilEvent(PAUSE))
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { updateUI() }
            .subscribe({ onMtuReceived(it) }, { onConnectionFailure(it) })

        compositeDisposable.add(disposable)
    }

    private fun onConnectionFailure(throwable: Throwable) {
        showSnackbarShort(R.id.content, "Connection error: $throwable")
    }

    private fun onConnectionReceived() {
        showSnackbarShort(R.id.content, "Connection received")
    }

    private fun onConnectionStateChange(newState: RxBleConnection.RxBleConnectionState) {
        connectionStateView.text = newState.toString()
        updateUI()
    }

    private fun onMtuReceived(mtu: Int) {
        Snackbar.make(findViewById(android.R.id.content), "MTU received: $mtu", Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun dispose() {
        connectionDisposable = null
        updateUI()
    }

    private fun triggerDisconnect() {
        connectionDisposable?.dispose()
    }

    private fun updateUI() {
        connectButton.setText(if (bleDevice.isConnected) R.string.disconnect else R.string.connect)
        autoConnectToggleSwitch.isEnabled = !bleDevice.isConnected
    }

    override fun onPause() {
        super.onPause()
        triggerDisconnect()
        compositeDisposable.clear()
    }
}
