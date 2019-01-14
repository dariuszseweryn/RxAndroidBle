package com.polidea.rxandroidble2.samplekotlin.example2_connection

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.sample.util.isConnected
import com.polidea.rxandroidble2.samplekotlin.R
import com.polidea.rxandroidble2.samplekotlin.SampleApplication
import com.polidea.rxandroidble2.samplekotlin.util.showSnackbarShort
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

private const val EXTRA_MAC_ADDRESS = "extra_mac_address"

class ConnectionExampleActivity : AppCompatActivity() {

    companion object {
        fun newInstance(context: Context, macAddress: String) =
            Intent(context, ConnectionExampleActivity::class.java).apply {
                putExtra(EXTRA_MAC_ADDRESS, macAddress)
            }
    }

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

    private var stateDisposable: Disposable? = null

    private val mtuDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example2)
        ButterKnife.bind(this)

        val macAddress = intent.getStringExtra(EXTRA_MAC_ADDRESS)
        title = getString(R.string.mac_address, macAddress)
        bleDevice = SampleApplication.rxBleClient.getBleDevice(macAddress)

        // How to listen for connection state changes
        bleDevice.observeConnectionStateChanges()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { onConnectionStateChange(it) }
            .let { stateDisposable = it }
    }

    @OnClick(R.id.connect_toggle)
    fun onConnectToggleClick() {
        if (bleDevice.isConnected) {
            triggerDisconnect()
        } else {
            bleDevice.establishConnection(autoConnectToggleSwitch.isChecked)
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { dispose() }
                .subscribe({ onConnectionReceived() }, { onConnectionFailure(it) })
                .let { connectionDisposable = it }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @OnClick(R.id.set_mtu)
    fun onSetMtu() {
        bleDevice.establishConnection(false)
            .flatMapSingle { rxBleConnection -> rxBleConnection.requestMtu(72) }
            .take(1) // Disconnect automatically after discovery
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { updateUI() }
            .subscribe({ onMtuReceived(it) }, { onConnectionFailure(it) })
            .let { mtuDisposable.add(it) }
    }

    private fun onConnectionFailure(throwable: Throwable) = showSnackbarShort("Connection error: $throwable")

    private fun onConnectionReceived() = showSnackbarShort("Connection received")

    private fun onConnectionStateChange(newState: RxBleConnection.RxBleConnectionState) {
        connectionStateView.text = newState.toString()
        updateUI()
    }

    private fun onMtuReceived(mtu: Int) = showSnackbarShort("MTU received: $mtu")

    private fun dispose() {
        connectionDisposable = null
        updateUI()
    }

    private fun triggerDisconnect() = connectionDisposable?.dispose()

    private fun updateUI() {
        connectButton.setText(if (bleDevice.isConnected) R.string.button_disconnect else R.string.button_connect)
        autoConnectToggleSwitch.isEnabled = !bleDevice.isConnected
    }

    override fun onPause() {
        super.onPause()
        triggerDisconnect()
        mtuDisposable.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        stateDisposable?.dispose()
    }
}
