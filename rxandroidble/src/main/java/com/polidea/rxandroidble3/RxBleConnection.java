package com.polidea.rxandroidble3;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.polidea.rxandroidble3.exceptions.BleCannotSetCharacteristicNotificationException;
import com.polidea.rxandroidble3.exceptions.BleCharacteristicNotFoundException;
import com.polidea.rxandroidble3.exceptions.BleConflictingNotificationAlreadySetException;
import com.polidea.rxandroidble3.exceptions.BleException;
import com.polidea.rxandroidble3.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble3.exceptions.BleGattException;
import com.polidea.rxandroidble3.exceptions.BleGattOperationType;
import com.polidea.rxandroidble3.internal.Priority;
import com.polidea.rxandroidble3.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble3.internal.operations.CharacteristicLongWriteOperation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;

/**
 * The BLE connection handle, supporting GATT operations. Operations are enqueued and the library makes sure that they are not
 * executed in the same time within the client instance.
 */
public interface RxBleConnection {

    /**
     * The overhead value that is subtracted from the amount of bytes available when writing to a characteristic.
     * The default MTU value on Android is 23 bytes which gives effectively 23 - GATT_WRITE_MTU_OVERHEAD = 20 bytes
     * available for payload.
     */
    int GATT_WRITE_MTU_OVERHEAD = 3;

    /**
     * The overhead value that is subtracted from the amount of bytes available when reading from a characteristic.
     * The default MTU value on Android is 23 bytes which gives effectively 23 - GATT_READ_MTU_OVERHEAD = 22 bytes
     * available for payload.
     */
    int GATT_READ_MTU_OVERHEAD = 1;

    /**
     * The minimum (default) value for MTU (Maximum Transfer Unit) used by a bluetooth connection.
     */
    int GATT_MTU_MINIMUM = 23;

    /**
     * The maximum supported value for MTU (Maximum Transfer Unit) used by a bluetooth connection on Android OS.
     * https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/android-5.1.0_r1/stack/include/gatt_api.h#119
     */
    int GATT_MTU_MAXIMUM = 517;

    /**
     * Description of correct values of connection priority
     */
    @Retention(RetentionPolicy.SOURCE)
    @RequiresApi(21 /* Build.VERSION_CODES.LOLLIPOP */)
    @IntDef({BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER,
            BluetoothGatt.CONNECTION_PRIORITY_BALANCED,
            BluetoothGatt.CONNECTION_PRIORITY_HIGH})
    @interface ConnectionPriority { }

    @Deprecated
    interface Connector {

        Single<RxBleConnection> prepareConnection(boolean autoConnect);
    }

    enum RxBleConnectionState {
        CONNECTING("CONNECTING"), CONNECTED("CONNECTED"), DISCONNECTED("DISCONNECTED"), DISCONNECTING("DISCONNECTING");

        private final String description;

        RxBleConnectionState(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return "RxBleConnectionState{" + description + '}';
        }
    }

    /**
     * The interface of a {@link CharacteristicLongWriteOperation} builder.
     */
    interface LongWriteOperationBuilder {

        /**
         * Setter for a byte array to write
         * This function MUST be called prior to {@link #build()}
         *
         * @param bytes the bytes to write
         * @return the LongWriteOperationBuilder
         */
        LongWriteOperationBuilder setBytes(@NonNull byte[] bytes);

        /**
         * Setter for a {@link UUID} of the {@link BluetoothGattCharacteristic} to write to
         * This function or {@link #setCharacteristic(BluetoothGattCharacteristic)} MUST be called prior to {@link #build()}
         *
         * @param uuid the UUID
         * @return the LongWriteOperationBuilder
         */
        LongWriteOperationBuilder setCharacteristicUuid(@NonNull UUID uuid);

