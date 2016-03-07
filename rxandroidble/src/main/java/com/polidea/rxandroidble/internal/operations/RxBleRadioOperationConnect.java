package com.polidea.rxandroidble.internal.operations;

import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTED;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import rx.Observable;
import rx.Subscription;
import rx.subjects.BehaviorSubject;

public class RxBleRadioOperationConnect extends RxBleRadioOperation<BluetoothGatt> {

    private final Context context;

    private final BluetoothDevice bluetoothDevice;

    private final RxBleGattCallback rxBleGattCallback;

    private final boolean autoConnect;

    private BehaviorSubject<BluetoothGatt> bluetoothGattBehaviorSubject = BehaviorSubject.create();

    private Subscription bluetoothGattSubscription;

    public RxBleRadioOperationConnect(Context context, BluetoothDevice bluetoothDevice, RxBleGattCallback rxBleGattCallback,
                                      boolean autoConnect) {
        this.context = context;
        this.bluetoothDevice = bluetoothDevice;
        this.rxBleGattCallback = rxBleGattCallback;
        this.autoConnect = autoConnect;
    }

    @Override
    public Observable<BluetoothGatt> asObservable() {
        return super.asObservable()
                .doOnUnsubscribe(() -> {
                    if (bluetoothGattSubscription != null) {
                        bluetoothGattSubscription.unsubscribe();
                    }
                });
    }

    @Override
    public void run() {
        //noinspection Convert2MethodRef
        final Runnable releaseRadioRunnable = () -> releaseRadio();
        final Runnable emptyRunnable = () -> {};

        final Runnable onNextRunnable = autoConnect ? emptyRunnable : releaseRadioRunnable;
        final Runnable onConnectCalledRunnable = autoConnect ? releaseRadioRunnable : emptyRunnable;

        getConnectedBluetoothGattObservable()
                .subscribe(
                        bluetoothGatt -> {
                            onNext(bluetoothGatt);
                            onNextRunnable.run();
                        },
                        (throwable) -> {
                            onError(throwable);
                            bluetoothGattBehaviorSubject.onCompleted();
                        },
                        () -> {
                            onCompleted();
                            bluetoothGattBehaviorSubject.onCompleted();
                        }
                );

        bluetoothGattSubscription = rxBleGattCallback.getBluetoothGatt().subscribe(bluetoothGattBehaviorSubject::onNext);
        final BluetoothGatt bluetoothGatt = connectGatt(bluetoothDevice, autoConnect, rxBleGattCallback.getBluetoothGattCallback());
        bluetoothGattBehaviorSubject.onNext(bluetoothGatt);
        onConnectCalledRunnable.run();
    }

    @NonNull
    private Observable<BluetoothGatt> getConnectedBluetoothGattObservable() {
        return Observable.combineLatest(
                rxBleGattCallback
                        .getOnConnectionStateChange()
                        .filter(rxBleConnectionState -> rxBleConnectionState == CONNECTED), // waiting for connected state
                getBluetoothGatt(), // using latest BluetoothGatt
                (rxBleConnectionState, bluetoothGatt) -> bluetoothGatt
        )
                .mergeWith(rxBleGattCallback.disconnectedErrorObservable()) // if gatt will disconnect during connecting emit error
                .first(); // using first
    }

    public Observable<BluetoothGatt> getBluetoothGatt() {
        return bluetoothGattBehaviorSubject;
    }

