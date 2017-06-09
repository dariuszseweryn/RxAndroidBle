package com.polidea.rxandroidble.internal.scan;


import static com.polidea.rxandroidble.scan.ScanCallbackType.CALLBACK_TYPE_ALL_MATCHES;
import static com.polidea.rxandroidble.scan.ScanCallbackType.CALLBACK_TYPE_FIRST_MATCH;
import static com.polidea.rxandroidble.scan.ScanCallbackType.CALLBACK_TYPE_MATCH_LOST;
import static com.polidea.rxandroidble.scan.ScanCallbackType.CALLBACK_TYPE_UNKNOWN;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import com.polidea.rxandroidble.ClientScope;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.util.UUIDUtil;
import com.polidea.rxandroidble.scan.ScanCallbackType;
import com.polidea.rxandroidble.scan.ScanRecord;
import javax.inject.Inject;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ClientScope
public class InternalScanResultCreator {

    private final UUIDUtil uuidUtil;

    @Inject
    public InternalScanResultCreator(UUIDUtil uuidUtil) {
        this.uuidUtil = uuidUtil;
    }

    public RxBleInternalScanResult create(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
        final ScanRecord scanRecordObj = uuidUtil.parseFromBytes(scanRecord);
        return new RxBleInternalScanResult(bluetoothDevice, rssi, System.nanoTime(), scanRecordObj,
                ScanCallbackType.CALLBACK_TYPE_UNSPECIFIED);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public RxBleInternalScanResult create(ScanResult result) {
        final ScanRecordImplNativeWrapper scanRecord = new ScanRecordImplNativeWrapper(result.getScanRecord());
        return new RxBleInternalScanResult(result.getDevice(), result.getRssi(), result.getTimestampNanos(), scanRecord,
                ScanCallbackType.CALLBACK_TYPE_BATCH);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public RxBleInternalScanResult create(int callbackType, ScanResult result) {
        final ScanRecordImplNativeWrapper scanRecord = new ScanRecordImplNativeWrapper(result.getScanRecord());
        return new RxBleInternalScanResult(result.getDevice(), result.getRssi(), result.getTimestampNanos(), scanRecord,
                toScanCallbackType(callbackType));
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private static ScanCallbackType toScanCallbackType(int callbackType) {
        switch (callbackType) {
            case ScanSettings.CALLBACK_TYPE_ALL_MATCHES:
                return CALLBACK_TYPE_ALL_MATCHES;
            case ScanSettings.CALLBACK_TYPE_FIRST_MATCH:
                return CALLBACK_TYPE_FIRST_MATCH;
            case ScanSettings.CALLBACK_TYPE_MATCH_LOST:
                return CALLBACK_TYPE_MATCH_LOST;
            default:
                RxBleLog.w("Unknown callback type %d -> check android.bluetooth.le.ScanSettings");
                return CALLBACK_TYPE_UNKNOWN;
        }
    }
}
