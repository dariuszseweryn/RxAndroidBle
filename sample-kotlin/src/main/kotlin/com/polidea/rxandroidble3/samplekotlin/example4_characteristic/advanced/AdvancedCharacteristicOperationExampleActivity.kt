package com.polidea.rxandroidble3.samplekotlin.example4_characteristic.advanced

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.jakewharton.rxbinding4.view.clicks
import com.polidea.rxandroidble3.samplekotlin.R
import com.polidea.rxandroidble3.samplekotlin.SampleApplication
import com.polidea.rxandroidble3.samplekotlin.util.showSnackbarShort
import com.polidea.rxandroidble3.samplekotlin.util.toHex
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.android.synthetic.main.activity_example4_advanced.compat_only_warning
import kotlinx.android.synthetic.main.activity_example4_advanced.connect_button
import kotlinx.android.synthetic.main.activity_example4_advanced.indicate_button
import kotlinx.android.synthetic.main.activity_example4_advanced.notify_button
import kotlinx.android.synthetic.main.activity_example4_advanced.read_button
import kotlinx.android.synthetic.main.activity_example4_advanced.read_hex_output
import kotlinx.android.synthetic.main.activity_example4_advanced.read_output
import kotlinx.android.synthetic.main.activity_example4_advanced.write_button
import kotlinx.android.synthetic.main.activity_example4_advanced.write_input
import java.util.UUID

private val TAG = AdvancedCharacteristicOperationExampleActivity::class.java.simpleName

private const val EXTRA_MAC_ADDRESS = "extra_mac_address"

private const val EXTRA_CHARACTERISTIC_UUID = "extra_uuid"

/**
 * This activity allows for connecting to a device and interact with a given characteristic.
 *
 * It may be used as a direct replacement for
 * [CharacteristicOperationExampleActivity][com.polidea.rxandroidble3.samplekotlin.example4_characteristic.CharacteristicOperationExampleActivity]
 *
 * When the connection is not established only the "CONNECT" button is active.
 * When the user clicks on the "CONNECT" button the connection is established and other buttons are enabled according to the properties
 * of the characteristic.
 * Subsequent clicks on the "CONNECT" button (whose name will change according to the connection state) will close the connection.
 *
 * When the connection is open other buttons are activated in regards of the characteristic's properties.
 * If the user clicks on "READ" a characteristic read is performed and the output is set on the TextView and EditText or a Snackbar is shown
 * in case of an error. "WRITE" clicks work the same as read but a write command is performed with data from the EditText.
 * If the characteristic has both of PROPERTY_NOTIFY and PROPERTY_INDICATE then only one of them is possible to be set at any given time.
 * Texts on notification and indication buttons will change accordingly to the current state of the notifications / indications.
 */
class AdvancedCharacteristicOperationExampleActivity : AppCompatActivity() {

    companion object {
        fun newInstance(context: Context, macAddress: String, uuid: UUID) =
            Intent(context, AdvancedCharacteristicOperationExampleActivity::class.java).apply {
                putExtra(EXTRA_MAC_ADDRESS, macAddress)
                putExtra(EXTRA_CHARACTERISTIC_UUID, uuid)
            }
    }

    private var activityFlowDisposable: Disposable? = null

    private lateinit var presenterEventObservable: Observable<PresenterEvent>

