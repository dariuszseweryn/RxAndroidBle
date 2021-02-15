package com.polidea.rxandroidble3.internal.serialization;

import com.polidea.rxandroidble3.internal.operations.Operation;

import io.reactivex.rxjava3.core.Observable;

/**
 * Interface used for serialization of {@link Operation} execution.
 *
 * Native Android BLE API is asynchronous but does not queue operations on it's own. Operations like scanning, connecting, reading, writing
 * in order to be successfully started need to be serialized at different levels.
 * <br>
 * i.e. When dealing with a {@link android.bluetooth.BluetoothGatt} each read and write needs to be synchronized but changing connection
 * priority does not.
 * <br>
 * i.e.2 When starting to connect the {@link android.bluetooth.BluetoothGatt} the Android Stack does queue direct connections internally
 * but due to a bug the callback may not be called — serializing connection establishment does allow for proper timeout management in this
 * case
 */
public interface ClientOperationQueue {

    /**
     * Function that queues an {@link Operation} for execution.
     * @param operation the operation to execute
     * @param <T> type of the operation values
     * @return the observable representing the operation execution
     */
    <T> Observable<T> queue(Operation<T> operation);
}
