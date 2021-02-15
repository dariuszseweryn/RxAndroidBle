package com.polidea.rxandroidble3.internal.scan;


import android.os.Build;
import android.os.ParcelUuid;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import android.util.SparseArray;
import com.polidea.rxandroidble3.internal.util.ScanRecordParser;
import com.polidea.rxandroidble3.scan.ScanRecord;
import java.util.List;
import java.util.Map;

@RequiresApi(21 /* Build.VERSION_CODES.LOLLIPOP */)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ScanRecordImplNativeWrapper implements ScanRecord {

    private final android.bluetooth.le.ScanRecord nativeScanRecord;
    private final ScanRecordParser scanRecordParser;

    public ScanRecordImplNativeWrapper(android.bluetooth.le.ScanRecord nativeScanRecord,
                                       ScanRecordParser scanRecordParser) {
        this.nativeScanRecord = nativeScanRecord;
        this.scanRecordParser = scanRecordParser;
    }

    @Override
    public int getAdvertiseFlags() {
        return nativeScanRecord.getAdvertiseFlags();
    }

    @Override
    public List<ParcelUuid> getServiceUuids() {
        return nativeScanRecord.getServiceUuids();
    }

    @Override
    public List<ParcelUuid> getServiceSolicitationUuids() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return nativeScanRecord.getServiceSolicitationUuids();
        } else {
            return scanRecordParser.parseFromBytes(nativeScanRecord.getBytes()).getServiceSolicitationUuids();
        }
    }

    @Override
    public SparseArray<byte[]> getManufacturerSpecificData() {
        return nativeScanRecord.getManufacturerSpecificData();
    }

    @Nullable
    @Override
    public byte[] getManufacturerSpecificData(int manufacturerId) {
        return nativeScanRecord.getManufacturerSpecificData(manufacturerId);
    }

    @Override
    public Map<ParcelUuid, byte[]> getServiceData() {
        return nativeScanRecord.getServiceData();
    }

    @Nullable
    @Override
    public byte[] getServiceData(ParcelUuid serviceDataUuid) {
        return nativeScanRecord.getServiceData(serviceDataUuid);
    }

    @Override
    public int getTxPowerLevel() {
        return nativeScanRecord.getTxPowerLevel();
    }

    @Nullable
    @Override
    public String getDeviceName() {
        return nativeScanRecord.getDeviceName();
    }

    @Override
    public byte[] getBytes() {
        return nativeScanRecord.getBytes();
    }
}