        /**
         * Setter for a {@link BluetoothGattCharacteristic} to write to
         * This function or {@link #setCharacteristicUuid(UUID)} MUST be called prior to {@link #build()}
         *
         * @param bluetoothGattCharacteristic the BluetoothGattCharacteristic
         * @return the LongWriteOperationBuilder
         */
        LongWriteOperationBuilder setCharacteristic(@NonNull BluetoothGattCharacteristic bluetoothGattCharacteristic);

        /**
         * Setter for a maximum size of a byte array that may be write at once
         * If this is not specified - the default value of the connection's MTU is used
         *
         * @param maxBatchSize the maximum size of a byte array to write at once
         * @return the LongWriteOperationBuilder
         */
        LongWriteOperationBuilder setMaxBatchSize(@IntRange(from = 1, to = GATT_MTU_MAXIMUM - GATT_WRITE_MTU_OVERHEAD) int maxBatchSize);

        /**
         * Setter for a retry strategy in case something goes wrong when writing data. If any {@link BleException} is raised,
         * a {@link WriteOperationRetryStrategy.LongWriteFailure} object is emitted.
         * {@link WriteOperationRetryStrategy.LongWriteFailure} contains both the {@link BleException} and the batch number
         * for which the write request failed. The {@link WriteOperationRetryStrategy.LongWriteFailure} emitted by the
         * writeOperationRetryStrategy will be used to retry the specified batch number write request.
         * <br>
         * If this is not specified - if batch write fails, the long write operation is stopped and whole operation is interrupted.
         * <br>
         * It is expected that the Observable returned from the writeOperationRetryStrategy will emit exactly the same events as the source,
         * however you may delay them at your pace.
         *
         * @param writeOperationRetryStrategy the retry strategy
         * @return the LongWriteOperationBuilder
         */
        LongWriteOperationBuilder setWriteOperationRetryStrategy(@NonNull WriteOperationRetryStrategy writeOperationRetryStrategy);

        /**
         * Setter for a strategy used to mark batch write completed. Only after previous batch has finished, the next (if any left) can be
         * written.
         * If this is not specified - the next batch of bytes is written right after the previous one has finished.
         *
         * A bytes batch is a part (slice) of the original byte array to write. Imagine a byte array of {0, 1, 2, 3, 4} where the maximum
         * number of bytes that may be transmitted at once is 2. Then the original byte array will be transmitted in three batches:
         * {0, 1}, {2, 3}, {4}
         *
         * It is expected that the Observable returned from the writeOperationAckStrategy will emit exactly the same events as the source,
         * however you may delay them at your pace.
         *
         * @param writeOperationAckStrategy the function that acknowledges writing of the batch of bytes. It takes
         *                                  an {@link Observable} that emits a boolean value each time the byte array batch
         *                                  has finished to write. {@link Boolean#TRUE} means that there are more items in the buffer,
         *                                  {@link Boolean#FALSE} otherwise. If you want to delay the next batch use provided observable
         *                                  and add some custom behavior (delay, waiting for a message from the device, etc.)
         * @return the LongWriteOperationBuilder
         */
        LongWriteOperationBuilder setWriteOperationAckStrategy(@NonNull WriteOperationAckStrategy writeOperationAckStrategy);

        /**
         * Build function for the long write
         *
         * @return the Observable which will enqueue the long write operation when subscribed.
         */
        Observable<byte[]> build();
    }

