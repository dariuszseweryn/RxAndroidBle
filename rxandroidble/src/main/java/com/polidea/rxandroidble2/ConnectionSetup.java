package com.polidea.rxandroidble2;

import android.bluetooth.BluetoothGattCallback;
import android.content.Context;

import com.polidea.rxandroidble2.internal.BleIllegalOperationException;

import java.util.concurrent.TimeUnit;

/**
 * Container for various connection parameters.
 */
public class ConnectionSetup {
    /** Durations after which Android would timeout internally (in direct mode) */
    public static final int DEFAULT_OPERATION_TIMEOUT = 30;
    public static final int DEFAULT_CONNECTING_TIMEOUT = 35;
    /**
     * Flag related with
     * {@link android.bluetooth.BluetoothDevice#connectGatt(Context, boolean, BluetoothGattCallback)} autoConnect flag.
     * In case of auto connect is enabled the observable will wait with the emission of RxBleConnection. Without
     * auto connect flag set to true the connection will fail
     * with {@link com.polidea.rxandroidble2.exceptions.BleGattException} if the device is not in range.
     */
    public final boolean autoConnect;
    /**
     * Flag describing the method of operation viability checking. If set to false,
     * a {@link BleIllegalOperationException} will be thrown everytime properties of the characteristic
     * don't match those required by the operation. If set to true, an event will be logged without interrupting the execution.
     */
    public final boolean suppressOperationCheck;
    /**
     * Timeout in seconds after which the operation will be considered as broken. Eventually the operation will be
     * canceled and removed from queue.
     */
    public final Timeout operationTimeout;
    /**
     * Timeout in seconds after which the connection will be considered as broken. Eventually the connection will be
     * canceled and removed from queue.
     */
    public final Timeout connectingTimeout;

    private ConnectionSetup(boolean autoConnect, boolean suppressOperationCheck, Timeout operationTimeout,
                            Timeout connectingTimeout) {
        this.autoConnect = autoConnect;
        this.suppressOperationCheck = suppressOperationCheck;
        this.operationTimeout = operationTimeout;
        this.connectingTimeout = connectingTimeout;
    }

    public static class Builder {

        private boolean autoConnect = false;
        private boolean suppressOperationCheck = false;
        private Timeout operationTimeout = new Timeout(DEFAULT_OPERATION_TIMEOUT, TimeUnit.SECONDS);
        private Timeout connectingTimeout = new Timeout(DEFAULT_CONNECTING_TIMEOUT, TimeUnit.SECONDS);

        /**
         * Autoconnect concept may be misleading at first glance. In cases when the BLE device is available and it is advertising constantly
         * you won't need to use autoconnect. Use autoconnect for connections where the BLE device is not advertising at
         * the moment of {@link RxBleDevice#establishConnection(ConnectionSetup)} call.
         *
         * @param autoConnect Flag related to
         *                    {@link android.bluetooth.BluetoothDevice#connectGatt(Context, boolean, BluetoothGattCallback)} autoConnect
         *                    flag. In case of auto connect is enabled the observable will wait with the emission of RxBleConnection.
         *                    Without auto connect flag set to true the connection will fail
         *                    with {@link com.polidea.rxandroidble2.exceptions.BleGattException} if the device is not in range after a
         *                    30 seconds timeout.
         * @return this builder instance
         */
        public Builder setAutoConnect(boolean autoConnect) {
            this.autoConnect = autoConnect;
            return this;
        }

        /**
         * @param suppressOperationCheck Flag describing the method of operation viability checking. If set to false,
         *                               a {@link BleIllegalOperationException} will be
         *                               emitted every time properties of the characteristic don't match those required by the operation.
         *                               If set to true, an event will be logged without interrupting the execution.
         * @return this builder instance
         */
        public Builder setSuppressIllegalOperationCheck(boolean suppressOperationCheck) {
            this.suppressOperationCheck = suppressOperationCheck;
            return this;
        }

        /**
         * @param operationTimeout Timeout after which the operation will be considered as broken. Eventually the operation
         *                         will be canceled and removed from queue. Keep in mind that it will cancel the library's operation
         *                         only and may leave Android's BLE stack in an inconsistent state.
         * @return this builder instance
         */
        public Builder setOperationTimeout(Timeout operationTimeout) {
            this.operationTimeout = operationTimeout;
            return this;
        }

        /**
         * @param connectingTimeout Timeout after which the connection will be considered as broken. Eventually the connection
         *                         will be canceled and removed from queue. Keep in mind that it will cancel the library's connection
         *                         only and may leave Android's BLE stack in an inconsistent state.
         * @return this builder instance
         */
        public Builder setConnectingTimeout(Timeout connectingTimeout) {
            this.connectingTimeout = connectingTimeout;
            return this;
        }

        public ConnectionSetup build() {
            return new ConnectionSetup(autoConnect, suppressOperationCheck, operationTimeout, connectingTimeout);
        }
    }
}