    private BluetoothGatt connectGatt(BluetoothDevice remoteDevice, boolean autoConnect, BluetoothGattCallback bluetoothGattCallback) {

        if (remoteDevice == null) {
            return null;
        }

        if (!autoConnect) {
            return connectGattCompat(bluetoothGattCallback, remoteDevice, false);
        }

        /**
         * Some implementations of Bluetooth Stack have a race condition where autoConnect flag
         * is not properly set before calling connectGatt. That's the reason for using reflection
         * to set the flag manually.
         */

        try {
            RxBleLog.v("Trying to connectGatt using reflection.");
            Object iBluetoothGatt = getIBluetoothGatt(getIBluetoothManager());

            if (iBluetoothGatt == null) {
                RxBleLog.w("Couldn't get iBluetoothGatt object");
                return connectGattCompat(bluetoothGattCallback, remoteDevice, true);
            }

            BluetoothGatt bluetoothGatt = createBluetoothGatt(iBluetoothGatt, remoteDevice);

            if (bluetoothGatt == null) {
                RxBleLog.w("Couldn't create BluetoothGatt object");
                return connectGattCompat(bluetoothGattCallback, remoteDevice, true);
            }

            boolean connectedSuccessfully = connectUsingReflection(bluetoothGatt, bluetoothGattCallback, true);

            if (!connectedSuccessfully) {
                RxBleLog.w("Connection using reflection failed, closing gatt");
                bluetoothGatt.close();
            }

            return bluetoothGatt;
        } catch (NoSuchMethodException |
                IllegalAccessException |
                IllegalArgumentException |
                InvocationTargetException |
                InstantiationException |
                NoSuchFieldException exception) {

            RxBleLog.w(exception, "Error during reflection");
            return connectGattCompat(bluetoothGattCallback, remoteDevice, true);
        }
    }

    private BluetoothGatt connectGattCompat(BluetoothGattCallback bluetoothGattCallback, BluetoothDevice remoteDevice, boolean autoConnect) {
        RxBleLog.v("Connecting without reflection");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return remoteDevice.connectGatt(context, autoConnect, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            return remoteDevice.connectGatt(context, autoConnect, bluetoothGattCallback);
        }
    }

    private boolean connectUsingReflection(BluetoothGatt bluetoothGatt, BluetoothGattCallback bluetoothGattCallback, boolean autoConnect)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        RxBleLog.v("Connecting using reflection");
        setAutoConnectValue(bluetoothGatt, autoConnect);
        Method connectMethod = bluetoothGatt.getClass().getDeclaredMethod("connect", Boolean.class, BluetoothGattCallback.class);
        connectMethod.setAccessible(true);
        return (Boolean) (connectMethod.invoke(bluetoothGatt, true, bluetoothGattCallback));
    }

    @TargetApi(Build.VERSION_CODES.M)
    private BluetoothGatt createBluetoothGatt(Object iBluetoothGatt, BluetoothDevice remoteDevice)
            throws IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor bluetoothGattConstructor = BluetoothGatt.class.getDeclaredConstructors()[0];
        bluetoothGattConstructor.setAccessible(true);
        RxBleLog.v("Found constructor with args count = " + bluetoothGattConstructor.getParameterTypes().length);

        if (bluetoothGattConstructor.getParameterTypes().length == 4) {
            return (BluetoothGatt) (bluetoothGattConstructor.newInstance(context, iBluetoothGatt, remoteDevice, BluetoothDevice.TRANSPORT_LE));
        } else {
            return (BluetoothGatt) (bluetoothGattConstructor.newInstance(context, iBluetoothGatt, remoteDevice));
        }
    }

    private Object getIBluetoothGatt(Object iBluetoothManager) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        if (iBluetoothManager == null) {
            return null;
        }

        Method getBluetoothGattMethod = getMethodFromClass(iBluetoothManager.getClass(), "getBluetoothGatt");
        return getBluetoothGattMethod.invoke(iBluetoothManager);
    }

    private Object getIBluetoothManager() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            return null;
        }

        Method getBluetoothManagerMethod = getMethodFromClass(bluetoothAdapter.getClass(), "getBluetoothManager");
        return getBluetoothManagerMethod.invoke(bluetoothAdapter);
    }

    private Method getMethodFromClass(Class<?> cls, String methodName) throws NoSuchMethodException {
        Method method = cls.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method;
    }

    private void setAutoConnectValue(BluetoothGatt bluetoothGatt, boolean autoConnect) throws NoSuchFieldException, IllegalAccessException {
        Field autoConnectField = bluetoothGatt.getClass().getDeclaredField("mAutoConnect");
        autoConnectField.setAccessible(true);
        autoConnectField.setBoolean(bluetoothGatt, autoConnect);
    }
}