    /**
     * Retry strategy allows retrying a long write operation. There are two supported scenarios:
     * - Once the failure happens you may re-emit the failure you've received, applying your own transformations like a delay or any other,
     * aiming to postpone the retry procedure.
     * - If that Observable calls {@code onComplete} or {@code onError} then {@code retry} will call
     * {@code onCompleted} or {@code onError} on the child subscription. The emission will be forwarded as an operation result.
     *
     * For general documentation related to retrying please refer to http://reactivex.io/documentation/operators/retry.html
     */
    interface WriteOperationRetryStrategy extends ObservableTransformer<WriteOperationRetryStrategy.LongWriteFailure,
            WriteOperationRetryStrategy.LongWriteFailure> {

        class LongWriteFailure {

            final int batchIndex;
            final BleGattException cause;

            /**
             * Default constructor
             *
             * @param batchIndex the zero-based batch index on which the write request failed
             * @param cause       the failed cause of the write request
             */
            public LongWriteFailure(int batchIndex, BleGattException cause) {
                this.batchIndex = batchIndex;
                this.cause = cause;
            }

            /**
             * Get the batch index of the failed write request
             *
             * @return the zero-based batch index
             */
            public int getBatchIndex() {
                return batchIndex;
            }

            /**
             * Get the failed cause of the write request
             *
             * @return a {@link BleGattException}
             */
            public BleGattException getCause() {
                return cause;
            }
        }
    }

    interface WriteOperationAckStrategy extends ObservableTransformer<Boolean, Boolean> {

    }

    /**
     * Performs GATT service discovery and emits discovered results. After service discovery you can walk through
     * {@link android.bluetooth.BluetoothGattService}s and {@link BluetoothGattCharacteristic}s.
     * <p>
     * Result of the discovery is cached internally so consecutive calls won't trigger BLE operation and can be
     * considered relatively lightweight.
     *
     * Uses default timeout of 20 seconds
     *
     * @return Observable emitting result a GATT service discovery.
     * @throws BleGattCannotStartException with {@link BleGattOperationType#SERVICE_DISCOVERY} type, when it wasn't possible to start
     *                                     the discovery for internal reasons.
     * @throws BleGattException            in case of GATT operation error with {@link BleGattOperationType#SERVICE_DISCOVERY} type.
     */
    Single<RxBleDeviceServices> discoverServices();

    /**
     * Performs GATT service discovery and emits discovered results. After service discovery you can walk through
     * {@link android.bluetooth.BluetoothGattService}s and {@link BluetoothGattCharacteristic}s.
     * <p>
     * Result of the discovery is cached internally so consecutive calls won't trigger BLE operation and can be
     * considered relatively lightweight.
     *
     * Timeouts after specified amount of time.
     *
     * @param timeout multiplier of TimeUnit after which the discovery will timeout in case of no return values
     * @param timeUnit TimeUnit for the timeout
     * @return Observable emitting result a GATT service discovery.
     * @throws BleGattCannotStartException with {@link BleGattOperationType#SERVICE_DISCOVERY} type, when it wasn't possible to start
     *                                     the discovery for internal reasons.
     * @throws BleGattException            in case of GATT operation error with {@link BleGattOperationType#SERVICE_DISCOVERY} type.
     */
    Single<RxBleDeviceServices> discoverServices(@IntRange(from = 1) long timeout, @NonNull TimeUnit timeUnit);

    /**
     * @see #setupNotification(UUID, NotificationSetupMode)  with default setup mode.
     */
    Observable<Observable<byte[]>> setupNotification(@NonNull UUID characteristicUuid);

    /**
     * @see #setupNotification(BluetoothGattCharacteristic, NotificationSetupMode) with default setup mode.
     */
    Observable<Observable<byte[]>> setupNotification(@NonNull BluetoothGattCharacteristic characteristic);

    /**
     * Setup characteristic notification in order to receive callbacks when given characteristic has been changed. Returned observable will
     * emit Observable<byte[]> once the notification setup has been completed. It is possible to setup more observables for the same
     * characteristic and the lifecycle of the notification will be shared among them.
     * <p>
     * Notification is automatically unregistered once this observable is unsubscribed.
     *
     * NOTE: due to stateful nature of characteristics if one will setupIndication() before setupNotification()
     * the notification will not be set up and will emit an BleCharacteristicNotificationOfOtherTypeAlreadySetException
     *
     * @param characteristicUuid Characteristic UUID for notification setup.
     * @param setupMode Configures how the notification is set up. For available modes see {@link NotificationSetupMode}.
     * @return Observable emitting another observable when the notification setup is complete.
     * @throws BleCharacteristicNotFoundException              if characteristic with given UUID hasn't been found.
     * @throws BleCannotSetCharacteristicNotificationException if setup process notification setup process fail. This may be an internal
     *                                                         reason or lack of permissions.
     * @throws BleConflictingNotificationAlreadySetException if indication is already setup for this characteristic
     */
    Observable<Observable<byte[]>> setupNotification(@NonNull UUID characteristicUuid, @NonNull NotificationSetupMode setupMode);

