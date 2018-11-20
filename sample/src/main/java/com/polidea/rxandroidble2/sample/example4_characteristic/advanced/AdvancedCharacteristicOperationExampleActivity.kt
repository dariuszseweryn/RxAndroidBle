package com.polidea.rxandroidble2.sample.example4_characteristic.advanced

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.jakewharton.rxbinding2.view.RxView
import com.polidea.rxandroidble2.sample.R
import com.polidea.rxandroidble2.sample.SampleApplication
import com.polidea.rxandroidble2.sample.util.hexToBytes
import com.polidea.rxandroidble2.sample.util.showSnackbarShort
import com.polidea.rxandroidble2.sample.util.toHex
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.UUID

private val TAG = AdvancedCharacteristicOperationExampleActivity::class.java.simpleName

private const val EXTRA_MAC_ADDRESS = "extra_mac_address"

private const val EXTRA_CHARACTERISTIC_UUID = "extra_uuid"

internal fun Context.newAdvancedCharacteristicOperationExampleActivity(macAddress: String, uuid: UUID) =
    Intent(this, AdvancedCharacteristicOperationExampleActivity::class.java).apply {
        putExtra(EXTRA_MAC_ADDRESS, macAddress)
        putExtra(EXTRA_CHARACTERISTIC_UUID, uuid)
    }

/**
 * This activity allows for connecting to a device and interact with a given characteristic.
 *
 *
 * It may be used as a direct replacement for
 * [com.polidea.rxandroidble2.sample.example4_characteristic.CharacteristicOperationExampleActivity]
 *
 *
 * When the connection is not established only the "CONNECT" button is active.
 * When the user clicks on the "CONNECT" button the connection is established and other buttons are enabled according to the properties
 * of the characteristic.
 * Subsequent clicks on the "CONNECT" button (whose name will change according to the connection state) will close the connection.
 *
 *
 * When the connection is open other buttons are activated in regards of the characteristic's properties.
 * If the user clicks on "READ" a characteristic read is performed and the output is set on the TextView and EditText or a Snackbar is shown
 * in case of an error. "WRITE" clicks work the same as read but a write command is performed with data from the EditText.
 * If the characteristic has both of PROPERTY_NOTIFY and PROPERTY_INDICATE then only one of them is possible to be set at any given time.
 * Texts on notification and indication buttons will change accordingly to the current state of the notifications / indications.
 */
class AdvancedCharacteristicOperationExampleActivity : RxAppCompatActivity() {

    @BindView(R.id.connect)
    internal lateinit var connectButton: Button

    @BindView(R.id.read_output)
    internal lateinit var readOutputView: TextView

    @BindView(R.id.read_hex_output)
    internal lateinit var readHexOutputView: TextView

    @BindView(R.id.write_input)
    internal lateinit var writeInput: TextView

    @BindView(R.id.compat_only_warning)
    internal lateinit var compatOnlyWarningTextView: TextView

    @BindView(R.id.read)
    internal lateinit var readButton: Button

    @BindView(R.id.write)
    internal lateinit var writeButton: Button

    @BindView(R.id.notify)
    internal lateinit var notifyButton: Button

    @BindView(R.id.indicate)
    internal lateinit var indicateButton: Button

    private var activityFlowDisposable: Disposable? = null

    private lateinit var presenterEventObservable: Observable<PresenterEvent>

