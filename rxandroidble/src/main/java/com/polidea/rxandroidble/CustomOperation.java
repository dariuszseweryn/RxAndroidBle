package com.polidea.rxandroidble;


import android.bluetooth.BluetoothGatt;
import com.polidea.rxandroidble.internal.RadioReleaseInterface;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Represents a custom operation that will be enqueued for future execution within the client instance.
 */
interface CustomOperation<T> {

    /**
     * Return an observable that implement a custom radio operation using low-level Android BLE API.
     * <p>
     * The {@link Observable} returned by this method will be subscribed to by the {@link RxBleRadio}
     * when it determines that the custom operation should be the next to be run.
     * <p>
     * The method receives everything needed to access the low-level Android BLE API objects mainly the
     * {@link BluetoothGatt} to interact with Android BLE GATT operations and {@link RxBleGattCallback}
     * to be notified when GATT operations completes.
     * <p>
     * Every event emitted by the returned {@link Observable} will be forwarded to the observable
     * returned by {@link Connection#queue(CustomOperation)}
     * <p>
     * As the implementer, your contract is to return an {@link Observable} that completes at some
     * point in time. When the returned observable terminates, either via the {@link Observer#onCompleted()} or
     * {@link Observer#onError(Throwable)} callback, the {@link RxBleRadio} queue's lock is released so that
     * queued operations can continue.
     * <p>
     * You <b>must</b> ensure the returned {@link Observable} do terminate either via {@code onCompleted}
     * or {@code onError(Throwable)}. Otherwise, the internal queue orchestrator will wait forever for
     * your {@link Observable} to complete and the it will not continue to process queued operations.
     * <p>
     * If this operation will be unsubscribed before {@code onCompleted} or {@code onError(Throwable)} will get called:<p>
     *      1. the operation should not make any more calls to {@link BluetoothGatt}<p>
     *      2. the operation should wait until all calls to the {@link BluetoothGatt} will finish — i.e. if called
     * {@link BluetoothGatt#writeCharacteristic(android.bluetooth.BluetoothGattCharacteristic)} wait for the emission from
     * {@link RxBleGattCallback#getOnCharacteristicWrite()} — if this requirement will not be fulfilled subsequent calls
     * to {@link BluetoothGatt} made by other operations will be rejected until the previous call will finish.<p>
     *      3. after all calls to {@link BluetoothGatt} will finish the operation must release the radio by calling
     * {@link RadioReleaseInterface#release()}
     *
     * @param bluetoothGatt         The Android API GATT instance
     * @param rxBleGattCallback     The internal Rx ready bluetooth gatt callback to be notified of GATT operations
     * @param scheduler             The RxBleRadio scheduler used to asObservable operation
     *                              (currently {@link AndroidSchedulers#mainThread()}
     * @param radioReleaseInterface The interface to call when normal operation of the library should be restored
     * @throws Throwable Any exception that your custom operation might throw
     */
    Observable<T> asObservable(BluetoothGatt bluetoothGatt,
                               RxBleGattCallback rxBleGattCallback,
                               Scheduler scheduler,
                               RadioReleaseInterface radioReleaseInterface) throws Throwable;
}
