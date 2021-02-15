package com.polidea.rxandroidble3.exceptions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.polidea.rxandroidble3.internal.logger.LoggerUtil;
import com.polidea.rxandroidble3.utils.GattStatusParser;

/**
 * Exception emitted when the BLE link has been disconnected either when the connection was already established
 * or was in pending connection state. This state is expected when the connection was released as a
 * part of expected behavior (with {@link android.bluetooth.BluetoothGatt#GATT_SUCCESS} state).
 *
 * @see com.polidea.rxandroidble3.RxBleDevice#establishConnection(boolean)
 */
public class BleDisconnectedException extends BleException {

    /**
     * Set when the state is not available, for example when the adapter has been switched off.
     */
    public static final int UNKNOWN_STATUS = -1;

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public final String bluetoothDeviceAddress;
    public final int state;

    public static BleDisconnectedException adapterDisabled(String macAddress) {
        return new BleDisconnectedException(new BleAdapterDisabledException(), macAddress, UNKNOWN_STATUS);
    }

    /**
     * @deprecated In general, there's no place in a public API that requires you to instantiate this exception directly.
     * If you use it anyway, please switch to {@link #BleDisconnectedException(String, int)}
     */
    @Deprecated
    public BleDisconnectedException() {
        this("", UNKNOWN_STATUS);
    }

    /**
     * @deprecated In general, there's no place in a public API that requires you to instantiate this exception directly.
     * If you use it anyway, please switch to {@link #BleDisconnectedException(Throwable, String, int)}
     */
    @Deprecated
    public BleDisconnectedException(Throwable throwable, @NonNull String bluetoothDeviceAddress) {
        this(throwable, bluetoothDeviceAddress, UNKNOWN_STATUS);
    }

    /**
     * @deprecated In general, there's no place in a public API that requires you to instantiate this exception directly.
     * If you use it anyway, please switch to {@link #BleDisconnectedException(String, int)} or don't use it.
     */
    @Deprecated
    public BleDisconnectedException(@NonNull String bluetoothDeviceAddress) {
        this(bluetoothDeviceAddress, UNKNOWN_STATUS);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public BleDisconnectedException(Throwable throwable, @NonNull String bluetoothDeviceAddress, int status) {
        super(createMessage(bluetoothDeviceAddress, status), throwable);
        this.bluetoothDeviceAddress = bluetoothDeviceAddress;
        this.state = status;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public BleDisconnectedException(@NonNull String bluetoothDeviceAddress, int status) {
        super(createMessage(bluetoothDeviceAddress, status));
        this.bluetoothDeviceAddress = bluetoothDeviceAddress;
        this.state = status;
    }

    private static String createMessage(@Nullable String bluetoothDeviceAddress, int status) {
        final String gattCallbackStatusDescription = GattStatusParser.getGattCallbackStatusDescription(status);
        return "Disconnected from " + LoggerUtil.commonMacMessage(bluetoothDeviceAddress) + " with status " + status + " ("
                + gattCallbackStatusDescription + ")";
    }
}
