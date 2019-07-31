package com.polidea.rxandroidble2.samplekotlin.example4_characteristic

import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.jakewharton.rx3.ReplayingShare
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.samplekotlin.R
import com.polidea.rxandroidble2.samplekotlin.SampleApplication
import com.polidea.rxandroidble2.samplekotlin.util.hasProperty
import com.polidea.rxandroidble2.samplekotlin.util.isConnected
import com.polidea.rxandroidble2.samplekotlin.util.showSnackbarShort
import com.polidea.rxandroidble2.samplekotlin.util.toHex
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_example4.connect
import kotlinx.android.synthetic.main.activity_example4.notify
import kotlinx.android.synthetic.main.activity_example4.read
import kotlinx.android.synthetic.main.activity_example4.read_hex_output
import kotlinx.android.synthetic.main.activity_example4.read_output
import kotlinx.android.synthetic.main.activity_example4.write
import kotlinx.android.synthetic.main.activity_example4.write_input
import java.util.UUID

private const val EXTRA_MAC_ADDRESS = "extra_mac_address"

private const val EXTRA_CHARACTERISTIC_UUID = "extra_uuid"

class CharacteristicOperationExampleActivity : AppCompatActivity() {

    companion object {
        fun newInstance(context: Context, macAddress: String, uuid: UUID) =
            Intent(context, CharacteristicOperationExampleActivity::class.java).apply {
                putExtra(EXTRA_MAC_ADDRESS, macAddress)
                putExtra(EXTRA_CHARACTERISTIC_UUID, uuid)
            }
    }

    private lateinit var characteristicUuid: UUID

    private val disconnectTriggerSubject = PublishSubject.create<Unit>()

    private lateinit var connectionObservable: Observable<RxBleConnection>

    private val connectionDisposable = CompositeDisposable()

    private lateinit var bleDevice: RxBleDevice

    private val inputBytes: ByteArray
        get() = write_input.text.toString().toByteArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example4)

        val macAddress = intent.getStringExtra(EXTRA_MAC_ADDRESS)
        characteristicUuid = intent.getSerializableExtra(EXTRA_CHARACTERISTIC_UUID) as UUID
        bleDevice = SampleApplication.rxBleClient.getBleDevice(macAddress!!)
        connectionObservable = prepareConnectionObservable()
        supportActionBar!!.subtitle = getString(R.string.mac_address, macAddress)

        connect.setOnClickListener { onConnectToggleClick() }
        read.setOnClickListener { onReadClick() }
        write.setOnClickListener { onWriteClick() }
        notify.setOnClickListener { onNotifyClick() }
    }

    private fun prepareConnectionObservable(): Observable<RxBleConnection> =
        bleDevice
            .establishConnection(false)
            .takeUntil(disconnectTriggerSubject)
            .compose(ReplayingShare.instance())

    private fun onConnectToggleClick() {
        if (bleDevice.isConnected) {
            triggerDisconnect()
        } else {
            connectionObservable
                .flatMapSingle { it.discoverServices() }
                .flatMapSingle { it.getCharacteristic(characteristicUuid) }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { connect.setText(R.string.connecting) }
                .subscribe(
                    { characteristic ->
                        updateUI(characteristic)
                        Log.i(javaClass.simpleName, "Hey, connection has been established!")
                    },
                    { onConnectionFailure(it) },
                    { updateUI(null) }
                )
                .let { connectionDisposable.add(it) }
        }
    }

    private fun onReadClick() {
        if (bleDevice.isConnected) {
            connectionObservable
                .firstOrError()
                .flatMap { it.readCharacteristic(characteristicUuid) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ bytes ->
                    read_output.text = String(bytes)
                    read_hex_output.text = bytes?.toHex()
                    write_input.setText(bytes?.toHex())
                }, { onReadFailure(it) })
                .let { connectionDisposable.add(it) }
        }
    }

    private fun onWriteClick() {
        if (bleDevice.isConnected) {
            connectionObservable
                .firstOrError()
                .flatMap { it.writeCharacteristic(characteristicUuid, inputBytes) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onWriteSuccess() }, { onWriteFailure(it) })
                .let { connectionDisposable.add(it) }
        }
    }

    private fun onNotifyClick() {
        if (bleDevice.isConnected) {
            connectionObservable
                .flatMap { it.setupNotification(characteristicUuid) }
                .doOnNext { runOnUiThread { notificationHasBeenSetUp() } }
                // we have to flatmap in order to get the actual notification observable
                // out of the enclosing observable, which only performed notification setup
                .flatMap { it }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onNotificationReceived(it) }, { onNotificationSetupFailure(it) })
                .let { connectionDisposable.add(it) }
        }
    }

    private fun onConnectionFailure(throwable: Throwable) {
        showSnackbarShort("Connection error: $throwable")
        updateUI(null)
    }

    private fun onReadFailure(throwable: Throwable) = showSnackbarShort("Read error: $throwable")

    private fun onWriteSuccess() = showSnackbarShort("Write success")

    private fun onWriteFailure(throwable: Throwable) = showSnackbarShort("Write error: $throwable")

    private fun onNotificationReceived(bytes: ByteArray) = showSnackbarShort("Change: ${bytes.toHex()}")

    private fun onNotificationSetupFailure(throwable: Throwable) =
        showSnackbarShort("Notifications error: $throwable")

    private fun notificationHasBeenSetUp() = showSnackbarShort("Notifications has been set up")

    private fun triggerDisconnect() = disconnectTriggerSubject.onNext(Unit)

    /**
     * This method updates the UI to a proper state.
     *
     * @param characteristic a nullable [BluetoothGattCharacteristic]. If it is null then UI is assuming a disconnected state.
     */
    private fun updateUI(characteristic: BluetoothGattCharacteristic?) {
        if (characteristic == null) {
            connect.setText(R.string.button_connect)
            read.isEnabled = false
            write.isEnabled = false
            notify.isEnabled = false
        } else {
            connect.setText(R.string.button_disconnect)
            with(characteristic) {
                read.isEnabled = hasProperty(BluetoothGattCharacteristic.PROPERTY_READ)
                write.isEnabled = hasProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)
                notify.isEnabled = hasProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        connectionDisposable.clear()
    }
}
