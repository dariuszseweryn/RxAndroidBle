package com.polidea.rxandroidble2.sample.example4_characteristic

import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.jakewharton.rx.ReplayingShare
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.sample.R
import com.polidea.rxandroidble2.sample.SampleApplication
import com.polidea.rxandroidble2.sample.util.hasProperty
import com.polidea.rxandroidble2.sample.util.hexToBytes
import com.polidea.rxandroidble2.sample.util.isConnected
import com.polidea.rxandroidble2.sample.util.showSnackbarShort
import com.polidea.rxandroidble2.sample.util.toHex
import com.trello.rxlifecycle2.android.ActivityEvent.PAUSE
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.util.UUID

private const val EXTRA_MAC_ADDRESS = "extra_mac_address"

private const val EXTRA_CHARACTERISTIC_UUID = "extra_uuid"

internal fun Context.newCharacteristicOperationExampleActivity(macAddress: String, uuid: UUID) =
    Intent(this, CharacteristicOperationExampleActivity::class.java).apply {
        putExtra(EXTRA_MAC_ADDRESS, macAddress)
        putExtra(EXTRA_CHARACTERISTIC_UUID, uuid)
    }

class CharacteristicOperationExampleActivity : RxAppCompatActivity() {

    @BindView(R.id.connect)
    internal lateinit var connectButton: Button

    @BindView(R.id.read_output)
    internal lateinit var readOutputView: TextView

    @BindView(R.id.read_hex_output)
    internal lateinit var readHexOutputView: TextView

    @BindView(R.id.write_input)
    internal lateinit var writeInput: TextView

    @BindView(R.id.read)
    internal lateinit var readButton: Button

    @BindView(R.id.write)
    internal lateinit var writeButton: Button

    @BindView(R.id.notify)
    internal lateinit var notifyButton: Button

    private lateinit var characteristicUuid: UUID

    private val disconnectTriggerSubject = PublishSubject.create<Boolean>()

    private lateinit var connectionObservable: Observable<RxBleConnection>

    private lateinit var bleDevice: RxBleDevice

    private val compositeDisposable = CompositeDisposable()

    private val inputBytes: ByteArray
        get() = writeInput.text.toString().hexToBytes()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example4)
        ButterKnife.bind(this)

        val macAddress = intent.getStringExtra(EXTRA_MAC_ADDRESS)
        characteristicUuid = intent.getSerializableExtra(EXTRA_CHARACTERISTIC_UUID) as UUID
        bleDevice = SampleApplication.rxBleClient.getBleDevice(macAddress)
        connectionObservable = prepareConnectionObservable()
        supportActionBar!!.subtitle = getString(R.string.mac_address, macAddress)
    }

    private fun prepareConnectionObservable(): Observable<RxBleConnection> =
        bleDevice
            .establishConnection(false)
            .takeUntil(disconnectTriggerSubject)
            .compose(bindUntilEvent(PAUSE))
            .compose(ReplayingShare.instance())

    @OnClick(R.id.connect)
    fun onConnectToggleClick() {
        if (bleDevice.isConnected) {
            triggerDisconnect()
        } else {
            connectionObservable
                .flatMapSingle { it.discoverServices() }
                .flatMapSingle { rxBleDeviceServices -> rxBleDeviceServices.getCharacteristic(characteristicUuid) }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { disposable -> connectButton.setText(R.string.connecting) }
                .subscribe(
                    { characteristic ->
                        updateUI(characteristic)
                        Log.i(javaClass.simpleName, "Hey, connection has been established!")
                    },
                    { onConnectionFailure(it) },
                    { onConnectionFinished() }
                )
                .let { compositeDisposable.add(it) }
        }
    }

    @OnClick(R.id.read)
    fun onReadClick() {
        if (bleDevice.isConnected) {
            connectionObservable
                .firstOrError()
                .flatMap { rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUuid) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ bytes ->
                    readOutputView.text = String(bytes)
                    readHexOutputView.text = bytes.toHex()
                    writeInput.text = bytes.toHex()
                }, { onReadFailure(it) })
                .let { compositeDisposable.add(it) }
        }
    }

    @OnClick(R.id.write)
    fun onWriteClick() {
        if (bleDevice.isConnected) {
            connectionObservable
                .firstOrError()
                .flatMap { rxBleConnection -> rxBleConnection.writeCharacteristic(characteristicUuid, inputBytes) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onWriteSuccess() }, { onWriteFailure(it) })
                .let { compositeDisposable.add(it) }
        }
    }

    @OnClick(R.id.notify)
    fun onNotifyClick() {
        if (bleDevice.isConnected) {
            connectionObservable
                .flatMap { rxBleConnection -> rxBleConnection.setupNotification(characteristicUuid) }
                .doOnNext { runOnUiThread { notificationHasBeenSetUp() } }
                .flatMap { it }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onNotificationReceived(it) }, { onNotificationSetupFailure(it) })
                .let { compositeDisposable.add(it) }
        }
    }

    private fun onConnectionFailure(throwable: Throwable) {
        showSnackbarShort(R.id.main, "Connection error: $throwable")
        updateUI(null)
    }

    private fun onConnectionFinished() {
        updateUI(null)
    }

    private fun onReadFailure(throwable: Throwable) {
        showSnackbarShort(R.id.main, "Read error: $throwable")
    }

    private fun onWriteSuccess() {
        showSnackbarShort(R.id.main, "Write success")
    }

    private fun onWriteFailure(throwable: Throwable) {
        showSnackbarShort(R.id.main, "Write error: $throwable")
    }

    private fun onNotificationReceived(bytes: ByteArray) {
        showSnackbarShort(R.id.main, "Change: ${bytes.toHex()}")
    }

    private fun onNotificationSetupFailure(throwable: Throwable) {
        showSnackbarShort(R.id.main, "Notifications error: $throwable")
    }

    private fun notificationHasBeenSetUp() {
        showSnackbarShort(R.id.main, "Notifications has been set up")
    }

    private fun triggerDisconnect() {
        disconnectTriggerSubject.onNext(true)
    }

    /**
     * This method updates the UI to a proper state.
     *
     * @param characteristic a nullable [BluetoothGattCharacteristic]. If it is null then UI is assuming a disconnected state.
     */
    private fun updateUI(characteristic: BluetoothGattCharacteristic?) {
        connectButton.setText(if (characteristic != null) R.string.disconnect else R.string.connect)
        readButton.isEnabled = characteristic.hasProperty(BluetoothGattCharacteristic.PROPERTY_READ)
        writeButton.isEnabled = characteristic.hasProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)
        notifyButton.isEnabled = characteristic.hasProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }
}
