package com.polidea.rxandroidble;

import android.bluetooth.BluetoothGatt;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Represents a custom operation that will be enqueued for future execution within the client instance.
 */
public interface RxBleRadioOperationCustom<T> {

    /**
     * Return an observable that implement a custom radio operation using low-level Android BLE API.
     * <p>
     * The {@link Observable<T>} returned by this method will be subscribed to by the {@link RxBleRadio}
     * when it determines that the custom operation should be the next to be run.
     * <p>
     * The method receives everything needed to access the low-level Android BLE API objects mainly the
     * {@link BluetoothGatt} to interact with Android BLE GATT operations and {@link RxBleGattCallback}
     * to be notified when GATT operations completes.
     * <p>
     * Every event emitted by the returned {@link Observable<T>} will be forwarded to the observable
     * returned by {@link RxBleConnection#queue(RxBleRadioOperationCustom)}
     * <p>
     * As the implementer, your contract is to return an {@link Observable<T>} that completes at some
     * point in time. When the returned observable terminates, either via the {@link Observer#onCompleted()} or
     * {@link Observer#onError(Throwable)} callback, the {@link RxBleRadio} queue's lock is released so that
     * queue operations can continue.
     * <p>
     * You <b>must</b> ensure the returned {@link Observable<T>} do terminate either via {@code onCompleted}
     * or {@code onError(Throwable)}. Otherwise, the internal queue orchestrator will wait forever for
     * your {@link Observable<T>} to complete and the it will not continue to process queued operations.
     *
     * @param bluetoothGatt     The Android API GATT instance
     * @param rxBleGattCallback The internal Rx ready bluetooth gatt callback to be notified of GATT operations
     * @param scheduler         The RxBleRadio scheduler used to asObservable operation (currently {@link AndroidSchedulers#mainThread()}
     * @throws Throwable Any exception that your custom operation might throw
     */
    @NonNull
    Observable<T> asObservable(BluetoothGatt bluetoothGatt,
                               RxBleGattCallback rxBleGattCallback,
                               Scheduler scheduler) throws Throwable;
}