    /**
     * Setup characteristic notification in order to receive callbacks when given characteristic has been changed. Returned observable will
     * emit Observable<byte[]> once the notification setup has been completed. It is possible to setup more observables for the same
     * characteristic and the lifecycle of the notification will be shared among them.
     * <p>
     * Notification is automatically unregistered once this observable is unsubscribed.
     * <p>
     * NOTE: due to stateful nature of characteristics if one will setupIndication() before setupNotification()
     * the notification will not be set up and will emit an BleCharacteristicNotificationOfOtherTypeAlreadySetException
     * <p>
     * The characteristic can be retrieved from {@link com.polidea.rxandroidble3.RxBleDeviceServices} emitted from
     * {@link RxBleConnection#discoverServices()}
     *
     * @param characteristic Characteristic for notification setup.
     * @param setupMode Configures how the notification is set up. For available modes see {@link NotificationSetupMode}.
     * @return Observable emitting another observable when the notification setup is complete.
     * @throws BleCannotSetCharacteristicNotificationException if setup process notification setup process fail. This may be an internal
     *                                                         reason or lack of permissions.
     * @throws BleConflictingNotificationAlreadySetException if indication is already setup for this characteristic
     */
    Observable<Observable<byte[]>> setupNotification(@NonNull BluetoothGattCharacteristic characteristic,
                                                     @NonNull NotificationSetupMode setupMode);

    /**
     * @see #setupIndication(UUID, NotificationSetupMode) with default setup mode.
     */
    Observable<Observable<byte[]>> setupIndication(@NonNull UUID characteristicUuid);

    /**
     * @see #setupIndication(BluetoothGattCharacteristic, NotificationSetupMode) with default setup mode.
     */
    Observable<Observable<byte[]>> setupIndication(@NonNull BluetoothGattCharacteristic characteristic);

    /**
     * Setup characteristic indication in order to receive callbacks when given characteristic has been changed. Returned observable will
     * emit Observable<byte[]> once the indication setup has been completed. It is possible to setup more observables for the same
     * characteristic and the lifecycle of the indication will be shared among them.
     * <p>
     * Indication is automatically unregistered once this observable is unsubscribed.
     * <p>
     * NOTE: due to stateful nature of characteristics if one will setupNotification() before setupIndication()
     * the indication will not be set up and will emit an BleCharacteristicNotificationOfOtherTypeAlreadySetException
     *
     * @param characteristicUuid Characteristic UUID for indication setup.
     * @param setupMode Configures how the notification is set up. For available modes see {@link NotificationSetupMode}.
     * @return Observable emitting another observable when the indication setup is complete.
     * @throws BleCharacteristicNotFoundException              if characteristic with given UUID hasn't been found.
     * @throws BleCannotSetCharacteristicNotificationException if setup process indication setup process fail. This may be an internal
     *                                                         reason or lack of permissions.
     * @throws BleConflictingNotificationAlreadySetException if notification is already setup for this characteristic
     */
    Observable<Observable<byte[]>> setupIndication(@NonNull UUID characteristicUuid, @NonNull NotificationSetupMode setupMode);