    private val inputBytes: ByteArray
        get() = writeInput.text.toString().hexToBytes()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example4_advanced)
        ButterKnife.bind(this)

        val macAddress = intent.getStringExtra(EXTRA_MAC_ADDRESS)
        val characteristicUuid = intent.getSerializableExtra(EXTRA_CHARACTERISTIC_UUID) as UUID
        val bleDevice = SampleApplication.rxBleClient.getBleDevice(macAddress)

        supportActionBar!!.subtitle = getString(R.string.mac_address, macAddress)

        /*
         * Since in this activity we use the same button for user interaction for connecting the peripheral, disconnecting before connection
         * is established and disconnecting after the connection is being made we need to share the same activatedClicksObservable.
         * It would be perfectly fine to use three different buttons and pass those observables to the Presenter.
         */
        val sharedConnectButtonClicks = connectButton.activatedClicksObservable().share()
        // same goes for setting up notifications and indications below
        val sharedNotifyButtonClicks = notifyButton.activatedClicksObservable().share()
        val sharedIndicateButtonClicks = indicateButton.activatedClicksObservable().share()

        presenterEventObservable = prepareActivityLogic(
            bleDevice,
            characteristicUuid,
            sharedConnectButtonClicks.compose(connectButton.onSubscribeSetText(R.string.connect)),
            sharedConnectButtonClicks.compose(connectButton.onSubscribeSetText(R.string.connecting)),
            sharedConnectButtonClicks.compose(connectButton.onSubscribeSetText(R.string.disconnect)),
            readButton.activatedClicksObservable(),
            /*
                 * Write button clicks are then mapped to byte[] from the editText. If there is a problem parsing input then a notification
                 * is shown and we wait for another click to write to try to parse again.
                 */
            writeButton.activatedClicksObservable().map { inputBytes }
                .doOnError { throwable -> showNotification("Could not parse input: $throwable") }
                .retryWhen { it },
            sharedNotifyButtonClicks.compose(notifyButton.onSubscribeSetText(R.string.setup_notification)),
            sharedNotifyButtonClicks.compose(notifyButton.onSubscribeSetText(R.string.setting_notification)),
            sharedNotifyButtonClicks.compose(notifyButton.onSubscribeSetText(R.string.teardown_notification)),
            sharedIndicateButtonClicks.compose(indicateButton.onSubscribeSetText(R.string.setup_indication)),
            sharedIndicateButtonClicks.compose(indicateButton.onSubscribeSetText(R.string.setting_indication)),
            sharedIndicateButtonClicks.compose(indicateButton.onSubscribeSetText(R.string.teardown_indication))
        )
    }

    override fun onResume() {
        super.onResume()
        activityFlowDisposable = presenterEventObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { it.handleEvent() }
    }

    override fun onPause() {
        super.onPause()
        activityFlowDisposable?.dispose()
        activityFlowDisposable = null
    }

    private fun showNotification(text: String) {
        showSnackbarShort(R.id.main, text)
    }

    private fun PresenterEvent.handleEvent() {
        Log.i(TAG, toString())
        when (this) {
            is InfoEvent -> showNotification(infoText)
            is CompatibilityModeEvent -> {
                val isCompatibility = show
                compatOnlyWarningTextView.visibility = if (isCompatibility) View.VISIBLE else View.INVISIBLE
                if (isCompatibility) {
                    /*
                    All characteristics that have PROPERTY_NOTIFY or PROPERTY_INDICATE should contain
                    a Client Characteristic Config Descriptor. The RxAndroidBle supports compatibility mode
                    for setting the notifications / indications because it is not possible to fix the firmware
                    in some third party peripherals. If you have possibility - inform the developer
                    of the firmware that it is an error so they can fix.
                    */
                    Log.e(
                        TAG, "THIS PERIPHERAL CHARACTERISTIC HAS PROPERTY_NOTIFY OR PROPERTY_INDICATE "
                                + "BUT DOES NOT HAVE CLIENT CHARACTERISTIC CONFIG DESCRIPTOR WHICH VIOLATES "
                                + "BLUETOOTH SPECIFICATION - CONTACT THE FIRMWARE DEVELOPER TO FIX IF POSSIBLE"
                    )
                }
            }
            is ResultEvent -> {
                when (type) {
                    Type.READ -> {
                        readOutputView.text = String(result)
                        readHexOutputView.text = result.toHex()
                        writeInput.text = result.toHex()
                    }
                    Type.WRITE -> showNotification("Write success")
                    Type.NOTIFY -> showNotification("Notification: ${result.toHex()}")
                    Type.INDICATE -> showNotification("Indication: ${result.toHex()}")
                }
            }
            is ErrorEvent -> {
                @Suppress("ReplaceSingleLineLet")
                when (type) {
                    Type.READ -> "Read error: $error"
                    Type.WRITE -> "Write error: $error"
                    Type.NOTIFY -> "Notifications error: $error"
                    Type.INDICATE -> "Indications error: $error"
                }.let { showNotification(it) }
            }
        }
    }
}

/**
 * Function that returns an observable that emits [Boolean.TRUE] every time the button is being clicked. It enables the button
 * whenever the returned Observable is being subscribed and disables it when un-subscribed. Takes care of making interactions with
 * the button on the proper thread.
 *
 * @param button the button to wrap into an Observable
 * @return the observable
 */
private fun Button.activatedClicksObservable(): Observable<Boolean> =
    Observable.using<Boolean, Button>(
        {
            isEnabled = true
            this
        },
        { RxView.clicks(it).map { true } },
        { it.isEnabled = false }
    )
        .subscribeOn(AndroidSchedulers.mainThread()) // RxView expects to be subscribed on the Main Thread
        .unsubscribeOn(AndroidSchedulers.mainThread())

/**
 * Function that returns a [io.reactivex.ObservableTransformer] which will on subscribe
 * set a text on a button using a proper thread
 *
 * @param button the button to set text on
 * @param textResId the text resource id
 * @return the transformer
 */
private fun Button.onSubscribeSetText(@StringRes textResId: Int): ObservableTransformer<Boolean, Boolean> =
    ObservableTransformer {
        it
            .doOnSubscribe { setText(textResId) }
            .subscribeOn(AndroidSchedulers.mainThread())
    }
