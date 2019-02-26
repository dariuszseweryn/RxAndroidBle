package com.polidea.rxandroidble2.internal.logger;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import com.polidea.rxandroidble2.LogConstants;
import com.polidea.rxandroidble2.internal.RxBleLog;
import com.polidea.rxandroidble2.internal.operations.Operation;
import java.util.UUID;

public class LoggerUtil {

    private LoggerUtil() {
    }

    private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        int byteArrayLength = bytes.length;

        if (byteArrayLength == 0) {
            return "[]";
        }

        int byteCharsLength = byteArrayLength * 2;
        int delimiterCount = (byteArrayLength - 1);
        int delimiterCharsLength = delimiterCount * 2;
        int arrayBorderLength = 2;

        char[] hexChars = new char[byteCharsLength + delimiterCharsLength + arrayBorderLength];

        for (int j = 0; j < byteArrayLength; j++) {
            int v = bytes[j] & 0xFF;
            int i = j * 2 + 1 /* start of array */ + j * 2 /* delimiters */;
            hexChars[i] = HEX_ARRAY[v >>> 4];
            hexChars[i + 1] = HEX_ARRAY[v & 0x0F];
        }

        for (int j = 0; j < byteArrayLength - 1; j++) {
            int i = j * 2 + 1 /* start of array */ + j * 2 /* delimiters */ + 2 /* initial hex chars */;
            hexChars[i] = ',';
            hexChars[i + 1] = ' ';
        }

        hexChars[0] = '[';
        hexChars[hexChars.length - 1] = ']';

        return new String(hexChars);
    }

    public static void logOperationStarted(Operation operation) {
        if (RxBleLog.isAtLeast(LogConstants.DEBUG)) {
            RxBleLog.d("STARTED  %s(%d)", operation.getClass().getSimpleName(), System.identityHashCode(operation));
        }
    }

    public static void logOperationRemoved(Operation operation) {
        if (RxBleLog.isAtLeast(LogConstants.DEBUG)) {
            RxBleLog.d("REMOVED  %s(%d)", operation.getClass().getSimpleName(), System.identityHashCode(operation));
        }
    }

    public static void logOperationQueued(Operation operation) {
        if (RxBleLog.isAtLeast(LogConstants.DEBUG)) {
            RxBleLog.d("QUEUED   %s(%d)", operation.getClass().getSimpleName(), System.identityHashCode(operation));
        }
    }

    public static void logOperationFinished(Operation operation, long startTime, long endTime) {
        if (RxBleLog.isAtLeast(LogConstants.DEBUG)) {
            RxBleLog.d("FINISHED %s(%d) in %d ms", operation.getClass().getSimpleName(),
                    System.identityHashCode(operation), (endTime - startTime));
        }
    }

    public static void logOperationSkippedBecauseDisposedWhenAboutToRun(Operation operation) {
        if (RxBleLog.isAtLeast(LogConstants.VERBOSE)) {
            RxBleLog.v("SKIPPED  %s(%d) just before running — is disposed", operation.getClass().getSimpleName(),
                    System.identityHashCode(operation));
        }
    }

    public static void logCallback(String callbackName, BluetoothGatt gatt, int status, BluetoothGattCharacteristic characteristic,
                                   boolean valueMatters) {
        if (!RxBleLog.isAtLeast(LogConstants.INFO)) {
            return;
        }
        AttributeLogWrapper value = new AttributeLogWrapper(characteristic.getUuid(), characteristic.getValue(), valueMatters);
        RxBleLog.i(commonCallbackMessage() + commonStatusMessage() + commonValueMessage(),
                gatt.getDevice().getAddress(), callbackName, status, value);
    }

    public static void logCallback(String callbackName, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                   boolean valueMatters) {
        if (!RxBleLog.isAtLeast(LogConstants.INFO)) {
            return;
        }
        AttributeLogWrapper value = new AttributeLogWrapper(characteristic.getUuid(), characteristic.getValue(), valueMatters);
        RxBleLog.i(commonCallbackMessage() + commonValueMessage(), gatt.getDevice().getAddress(), callbackName, value);
    }

    public static void logCallback(String callbackName, BluetoothGatt gatt, int status, BluetoothGattDescriptor descriptor,
                                   boolean valueMatters) {
        if (!RxBleLog.isAtLeast(LogConstants.INFO)) {
            return;
        }
        AttributeLogWrapper value = new AttributeLogWrapper(descriptor.getUuid(), descriptor.getValue(), valueMatters);
        RxBleLog.i(commonCallbackMessage() + commonStatusMessage() + commonValueMessage(),
                gatt.getDevice().getAddress(), callbackName, status, value);
    }

    public static void logCallback(String callbackName, BluetoothGatt gatt, int status) {
        if (!RxBleLog.isAtLeast(LogConstants.INFO)) {
            return;
        }
        RxBleLog.i(commonCallbackMessage() + commonStatusMessage(), gatt.getDevice().getAddress(), callbackName, status);
    }

    public static void logCallback(String callbackName, BluetoothGatt gatt, int status, int value) {
        if (!RxBleLog.isAtLeast(LogConstants.INFO)) {
            return;
        }
        RxBleLog.i(commonCallbackMessage() + commonStatusMessage() + commonValueMessage(),
                gatt.getDevice().getAddress(), callbackName, status, value);
    }

    private static String commonCallbackMessage() {
        return "(MAC: '%s') %24s()";
    }

    private static String commonStatusMessage() {
        return ", status=%d";
    }

    private static String commonValueMessage() {
        return ", value=%s";
    }

    static class AttributeLogWrapper {

        private final UUID uuid;
        private final byte[] value;
        private final boolean valueMatters;

        AttributeLogWrapper(UUID uuid, byte[] value, boolean valueMatters) {
            this.uuid = uuid;
            this.value = value;
            this.valueMatters = valueMatters;
        }

        @Override
        public String toString() {
            return "[uuid='" + getUuid()
                    + (valueMatters ? ("', hexValue=" + getValue()) : "'")
                    + ']';
        }

        private String getUuid() {
            int uuidLogSetting = RxBleLog.getUuidLogSetting();
            if (uuidLogSetting == LogConstants.UUIDS_FULL) {
                return uuid.toString();
            }
            return "...";
        }

        private String getValue() {
            if (RxBleLog.getShouldLogAttributeValues()) {
                return bytesToHex(value);
            }
            return "[...]";
        }
    }
}
