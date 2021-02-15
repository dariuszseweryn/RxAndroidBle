package com.polidea.rxandroidble2.internal.logger;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import com.polidea.rxandroidble2.LogConstants;
import com.polidea.rxandroidble2.internal.RxBleLog;
import com.polidea.rxandroidble2.internal.operations.Operation;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class LoggerUtil {

    private LoggerUtil() {
    }

    private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";

        if (!RxBleLog.getShouldLogAttributeValues()) {
            return "[...]";
        }

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

    public static void logOperationRunning(Operation operation) {
        RxBleLog.i("RUNNING  %s", operation);
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
        RxBleLog.i(commonMacMessage(gatt) + commonCallbackMessage() + commonStatusMessage() + commonValueMessage(),
                callbackName, status, value);
    }

    public static void logCallback(String callbackName, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                   boolean valueMatters) {
        if (!RxBleLog.isAtLeast(LogConstants.INFO)) {
            return;
        }
        AttributeLogWrapper value = new AttributeLogWrapper(characteristic.getUuid(), characteristic.getValue(), valueMatters);
        RxBleLog.i(commonMacMessage(gatt) + commonCallbackMessage() + commonValueMessage(), callbackName, value);
    }

    public static void logCallback(String callbackName, BluetoothGatt gatt, int status, BluetoothGattDescriptor descriptor,
                                   boolean valueMatters) {
        if (!RxBleLog.isAtLeast(LogConstants.INFO)) {
            return;
        }
        AttributeLogWrapper value = new AttributeLogWrapper(descriptor.getUuid(), descriptor.getValue(), valueMatters);
        RxBleLog.i(commonMacMessage(gatt) + commonCallbackMessage() + commonStatusMessage() + commonValueMessage(),
                callbackName, status, value);
    }

    public static void logCallback(String callbackName, BluetoothGatt gatt, int status) {
        if (!RxBleLog.isAtLeast(LogConstants.INFO)) {
            return;
        }
        RxBleLog.i(commonMacMessage(gatt) + commonCallbackMessage() + commonStatusMessage(), callbackName, status);
    }

    public static void logCallback(String callbackName, BluetoothGatt gatt, int status, int value) {
        if (!RxBleLog.isAtLeast(LogConstants.INFO)) {
            return;
        }
        RxBleLog.i(commonMacMessage(gatt) + commonCallbackMessage() + commonStatusMessage() + commonValueMessage(),
                callbackName, status, value);
    }

    public static void logConnectionUpdateCallback(String callbackName, BluetoothGatt gatt,
                                                   int status, int interval, int latency, int timeout) {
        if (!RxBleLog.isAtLeast(LogConstants.INFO)) {
            return;
        }
        String customValueMessage = ", interval=%d (%.2f ms), latency=%d, timeout=%d (%.0f ms)";
        RxBleLog.i(commonMacMessage(gatt) + commonCallbackMessage() + commonStatusMessage() + customValueMessage,
                callbackName, status, interval, interval * 1.25f, latency, timeout, timeout * 10f);
    }

    public static String commonMacMessage(BluetoothGatt gatt) {
        if (gatt == null) return "MAC=null";
        return commonMacMessage(gatt.getDevice().getAddress());
    }

    public static String commonMacMessage(String macAddress) {
        if (macAddress == null) return "MAC=null";
        int logSetting = RxBleLog.getMacAddressLogSetting();
        switch (logSetting) {

            case LogConstants.MAC_ADDRESS_TRUNCATED:
                macAddress = macAddress.substring(0, 15) + "XX";
                break;
            case LogConstants.NONE:
                macAddress = "XX:XX:XX:XX:XX:XX";
            case LogConstants.MAC_ADDRESS_FULL:
            default:
        }
        return String.format("MAC='%s'", macAddress);
    }

    private static String commonCallbackMessage() {
        return " %24s()";
    }

    private static String commonStatusMessage() {
        return ", status=%d";
    }

    private static String commonValueMessage() {
        return ", value=%s";
    }

    public static AttributeLogWrapper wrap(BluetoothGattCharacteristic characteristic, boolean valueMatters) {
        return new AttributeLogWrapper(characteristic.getUuid(), characteristic.getValue(), valueMatters);
    }

    public static AttributeLogWrapper wrap(BluetoothGattDescriptor descriptor, boolean valueMatters) {
        return new AttributeLogWrapper(descriptor.getUuid(), descriptor.getValue(), valueMatters);
    }

    public static String getUuidToLog(UUID uuid) {
        int uuidLogSetting = RxBleLog.getUuidLogSetting();
        if (uuidLogSetting == LogConstants.UUIDS_FULL) {
            return uuid.toString();
        }
        return "...";
    }

    public static String getUuidSetToLog(Set<UUID> uuidSet) {
        int size = uuidSet.size();
        String[] uuids = new String[size];
        Iterator<UUID> iterator = uuidSet.iterator();
        for (int i = 0; i < size; i++) {
            String uuidToLog = LoggerUtil.getUuidToLog(iterator.next());
            uuids[i] = uuidToLog;
        }
        return Arrays.toString(uuids);
    }

    public static class AttributeLogWrapper {

        private final UUID uuid;
        private final byte[] value;
        private final boolean valueMatters;

        public AttributeLogWrapper(UUID uuid, byte[] value, boolean valueMatters) {
            this.uuid = uuid;
            this.value = value;
            this.valueMatters = valueMatters;
        }

        @Override
        public String toString() {
            return "[uuid='" + getUuidToLog(uuid)
                    + (valueMatters ? ("', hexValue=" + bytesToHex(value)) : "'")
                    + ']';
        }
    }
}
