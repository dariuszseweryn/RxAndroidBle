package com.polidea.rxandroidble;

import android.bluetooth.BluetoothGattCallback;
import android.content.Context;

/**
 * Container for various connection parameters.
 */
public class ConnectionSetup {
    /**
     * Marker related with
     * {@link android.bluetooth.BluetoothDevice#connectGatt(Context, boolean, BluetoothGattCallback)} autoConnect flag.
     * In case of auto connect is enabled the observable will wait with the emission of RxBleConnection. Without
     * auto connect flag set to true the connection will fail
     * with {@link com.polidea.rxandroidble.exceptions.BleGattException} if the device is not in range.
     */
    public final boolean autoConnect;
    /**
     * Flag describing the method of operation viability checking. If set to false,
     * a {@link com.polidea.rxandroidble.exceptions.BleIllegalOperationException} will be thrown everytime properties of the characteristic
     * don't match those required by the operation. If set to true, an event will be logged without interrupting the execution.
     */
    public final boolean suppressOperationCheck;

    private ConnectionSetup(boolean autoConnect, boolean suppressOperationCheck) {
        this.autoConnect = autoConnect;
        this.suppressOperationCheck = suppressOperationCheck;
    }

    public static class Builder {

        private boolean autoConnect = false;
        private boolean suppressOperationCheck = false;


        /**
         * @param autoConnect Marker related with
         *                    {@link android.bluetooth.BluetoothDevice#connectGatt(Context, boolean, BluetoothGattCallback)} autoConnect
         *                    flag. In case of auto connect is enabled the observable will wait with the emission of RxBleConnection.
         *                    Without auto connect flag set to true the connection will fail
         *                    with {@link com.polidea.rxandroidble.exceptions.BleGattException} if the device is not in range.
         * @return this builder instance
         */
        public Builder setAutoConnect(boolean autoConnect) {
            this.autoConnect = autoConnect;
            return this;
        }

        /**
         * @param suppressOperationCheck Flag describing the method of operation viability checking. If set to false,
         *                               a {@link com.polidea.rxandroidble.exceptions.BleIllegalOperationException} will be
         *                               thrown everytime properties of the characteristic don't match those required by the operation.
         *                               If set to true, an event will be logged without interrupting the execution.
         * @return this builder instance
         */
        public Builder setSuppressIllegalOperationCheck(boolean suppressOperationCheck) {
            this.suppressOperationCheck = suppressOperationCheck;
            return this;
        }

        public ConnectionSetup build() {
            return new ConnectionSetup(autoConnect, suppressOperationCheck);
        }
    }
}