    private val inputBytes: ByteArray
        get() = write_input.text.toString().toByteArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example4_advanced)

        val macAddress = intent.getStringExtra(EXTRA_MAC_ADDRESS)
        val characteristicUuid = intent.getSerializableExtra(EXTRA_CHARACTERISTIC_UUID) as UUID
        val bleDevice = SampleApplication.rxBleClient.getBleDevice(macAddress!!)

        supportActionBar!!.subtitle = getString(R.string.mac_address, macAddress)

        /*
         * Since in this activity we use the same button for user interaction for connecting the peripheral, disconnecting before connection
         * is established and disconnecting after the connection is being made we need to share the same activatedClicksObservable.
         * It would be perfectly fine to use three different buttons and pass those observables to the Presenter.
         */
        val sharedConnectButtonClicks = connect_button.activatedClicksObservable().share()
        // same goes for setting up notifications and indications below
        val sharedNotifyButtonClicks = notify_button.activatedClicksObservable().share()
        val sharedIndicateButtonClicks = indicate_button.activatedClicksObservable().share()

        // We setup the button texts reflecting the current state of the button for connect button, notification button
        // and indication button.
        val (connect, connecting, disconnect) =
            sharedConnectButtonClicks.setupButtonTexts(
                connect_button,
                R.string.button_connect,
                R.string.connecting,
                R.string.button_disconnect
            )

        val (setupNotification, settingNotification, teardownNotification) =
            sharedNotifyButtonClicks.setupButtonTexts(
                notify_button,
                R.string.button_setup_notification,
                R.string.setting_notification,
                R.string.teardown_notification
            )

        val (setupIndication, settingIndication, teardownIndication) =
            sharedIndicateButtonClicks.setupButtonTexts(
                indicate_button,
                R.string.button_setup_indication,
                R.string.setting_indication,
                R.string.teardown_indication
            )

        val readObservable = read_button.activatedClicksObservable()

        /*
         * Write button clicks are then mapped to byte[] from the editText. If there is a problem parsing input then a notification
         * is shown and we wait for another click to write to try to parse again.
         */
        val writeObservable =
            write_button.activatedClicksObservable()
                .map { inputBytes }
                .doOnError { throwable -> showSnackbarShort("Could not parse input: $throwable") }
                .retryWhen { it }

        presenterEventObservable = prepareActivityLogic(
            bleDevice,
            characteristicUuid,
            connect,
            connecting,
            disconnect,
            readObservable,
            writeObservable,
            setupNotification,
            settingNotification,
            teardownNotification,
            setupIndication,
            settingIndication,
            teardownIndication
        )
    }

    override fun onResume() {
        super.onResume()
        presenterEventObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { it.handleEvent() }
            .let { activityFlowDisposable = it }
    }

    override fun onPause() {
        super.onPause()
        activityFlowDisposable?.dispose()
        activityFlowDisposable = null
    }

    /**
     * Handles different presenter event types. For each type redirects to appropriate function.
     */
    private fun PresenterEvent.handleEvent() {
        Log.i(TAG, toString())
        when (this) {
            is InfoEvent -> showSnackbarShort(infoText)
            is CompatibilityModeEvent -> handleCompatibility()
            is ResultEvent -> handleResult()
            is ErrorEvent -> handleError()
        }
    }

    /**
     * Handles compatibility mode event.
     */
    private fun CompatibilityModeEvent.handleCompatibility() {
        compat_only_warning.visibility = if (isCompatibility) View.VISIBLE else View.INVISIBLE
        if (isCompatibility) {
            /*
            All characteristics that have PROPERTY_NOTIFY or PROPERTY_INDICATE should contain
            a Client Characteristic Config Descriptor. The RxAndroidBle supports compatibility mode
            for setting the notifications / indications because it is not possible to fix the firmware
            in some third party peripherals. If you have possibility - inform the developer
            of the firmware that it is an error so they can fix.
            */
            Log.e(
                TAG, """
                            |THIS PERIPHERAL CHARACTERISTIC HAS PROPERTY_NOTIFY OR PROPERTY_INDICATE
                            |BUT DOES NOT HAVE CLIENT CHARACTERISTIC CONFIG DESCRIPTOR WHICH VIOLATES
                            |BLUETOOTH SPECIFICATION - CONTACT THE FIRMWARE DEVELOPER TO FIX IF POSSIBLE
                            """.trimMargin()
            )
        }
    }

    /**
     * Handles result event.
     */
    private fun ResultEvent.handleResult() {
        when (type) {
            Type.READ -> {
                read_output.text = String(result.toByteArray())
                read_hex_output.text = result.toByteArray().toHex()
                write_input.setText(result.toByteArray().toHex())
            }
            Type.WRITE -> showSnackbarShort("Write success")
            Type.NOTIFY -> showSnackbarShort("Notification: ${result.toByteArray().toHex()}")
            Type.INDICATE -> showSnackbarShort("Indication: ${result.toByteArray().toHex()}")
        }
    }

    /**
     * Handles error event.
     */
    private fun ErrorEvent.handleError() {
        @Suppress("ReplaceSingleLineLet")
        when (type) {
            Type.READ -> "Read error: $error"
            Type.WRITE -> "Write error: $error"
            Type.NOTIFY -> "Notifications error: $error"
            Type.INDICATE -> "Indications error: $error"
        }.let { showSnackbarShort(it) }
    }
}

/**
 * Function that returns an observable that emits `true` every time the button is being clicked. It enables the button
 * whenever the returned Observable is being subscribed and disables it when un-subscribed. Takes care of making interactions with
 * the button on the proper thread.
 *
 * @param button the button to wrap into an Observable
 * @return the observable
 */
private fun Button.activatedClicksObservable(): Observable<Boolean> =
    Observable.using(
        { apply { isEnabled = true } },
        { it.clicks().map { true } },
        { it.isEnabled = false }
    )
        .subscribeOn(AndroidSchedulers.mainThread()) // RxView expects to be subscribed on the Main Thread
        .unsubscribeOn(AndroidSchedulers.mainThread())

/**
 * Set up button texts reflecting current button state.
 *
 * @param start button text to trigger the process when it's not yet started
 * @param progress button text while in progress
 * @param end button text to end the process
 */
private fun Observable<Boolean>.setupButtonTexts(
    button: Button,
    @StringRes start: Int,
    @StringRes progress: Int,
    @StringRes end: Int
): Triple<Observable<Boolean>, Observable<Boolean>, Observable<Boolean>> =
    Triple(
        compose(button.onSubscribeSetText(start)),
        compose(button.onSubscribeSetText(progress)),
        compose(button.onSubscribeSetText(end))
    )

/**
 * Function that returns an [ObservableTransformer] which will on subscribe
 * set a text on a button using a proper thread
 *
 * @param button the button to set text on
 * @param textResId the text resource id
 * @return the transformer
 */
private fun Button.onSubscribeSetText(@StringRes textResId: Int): ObservableTransformer<Boolean, Boolean> =
    ObservableTransformer {
        it.doOnSubscribe { setText(textResId) }
            .subscribeOn(AndroidSchedulers.mainThread())
    }
