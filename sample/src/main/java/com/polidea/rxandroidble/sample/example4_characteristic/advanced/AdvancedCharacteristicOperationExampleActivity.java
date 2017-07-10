package com.polidea.rxandroidble.sample.example4_characteristic.advanced;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.jakewharton.rxbinding.view.RxView;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.sample.DeviceActivity;
import com.polidea.rxandroidble.sample.R;
import com.polidea.rxandroidble.sample.SampleApplication;
import com.polidea.rxandroidble.sample.util.HexString;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;
import java.util.UUID;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * This activity allows for connecting to a device and interact with a given characteristic.
 * <p>
 * It may be used as a direct replacement for
 * {@link com.polidea.rxandroidble.sample.example4_characteristic.CharacteristicOperationExampleActivity}
 * <p>
 * When the connection is not established only the "CONNECT" button is active.
 * When the user will click on "CONNECT" button the connection is established and other buttons are enabled according to the properties
 * of the characteristic.
 * Subsequent clicks on "CONNECT" button (which name will change accordingly to the connection state) will close the connection.
 * <p>
 * When the connection is open other buttons are activated in regards of the characteristic's properties.
 * If the user clicks on "READ" a characteristic read is performed and the output is set on the TextView and EditText or a Snackbar is shown
 * in case of an error. "WRITE" clicks work the same as read but a write command is performed with data from the EditText.
 * If the characteristic has both of PROPERTY_NOTIFY and PROPERTY_INDICATE then only one of them is possible to be set at any given time.
 * Texts on notification and indication buttons will change accordingly to the current state of the notifications / indications.
 */
public class AdvancedCharacteristicOperationExampleActivity extends RxAppCompatActivity {

