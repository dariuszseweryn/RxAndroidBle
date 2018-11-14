package com.polidea.rxandroidble2.sample.example2_connection

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.SwitchCompat
import android.view.View
import android.widget.Button
import android.widget.EditText
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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

class ConnectionExampleActivity : RxAppCompatActivity() {

    @BindView(R.id.connection_state)
    internal var connectionStateView: TextView? = null
    @BindView(R.id.connect_toggle)
    internal var connectButton: Button? = null
    @BindView(R.id.newMtu)
    internal var textMtu: EditText? = null
    @BindView(R.id.set_mtu)
    internal var setMtuButton: Button? = null
    @BindView(R.id.autoconnect)
    internal var autoConnectToggleSwitch: SwitchCompat? = null
    private var bleDevice: RxBleDevice? = null
    private var connectionDisposable: Disposable? = null
    private val compositeDisposable = CompositeDisposable()

    private val isConnected: Boolean
        get() = bleDevice!!.connectionState == RxBleConnection.RxBleConnectionState.CONNECTED

    @OnClick(R.id.connect_toggle)
    fun onConnectToggleClick() {
        if (isConnected) {
            triggerDisconnect()
        } else {
            connectionDisposable = bleDevice!!.establishConnection(autoConnectToggleSwitch!!.isChecked)
                .compose(bindUntilEvent(PAUSE))
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { this.dispose() }
                .subscribe(
                    { this.onConnectionReceived(it) },
                    { this.onConnectionFailure(it) }
                )
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @OnClick(R.id.set_mtu)
    fun onSetMtu() {
        val disposable = bleDevice!!.establishConnection(false)
            .flatMapSingle { rxBleConnection -> rxBleConnection.requestMtu(72) }
            .take(1) // Disconnect automatically after discovery
            .compose(bindUntilEvent(PAUSE))
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { this.updateUI() }
            .subscribe({ this.onMtuReceived(it) }, { this.onConnectionFailure(it) })

        compositeDisposable.add(disposable)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example2)
        ButterKnife.bind(this)
        val macAddress = intent.getStringExtra(DeviceActivity.EXTRA_MAC_ADDRESS)
        title = getString(R.string.mac_address, macAddress)
        bleDevice = SampleApplication.rxBleClient.getBleDevice(macAddress)
        // How to listen for connection state changes
        val disposable = bleDevice!!.observeConnectionStateChanges()
            .compose<RxBleConnectionState>(bindUntilEvent<RxBleConnectionState>(DESTROY))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { this.onConnectionStateChange(it) }

        compositeDisposable.add(disposable)
    }

    private fun onConnectionFailure(throwable: Throwable) {

        Snackbar.make(findViewById<View>(android.R.id.content), "Connection error: $throwable", Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun onConnectionReceived(connection: RxBleConnection) {

        Snackbar.make(findViewById<View>(android.R.id.content), "Connection received", Snackbar.LENGTH_SHORT).show()
    }

    private fun onConnectionStateChange(newState: RxBleConnection.RxBleConnectionState) {
        connectionStateView!!.text = newState.toString()
        updateUI()
    }

    private fun onMtuReceived(mtu: Int?) {

        Snackbar.make(findViewById<View>(android.R.id.content), "MTU received: " + mtu!!, Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun dispose() {
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
        autoConnectToggleSwitch!!.isEnabled = !connected
    }

    override fun onPause() {
        super.onPause()

        triggerDisconnect()
        compositeDisposable.clear()
    }
}
