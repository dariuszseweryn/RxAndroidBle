package com.polidea.rxandroidble3;

import android.bluetooth.BluetoothGatt;
import androidx.annotation.NonNull;

import com.polidea.rxandroidble3.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble3.internal.serialization.ConnectionOperationQueue;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Scheduler;

/**
 * Represents a custom operation that will be enqueued for future execution within the client instance.
 */
public interface RxBleCustomOperation<T> {

    /**
     * Return an observable that implement a custom operation using low-level Android BLE API.
     * <p>
     * The {@link Observable} returned by this method will be subscribed to by the {@link ConnectionOperationQueue}
     * when it determines that the custom operation should be the next to be run.
     * <p>
     * The method receives everything needed to access the low-level Android BLE API objects mainly the
     * {@link BluetoothGatt} to interact with Android BLE GATT operations and {@link RxBleGattCallback}
     * to be notified when GATT operations completes.
     * <p>
     * Every event emitted by the returned {@link Observable} will be forwarded to the observable
     * returned by {@link RxBleConnection#queue(RxBleCustomOperation)}
     * <p>
     * As the implementer, your contract is to return an {@link Observable} that completes at some
     * point in time. When the returned observable terminates, either via the {@link Observer#onComplete()} or
     * {@link Observer#onError(Throwable)} callback, the {@link ConnectionOperationQueue} queue's lock is released so that
     * queue operations can continue.
     * <p>
     * You <b>must</b> ensure the returned {@link Observable} do terminate either via {@code onCompleted}
     * or {@code onError(Throwable)}. Otherwise, the internal queue orchestrator will wait forever for
     * your {@link Observable} to complete and the it will not continue to process queued operations.
     *
     * @param bluetoothGatt     The Android API GATT instance
     * @param rxBleGattCallback The internal Rx ready bluetooth gatt callback to be notified of GATT operations
     * @param scheduler         The ClientOperationQueue scheduler used to asObservable operation
     * @throws Throwable Any exception that your custom operation might throw
     */
    @NonNull
    Observable<T> asObservable(BluetoothGatt bluetoothGatt,
                               RxBleGattCallback rxBleGattCallback,
                               Scheduler scheduler) throws Throwable;
}