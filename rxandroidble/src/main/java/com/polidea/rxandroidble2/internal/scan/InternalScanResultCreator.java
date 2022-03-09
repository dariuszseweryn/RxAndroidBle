package com.polidea.rxandroidble2.internal.scan;


import static com.polidea.rxandroidble2.scan.ScanCallbackType.CALLBACK_TYPE_ALL_MATCHES;
import static com.polidea.rxandroidble2.scan.ScanCallbackType.CALLBACK_TYPE_FIRST_MATCH;
import static com.polidea.rxandroidble2.scan.ScanCallbackType.CALLBACK_TYPE_MATCH_LOST;
import static com.polidea.rxandroidble2.scan.ScanCallbackType.CALLBACK_TYPE_UNKNOWN;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import com.polidea.rxandroidble2.ClientScope;
import com.polidea.rxandroidble2.IsConnectableStatus;
import com.polidea.rxandroidble2.internal.RxBleLog;
import com.polidea.rxandroidble2.internal.util.ScanRecordParser;
import com.polidea.rxandroidble2.scan.ScanCallbackType;
import com.polidea.rxandroidble2.scan.ScanRecord;
import bleshadow.javax.inject.Inject;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ClientScope
public class InternalScanResultCreator {

    private final ScanRecordParser scanRecordParser;

    @Inject
    public InternalScanResultCreator(ScanRecordParser scanRecordParser) {
        this.scanRecordParser = scanRecordParser;
    }

    public RxBleInternalScanResult create(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
        final ScanRecord scanRecordObj = scanRecordParser.parseFromBytes(scanRecord);
        return new RxBleInternalScanResult(bluetoothDevice, rssi, System.nanoTime(), scanRecordObj,
                ScanCallbackType.CALLBACK_TYPE_UNSPECIFIED, IsConnectableStatus.LEGACY_UNKNOWN);
    }

    @RequiresApi(21 /* Build.VERSION_CODES.LOLLIPOP */)
    public RxBleInternalScanResult create(ScanResult result) {
        final ScanRecordImplNativeWrapper scanRecord = new ScanRecordImplNativeWrapper(result.getScanRecord(), scanRecordParser);
        return new RxBleInternalScanResult(result.getDevice(), result.getRssi(), result.getTimestampNanos(), scanRecord,
                ScanCallbackType.CALLBACK_TYPE_BATCH, isConnectable(result));
    }

    @RequiresApi(21 /* Build.VERSION_CODES.LOLLIPOP */)
    public RxBleInternalScanResult create(int callbackType, ScanResult result) {
        final ScanRecordImplNativeWrapper scanRecord = new ScanRecordImplNativeWrapper(result.getScanRecord(), scanRecordParser);
        return new RxBleInternalScanResult(result.getDevice(), result.getRssi(), result.getTimestampNanos(), scanRecord,
                toScanCallbackType(callbackType), isConnectable(result));
    }

    @RequiresApi(21 /* Build.VERSION_CODES.LOLLIPOP */)
    private static ScanCallbackType toScanCallbackType(int callbackType) {
        switch (callbackType) {
            case ScanSettings.CALLBACK_TYPE_ALL_MATCHES:
                return CALLBACK_TYPE_ALL_MATCHES;
            case ScanSettings.CALLBACK_TYPE_FIRST_MATCH:
                return CALLBACK_TYPE_FIRST_MATCH;
            case ScanSettings.CALLBACK_TYPE_MATCH_LOST:
                return CALLBACK_TYPE_MATCH_LOST;
            default:
                RxBleLog.w("Unknown callback type %d -> check android.bluetooth.le.ScanSettings", callbackType);
                return CALLBACK_TYPE_UNKNOWN;
        }
    }

    private IsConnectableStatus isConnectable(ScanResult scanResult) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (scanResult.isConnectable()) {
                return IsConnectableStatus.CONNECTABLE;
            } else {
                return IsConnectableStatus.NOT_CONNECTABLE;
            }
        }
        return IsConnectableStatus.LEGACY_UNKNOWN;
    }
}