    /**
     * Setup characteristic indication in order to receive callbacks when given characteristic has been changed. Returned observable will
     * emit Observable<byte[]> once the indication setup has been completed. It is possible to setup more observables for the same
     * characteristic and the lifecycle of the indication will be shared among them.
     * <p>
     * Indication is automatically unregistered once this observable is unsubscribed.
     * <p>
     * NOTE: due to stateful nature of characteristics if one will setupNotification() before setupIndication()
     * the indication will not be set up and will emit an BleCharacteristicNotificationOfOtherTypeAlreadySetException
     * <p>
     * The characteristic can be retrieved from {@link com.polidea.rxandroidble3.RxBleDeviceServices} emitted from
     * {@link RxBleConnection#discoverServices()}
     *
     * @param characteristic Characteristic for indication setup.
     * @param setupMode Configures how the notification is set up. For available modes see {@link NotificationSetupMode}.
     * @return Observable emitting another observable when the indication setup is complete.
     * @throws BleCannotSetCharacteristicNotificationException if setup process indication setup process fail. This may be an internal
     *                                                         reason or lack of permissions.
     * @throws BleConflictingNotificationAlreadySetException if notification is already setup for this characteristic
     */
    Observable<Observable<byte[]>> setupIndication(@NonNull BluetoothGattCharacteristic characteristic,
                                                   @NonNull NotificationSetupMode setupMode);

    /**
     * Convenience method for characteristic retrieval. First step is service discovery which is followed by service/characteristic
     * traversal. This is an alias to:
     * <ol>
     * <li>{@link #discoverServices()}
     * <li>{@link RxBleDeviceServices#getCharacteristic(UUID)}
     * </ol>
     *
     * @param characteristicUuid Requested characteristic UUID.
     * @return Observable emitting matching characteristic or error if hasn't been found.
     * @throws BleCharacteristicNotFoundException if characteristic with given UUID hasn't been found.
     */
    @Deprecated
    Single<BluetoothGattCharacteristic> getCharacteristic(@NonNull UUID characteristicUuid);

    /**
     * Performs GATT read operation on a characteristic with given UUID.
     *
     * @param characteristicUuid Requested characteristic UUID.
     * @return Observable emitting characteristic value or an error in case of failure.
     * @throws BleCharacteristicNotFoundException if characteristic with given UUID hasn't been found.
     * @throws BleGattCannotStartException        if read operation couldn't be started for internal reason.
     * @throws BleGattException                   if read operation failed
     */
    Single<byte[]> readCharacteristic(@NonNull UUID characteristicUuid);

    /**
     * Performs GATT read operation on a given characteristic.
     *
     * @param characteristic Requested characteristic.
     * @return Observable emitting characteristic value or an error in case of failure.
     * @throws BleGattCannotStartException        if read operation couldn't be started for internal reason.
     * @throws BleGattException                   if read operation failed
     * @see #getCharacteristic(UUID) to obtain the characteristic.
     * @see #discoverServices() to obtain the characteristic.
     */
    Single<byte[]> readCharacteristic(@NonNull BluetoothGattCharacteristic characteristic);

    /**
     * Performs GATT write operation on a characteristic with given UUID.
     *
     * @param characteristicUuid Requested characteristic UUID.
     * @return Observable emitting characteristic value after write or an error in case of failure.
     * @throws BleCharacteristicNotFoundException if characteristic with given UUID hasn't been found.
     * @throws BleGattCannotStartException        if write operation couldn't be started for internal reason.
     * @throws BleGattException                   if write operation failed
     */
    Single<byte[]> writeCharacteristic(@NonNull UUID characteristicUuid, @NonNull byte[] data);

    /**
     * Performs GATT write operation on a given characteristic.
     *
     * @param bluetoothGattCharacteristic Characteristic to write.
     * @param data the byte array to write
     * @return Observable emitting written data or an error in case of failure.
     * @throws BleGattCannotStartException if write operation couldn't be started for internal reason.
     * @throws BleGattException            if write operation failed
     * @see #getCharacteristic(UUID) to obtain the characteristic.
     * @see #discoverServices() to obtain the characteristic.
     */
    Single<byte[]> writeCharacteristic(@NonNull BluetoothGattCharacteristic bluetoothGattCharacteristic, @NonNull byte[] data);

