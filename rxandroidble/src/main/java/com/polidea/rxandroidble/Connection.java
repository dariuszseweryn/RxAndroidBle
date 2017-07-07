package com.polidea.rxandroidble;


import android.bluetooth.BluetoothGatt;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import com.polidea.rxandroidble.setup.NotificationSetup;
import com.polidea.rxandroidble.setup.DiscoverySetup;
import com.polidea.rxandroidble.setup.WriteSetup;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import rx.Completable;
import rx.Observable;
import rx.Single;

public interface Connection {

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
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @IntDef({BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER,
            BluetoothGatt.CONNECTION_PRIORITY_BALANCED,
            BluetoothGatt.CONNECTION_PRIORITY_HIGH})
    @interface ConnectionPriority {

    }

    Single<RxBleDeviceServices> discoverServices();

    Single<RxBleDeviceServices> discoverServices(@NonNull DiscoverySetup discoverySetup);

    Single<byte[]> readCharacteristic(@NonNull Characteristic characteristic);

    Completable writeCharacteristic(@NonNull Characteristic characteristic, @NonNull byte[] value);

    Completable writeCharacteristic(@NonNull Characteristic characteristic, @NonNull byte[] value, @NonNull WriteSetup writeSetup);

    Observable<Observable<byte[]>> setupNotifcation(@NonNull Characteristic characteristic);

    Observable<Observable<byte[]>> setupNotifcation(@NonNull Characteristic characteristic, @NonNull NotificationSetup notificationSetup);

    Single<byte[]> readDescriptor(@NonNull Descriptor descriptor);

    Completable writeDescriptor(@NonNull Descriptor descriptor, @NonNull byte[] value);

    Single<Integer> readRssi();

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    Single<Integer> requestMtu(@IntRange(from = GATT_MTU_MINIMUM, to = GATT_MTU_MAXIMUM) int mtu);

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    Observable<Integer> observeMtuChanges();

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    int getMtu();

    /*
     * [DS] 07.07.2017 It is observed that BluetoothGatt.requestConnectionPriority() does not block other executions the same as other calls
     * Because of that it is not needed to keep the timeout. This API should be compatible with the incoming Android O.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    Single<ConnectionParams> requestConnectionPriority(@Connection.ConnectionPriority int connectionPriority);

//    [DS] 07.07.2017 Preparation for the new BluetoothGattCallback.onConnectionUpdated()
//    @RequiresApi(api = Build.VERSION_CODES.O)
//    Observable<ConnectionParams> observeConnectionParams();

    <T> Observable<T> queue(@NonNull CustomOperation<T> operation);
}