    private static final String TAG = AdvancedCharacteristicOperationExampleActivity.class.getSimpleName();
    public static final String EXTRA_CHARACTERISTIC_UUID = "extra_uuid";
    @BindView(R.id.connect)
    Button connectButton;
    @BindView(R.id.read_output)
    TextView readOutputView;
    @BindView(R.id.read_hex_output)
    TextView readHexOutputView;
    @BindView(R.id.write_input)
    TextView writeInput;
    @BindView(R.id.compat_only_warning)
    TextView compatOnlyWarningTextView;
    @BindView(R.id.read)
    Button readButton;
    @BindView(R.id.write)
    Button writeButton;
    @BindView(R.id.notify)
    Button notifyButton;
    @BindView(R.id.indicate)
    Button indicateButton;
    private Subscription activityFlowSubscription;
    private Observable<PresenterEvent> presenterEventObservable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example4_advanced);
        ButterKnife.bind(this);
        final String macAddress = getIntent().getStringExtra(DeviceActivity.EXTRA_MAC_ADDRESS);
        final UUID characteristicUuid = (UUID) getIntent().getSerializableExtra(EXTRA_CHARACTERISTIC_UUID);
        final RxBleDevice bleDevice = SampleApplication.getRxBleClient(this).getBleDevice(macAddress);
        //noinspection ConstantConditions
        getSupportActionBar().setSubtitle(getString(R.string.mac_address, macAddress));

        /*
         * Since in this activity we use the same button for user interaction for connecting the peripheral, disconnecting before connection
         * is established and disconnecting after the connection is being made we need to share the same activatedClicksObservable.
         * It would be perfectly fine to use three different buttons and pass those observables to the Presenter.
         */
        final Observable<Boolean> sharedConnectButtonClicks = activatedClicksObservable(connectButton).share();
        // same goes for setting up notifications and indications below
        final Observable<Boolean> sharedNotifyButtonClicks = activatedClicksObservable(notifyButton).share();
        final Observable<Boolean> sharedIndicateButtonClicks = activatedClicksObservable(indicateButton).share();

        presenterEventObservable = Presenter.prepareActivityLogic(
                bleDevice,
                characteristicUuid,
                sharedConnectButtonClicks.compose(onSubscribeSetText(connectButton, R.string.connect)),
                sharedConnectButtonClicks.compose(onSubscribeSetText(connectButton, R.string.connecting)),
                sharedConnectButtonClicks.compose(onSubscribeSetText(connectButton, R.string.disconnect)),
                activatedClicksObservable(readButton),
                /*
                 * Write button clicks are then mapped to byte[] from the editText. If there is a problem parsing input then a notification
                 * is shown and we wait for another click to write to try to parse again.
                 */
                activatedClicksObservable(writeButton).map(aBoolean -> getInputBytes())
                        .doOnError(throwable -> showNotification("Could not parse input: " + throwable))
                        .retryWhen(errorNotificationHandler -> errorNotificationHandler),
                sharedNotifyButtonClicks.compose(onSubscribeSetText(notifyButton, R.string.setup_notification)),
                sharedNotifyButtonClicks.compose(onSubscribeSetText(notifyButton, R.string.setting_notification)),
                sharedNotifyButtonClicks.compose(onSubscribeSetText(notifyButton, R.string.teardown_notification)),
                sharedIndicateButtonClicks.compose(onSubscribeSetText(indicateButton, R.string.setup_indication)),
                sharedIndicateButtonClicks.compose(onSubscribeSetText(indicateButton, R.string.setting_indication)),
                sharedIndicateButtonClicks.compose(onSubscribeSetText(indicateButton, R.string.teardown_indication))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityFlowSubscription = presenterEventObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleEvent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        activityFlowSubscription.unsubscribe();
        activityFlowSubscription = null;
    }

    /**
     * Function that returns an observable that emits {@link Boolean#TRUE} every time the button is being clicked. It enables the button
     * whenever the returned Observable is being subscribed and disables it when un-subscribed. Takes care of making interactions with
     * the button on the proper thread.
     *
     * @param button the button to wrap into an Observable
     * @return the observable
     */
    @NonNull
    private static Observable<Boolean> activatedClicksObservable(Button button) {
        return Observable.using(
                () -> {
                    button.setEnabled(true);
                    return button;
                },
                aView -> RxView.clicks(aView).map(aVoid -> Boolean.TRUE), // some RxJava operators are not intended to use with nulls
                aView -> aView.setEnabled(false)
        )
                .subscribeOn(AndroidSchedulers.mainThread()) // RxView expects to be subscribed on the Main Thread
                .unsubscribeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Function that returns a {@link rx.Observable.Transformer} which will on subscribe set a text on a button using a proper thread
     *
     * @param button the button to set text on
     * @param textResId the text resource id
     * @return the transformer
     */
    @NonNull
    private static Observable.Transformer<Boolean, Boolean> onSubscribeSetText(Button button, @StringRes int textResId) {
        return booleanObservable -> booleanObservable
                .doOnSubscribe(() -> button.setText(textResId))
                .subscribeOn(AndroidSchedulers.mainThread());
    }

    private byte[] getInputBytes() {
        return HexString.hexToBytes(writeInput.getText().toString());
    }

    private void showNotification(String text) {
        Snackbar.make(findViewById(R.id.main), text, Snackbar.LENGTH_SHORT).show();
    }

    private void handleEvent(PresenterEvent presenterEvent) {
        Log.i(TAG, presenterEvent.toString());
        if (presenterEvent instanceof InfoEvent) {
            InfoEvent infoEvent = (InfoEvent) presenterEvent;
            final String infoText = infoEvent.infoText;
            showNotification(infoText);
        }
        if (presenterEvent instanceof CompatibilityModeEvent) {
            final CompatibilityModeEvent compatibilityModeEvent = (CompatibilityModeEvent) presenterEvent;
            final boolean isCompatibility = compatibilityModeEvent.show;
            compatOnlyWarningTextView.setVisibility(isCompatibility ? View.VISIBLE : View.INVISIBLE);
            if (isCompatibility) {
                /*
                All characteristics that have PROPERTY_NOTIFY or PROPERTY_INDICATE should contain
                a Client Characteristic Config Descriptor. The RxAndroidBle supports compatibility mode
                for setting the notifications / indications because it is not possible to fix the firmware
                in some third party peripherals. If you have possibility - inform the developer
                of the firmware that it is an error so they can fix.
                */
                Log.e(TAG, "THIS PERIPHERAL CHARACTERISTIC HAS PROPERTY_NOTIFY OR PROPERTY_INDICATE "
                        + "BUT DOES NOT HAVE CLIENT CHARACTERISTIC CONFIG DESCRIPTOR WHICH VIOLATES "
                        + "BLUETOOTH SPECIFICATION - CONTACT THE FIRMWARE DEVELOPER TO FIX IF POSSIBLE");
            }
        }
        if (presenterEvent instanceof ResultEvent) {
            final ResultEvent resultEvent = (ResultEvent) presenterEvent;
            switch (resultEvent.type) {

                case READ:
                    final byte[] updateReadValue = resultEvent.result;
                    final String stringValue = new String(updateReadValue);
                    readOutputView.setText(stringValue);
                    final String hexValueText = HexString.bytesToHex(updateReadValue);
                    readHexOutputView.setText(hexValueText);
                    writeInput.setText(hexValueText);
                    break;
                case WRITE:
                    showNotification("Write success");
                    break;
                case NOTIFY:
                    showNotification("Notification: " + HexString.bytesToHex(resultEvent.result));
                    break;
                case INDICATE:
                default: // added because Checkstyle is complaining
                    showNotification("Indication: " + HexString.bytesToHex(resultEvent.result));
                    break;
            }
        }
        if (presenterEvent instanceof ErrorEvent) {
            final ErrorEvent errorEvent = (ErrorEvent) presenterEvent;
            final Throwable throwable = errorEvent.error;
            final String notificationText;
            switch (errorEvent.type) {

                case READ:
                    notificationText = "Read error: " + throwable;
                    break;
                case WRITE:
                    notificationText = "Write error: " + throwable;
                    break;
                case NOTIFY:
                    notificationText = "Notifications error: " + throwable;
                    break;
                case INDICATE:
                default: // added because Checkstyle is complaining
                    notificationText = "Indications error: " + throwable;
                    break;
            }
            showNotification(notificationText);
        }
    }
}