    /**
     * Returns a LongWriteOperationBuilder used for creating atomic write operations divided into multiple writes.
     * This is useful when the BLE peripheral does NOT handle long writes on the firmware level (in which situation
     * a regular {@link #writeCharacteristic(UUID, byte[])} should be sufficient.
     *
     * @return the LongWriteOperationBuilder
     */
    LongWriteOperationBuilder createNewLongWriteBuilder();

    /**
     * Performs GATT read operation on a descriptor from a characteristic with a given UUID from a service with a given UUID.
     *
     * @param serviceUuid Requested {@link android.bluetooth.BluetoothGattService} UUID
     * @param characteristicUuid Requested {@link android.bluetooth.BluetoothGattCharacteristic} UUID
     * @param descriptorUuid Requested {@link android.bluetooth.BluetoothGattDescriptor} UUID
     * @return Observable emitting the descriptor value after read or an error in case of failure.
     * @throws BleGattCannotStartException if read operation couldn't be started for internal reason.
     * @throws BleGattException            if read operation failed
     * @see #getCharacteristic(UUID) to obtain the characteristic.
     * @see #discoverServices() to obtain the characteristic.
     */
    Single<byte[]> readDescriptor(@NonNull UUID serviceUuid, @NonNull UUID characteristicUuid, @NonNull UUID descriptorUuid);

    /**
     * Performs GATT read operation on a descriptor from a characteristic with a given UUID from a service with a given UUID.
     *
     * @param descriptor Requested {@link android.bluetooth.BluetoothGattDescriptor}
     * @return Observable emitting the descriptor value after read or an error in case of failure.
     * @throws BleGattCannotStartException if read operation couldn't be started for internal reason.
     * @throws BleGattException            if read operation failed
     * @see #getCharacteristic(UUID) to obtain the characteristic from which you can get the {@link BluetoothGattDescriptor}.
     * @see #discoverServices() to obtain the {@link BluetoothGattDescriptor}.
     */
    Single<byte[]> readDescriptor(@NonNull BluetoothGattDescriptor descriptor);

    /**
     * Performs GATT write operation on a descriptor from a characteristic with a given UUID from a service with a given UUID.
     *
     * @param serviceUuid Requested {@link android.bluetooth.BluetoothGattDescriptor} UUID
     * @param characteristicUuid Requested {@link android.bluetooth.BluetoothGattCharacteristic} UUID
     * @param descriptorUuid Requested {@link android.bluetooth.BluetoothGattDescriptor} UUID
     * @return Completable which completes after a successful write operation or an error in case of failure.
     * @throws BleGattCannotStartException if write operation couldn't be started for internal reason.
     * @throws BleGattException            if write operation failed
     */
    Completable writeDescriptor(@NonNull UUID serviceUuid, @NonNull UUID characteristicUuid,
                                       @NonNull UUID descriptorUuid, @NonNull byte[] data);

    /**
     * Performs GATT write operation on a given descriptor.
     *
     * @param descriptor Requested {@link android.bluetooth.BluetoothGattDescriptor}
     * @return Completable which completes after a successful write operation or an error in case of failure.
     * @throws BleGattCannotStartException if write operation couldn't be started for internal reason.
     * @throws BleGattException            if write operation failed
     * @see #getCharacteristic(UUID) to obtain the characteristic.
     * @see #discoverServices() to obtain the characteristic.
     */
    Completable writeDescriptor(@NonNull BluetoothGattDescriptor descriptor, @NonNull byte[] data);

