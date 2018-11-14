package com.polidea.rxandroidble2.sample.example4_characteristic

import android.bluetooth.BluetoothGattCharacteristic
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.util.Log
import android.widget.Button
import android.widget.TextView

import com.jakewharton.rx.ReplayingShare
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.sample.DeviceActivity
import com.polidea.rxandroidble2.sample.R
import com.polidea.rxandroidble2.sample.SampleApplication
import com.polidea.rxandroidble2.sample.util.HexString
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity

import java.util.UUID

import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject

import com.trello.rxlifecycle2.android.ActivityEvent.PAUSE

class CharacteristicOperationExampleActivity : RxAppCompatActivity() {
    @BindView(R.id.connect)
    internal var connectButton: Button? = null
    @BindView(R.id.read_output)
    internal var readOutputView: TextView? = null
    @BindView(R.id.read_hex_output)
    internal var readHexOutputView: TextView? = null
    @BindView(R.id.write_input)
    internal var writeInput: TextView? = null
    @BindView(R.id.read)
    internal var readButton: Button? = null
    @BindView(R.id.write)
    internal var writeButton: Button? = null
    @BindView(R.id.notify)
    internal var notifyButton: Button? = null
    private var characteristicUuid: UUID? = null
    private val disconnectTriggerSubject = PublishSubject.create<Boolean>()
    private var connectionObservable: Observable<RxBleConnection>? = null
    private var bleDevice: RxBleDevice? = null
    private val compositeDisposable = CompositeDisposable()

    private val isConnected: Boolean
        get() = bleDevice!!.connectionState == RxBleConnection.RxBleConnectionState.CONNECTED

    private val inputBytes: ByteArray
        get() = HexString.hexToBytes(writeInput!!.text.toString())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example4)
        ButterKnife.bind(this)
        val macAddress = intent.getStringExtra(DeviceActivity.EXTRA_MAC_ADDRESS)
        characteristicUuid = intent.getSerializableExtra(EXTRA_CHARACTERISTIC_UUID) as UUID
        bleDevice = SampleApplication.rxBleClient.getBleDevice(macAddress)
        connectionObservable = prepareConnectionObservable()

        supportActionBar!!.subtitle = getString(R.string.mac_address, macAddress)
    }

    private fun prepareConnectionObservable(): Observable<RxBleConnection> {
        return bleDevice!!
            .establishConnection(false)
            .takeUntil(disconnectTriggerSubject)
            .compose(bindUntilEvent(PAUSE))
            .compose(ReplayingShare.instance())
    }

    @OnClick(R.id.connect)
    fun onConnectToggleClick() {

        if (isConnected) {
            triggerDisconnect()
        } else {
            val connectionDisposable = connectionObservable!!
                .flatMapSingle<RxBleDeviceServices>(Function<RxBleConnection, SingleSource<out RxBleDeviceServices>> { it.discoverServices() })
                .flatMapSingle<BluetoothGattCharacteristic> { rxBleDeviceServices ->
                    rxBleDeviceServices.getCharacteristic(
                        characteristicUuid!!
                    )
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { disposable -> connectButton!!.setText(R.string.connecting) }
                .subscribe(
                    { characteristic ->
                        updateUI(characteristic)
                        Log.i(javaClass.simpleName, "Hey, connection has been established!")
                    },
                    Consumer<Throwable> { this.onConnectionFailure(it) },
                    Action { this.onConnectionFinished() }
                )

            compositeDisposable.add(connectionDisposable)
        }
    }

    @OnClick(R.id.read)
    fun onReadClick() {

        if (isConnected) {
            val disposable = connectionObservable!!
                .firstOrError()
                .flatMap { rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUuid!!) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ bytes ->
                    readOutputView!!.text = String(bytes)
                    readHexOutputView!!.text = HexString.bytesToHex(bytes)
                    writeInput!!.text = HexString.bytesToHex(bytes)
                }, Consumer<Throwable> { this.onReadFailure(it) })

            compositeDisposable.add(disposable)
        }
    }

    @OnClick(R.id.write)
    fun onWriteClick() {

        if (isConnected) {
            val disposable = connectionObservable!!
                .firstOrError()
                .flatMap { rxBleConnection -> rxBleConnection.writeCharacteristic(characteristicUuid!!, inputBytes) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { bytes -> onWriteSuccess() },
                    Consumer<Throwable> { this.onWriteFailure(it) }
                )

            compositeDisposable.add(disposable)
        }
    }

    @OnClick(R.id.notify)
    fun onNotifyClick() {

        if (isConnected) {
            val disposable = connectionObservable!!
                .flatMap { rxBleConnection -> rxBleConnection.setupNotification(characteristicUuid!!) }
                .doOnNext { notificationObservable -> runOnUiThread { this.notificationHasBeenSetUp() } }
                .flatMap { notificationObservable -> notificationObservable }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    Consumer<ByteArray> { this.onNotificationReceived(it) },
                    Consumer<Throwable> { this.onNotificationSetupFailure(it) })

            compositeDisposable.add(disposable)
        }
    }

    private fun onConnectionFailure(throwable: Throwable) {

        Snackbar.make(findViewById<View>(R.id.main), "Connection error: $throwable", Snackbar.LENGTH_SHORT).show()
        updateUI(null)
    }

    private fun onConnectionFinished() {
        updateUI(null)
    }

    private fun onReadFailure(throwable: Throwable) {

        Snackbar.make(findViewById<View>(R.id.main), "Read error: $throwable", Snackbar.LENGTH_SHORT).show()
    }

    private fun onWriteSuccess() {

        Snackbar.make(findViewById<View>(R.id.main), "Write success", Snackbar.LENGTH_SHORT).show()
    }

    private fun onWriteFailure(throwable: Throwable) {

        Snackbar.make(findViewById<View>(R.id.main), "Write error: $throwable", Snackbar.LENGTH_SHORT).show()
    }

    private fun onNotificationReceived(bytes: ByteArray) {

        Snackbar.make(findViewById<View>(R.id.main), "Change: " + HexString.bytesToHex(bytes), Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun onNotificationSetupFailure(throwable: Throwable) {

        Snackbar.make(findViewById<View>(R.id.main), "Notifications error: $throwable", Snackbar.LENGTH_SHORT).show()
    }

    private fun notificationHasBeenSetUp() {

        Snackbar.make(findViewById<View>(R.id.main), "Notifications has been set up", Snackbar.LENGTH_SHORT).show()
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
        connectButton!!.setText(if (characteristic != null) R.string.disconnect else R.string.connect)
        readButton!!.isEnabled = hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_READ)
        writeButton!!.isEnabled = hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE)
        notifyButton!!.isEnabled = hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_NOTIFY)
    }

    private fun hasProperty(characteristic: BluetoothGattCharacteristic?, property: Int): Boolean {
        return characteristic != null && characteristic.properties and property > 0
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    companion object {

        val EXTRA_CHARACTERISTIC_UUID = "extra_uuid"
    }
}