    /**
     * Performs a GATT request connection priority operation, which requests a connection parameter
     * update on the remote device. NOTE: peripheral may silently decline request.
     * <p>
     * Tells Android to request an update of connection interval and slave latency parameters.
     * Using {@link BluetoothGatt#CONNECTION_PRIORITY_HIGH} will increase transmission speed and
     * battery drainage, if accepted by the device, compared to {@link BluetoothGatt#CONNECTION_PRIORITY_BALANCED},
     * while using {@link BluetoothGatt#CONNECTION_PRIORITY_LOW_POWER} will cause higher latencies
     * and save battery, if accepted by the device, compared to {@link BluetoothGatt#CONNECTION_PRIORITY_BALANCED}.
     * <p>
     * By default connection is balanced.
     * <p>
     * NOTE: Till API 26 (8.0) there was no method like `BluetoothGattCallback.onConnectionPriorityChanged()`. It was not possible to know
     * if the request was successful (accepted by the peripheral). This also causes the need of specifying when the request is considered
     * finished (parameter delay and timeUnit). Since API 26 the mentioned callback is hidden, yet possible to use. It is not used to
     * automatically complete this request due to Android OS changing connection parameters on its own. It is not possible to determine
     * which callback is actually finishing the request nor if the Android OS will not change the parameters right after the request.
     * If access to the callback is a must for your implementation you may achieve it by
     * {@link RxBleGattCallback#setHiddenNativeCallback(HiddenBluetoothGattCallback)} via {@link #queue(RxBleCustomOperation)} and create
     * a custom request connection priority operation.
     * <p>
     * As of Lollipop the connection parameters are:
     * * {@link BluetoothGatt#CONNECTION_PRIORITY_BALANCED}: min interval 30 ms, max interval 50 ms, slave latency 0
     * * {@link BluetoothGatt#CONNECTION_PRIORITY_HIGH}: min interval 7.5 ms, max interval 10ms, slave latency 0
     * * {@link BluetoothGatt#CONNECTION_PRIORITY_LOW_POWER}: min interval 100ms, max interval 125 ms, slave latency 2
     * <p>
     * Returned completable completes after the specified delay if and only if
     * {@link BluetoothGatt#requestConnectionPriority(int)} has returned true.
     *
     * @param connectionPriority requested connection priority
     * @param delay              delay after which operation is assumed to be successful (must be shorter than 30 seconds)
     * @param timeUnit           time unit of the delay
     * @return Completable which finishes after calling the request and the specified delay
     * @throws BleGattCannotStartException with {@link BleGattOperationType#CONNECTION_PRIORITY_CHANGE} type
     *                                     if requested operation returned false or threw exception
     * @throws IllegalArgumentException    in case of invalid connection priority or delay
     */
    @RequiresApi(21 /* Build.VERSION_CODES.LOLLIPOP */)
    Completable requestConnectionPriority(
            @ConnectionPriority int connectionPriority,
            @IntRange(from = 1) long delay,
            @NonNull TimeUnit timeUnit
    );

    /**
     * Allows observing of connection parameters updates. This is part of Android's hidden API and therefore is not guaranteed to work.
     * It was added in API 26 (8.0, Oreo) in {@link android.bluetooth.BluetoothGattCallback} and will not work on lower API levels at all.
     * The system does change the parameters on its own at the beginning of connection (i.e. to speed up service discovery process).
     * The parameters may be further changed by using {@link #requestConnectionPriority(int, long, TimeUnit)}.
     *
     * @return Observable which may emit updates of the connection parameters
     */
    @RequiresApi(26 /* Build.VERSION_CODES.O */)
    Observable<ConnectionParameters> observeConnectionParametersUpdates();

    /**
     * Performs GATT read rssi operation.
     *
     * @return Observable emitting the read RSSI value
     */
    Single<Integer> readRssi();

    /**
     * Performs GATT MTU (Maximum Transfer Unit) request.
     *
     * Timeouts after 10 seconds.
     *
     * @return Observable emitting result the MTU requested.
     * @throws BleGattCannotStartException with {@link BleGattOperationType#ON_MTU_CHANGED} type, when it wasn't possible to set
     *                                     the MTU for internal reasons.
     * @throws BleGattException            in case of GATT operation error with {@link BleGattOperationType#ON_MTU_CHANGED} type.
     */
    @RequiresApi(21 /* Build.VERSION_CODES.LOLLIPOP */)
    Single<Integer> requestMtu(@IntRange(from = GATT_MTU_MINIMUM, to = GATT_MTU_MAXIMUM) int mtu);

    /**
     * Get currently negotiated MTU value. On pre-lollipop Android versions it will always return 23.
     *
     * @return currently negotiated MTU value.
     */
    int getMtu();

    /**
     * <b>This method requires deep knowledge of RxAndroidBLE internals. Use it only as a last resort if you know
     * what your are doing.</b>
     * <p>
     * Queue an operation for future execution. The method accepts a {@link RxBleCustomOperation} concrete implementation
     * and will queue it inside connection operation queue. When ready to execute, the {@link Observable} returned
     * by the {@link RxBleCustomOperation#asObservable(BluetoothGatt, RxBleGattCallback, Scheduler)} will be
     * subscribed to.
     * <p>
     * Every event emitted by the {@link Observable} returned by
     * {@link RxBleCustomOperation#asObservable(BluetoothGatt, RxBleGattCallback, Scheduler)} will be forwarded
     * to the {@link Observable} returned by this method.
     * <p>
     * You <b>must</b> ensure the custom operation's {@link Observable} does terminate either via {@code onCompleted}
     * or {@code onError(Throwable)}. Otherwise, the internal queue orchestrator will wait forever for
     * your {@link Observable} to complete. Normal queue processing will be resumed after the {@link Observable}
     * returned by {@link RxBleCustomOperation#asObservable(BluetoothGatt, RxBleGattCallback, Scheduler)}
     * completes.
     * <p>
     * The operation will be added to the queue using a {@link Priority#NORMAL}
     * priority.
     *
     * @param operation The custom operation to queue.
     * @param <T>       The type returned by the {@link RxBleCustomOperation} instance.
     * @return Observable emitting the value after execution or an error in case of failure.
     */
    <T> Observable<T> queue(@NonNull RxBleCustomOperation<T> operation);

    /**
     * <b>This method requires deep knowledge of RxAndroidBLE internals. Use it only as a last resort if you know
     * what your are doing.</b>
     * <p>
     * Queue an operation for future execution. The method accepts a {@link RxBleCustomOperation} concrete implementation
     * and will queue it inside connection operation queue. When ready to execute, the {@link Observable} returned
     * by the {@link RxBleCustomOperation#asObservable(BluetoothGatt, RxBleGattCallback, Scheduler)} will be
     * subscribed to.
     * <p>
     * Every event emitted by the {@link Observable} returned by
     * {@link RxBleCustomOperation#asObservable(BluetoothGatt, RxBleGattCallback, Scheduler)} will be forwarded
     * to the {@link Observable} returned by this method.
     * <p>
     * You <b>must</b> ensure the custom operation's {@link Observable} does terminate either via {@code onCompleted}
     * or {@code onError(Throwable)}. Otherwise, the internal queue orchestrator will wait forever for
     * your {@link Observable} to complete. Normal queue processing will be resumed after the {@link Observable}
     * returned by {@link RxBleCustomOperation#asObservable(BluetoothGatt, RxBleGattCallback, Scheduler)}
     * completes.
     * <p>
     *
     * @param operation The custom operation to queue.
     * @param <T>       The type returned by the {@link RxBleCustomOperation} instance.
     * @param priority  Priority affected to this operation
     * @return Observable emitting the value after execution or an error in case of failure.
     */
    <T> Observable<T> queue(@NonNull RxBleCustomOperation<T> operation, Priority priority);

}
