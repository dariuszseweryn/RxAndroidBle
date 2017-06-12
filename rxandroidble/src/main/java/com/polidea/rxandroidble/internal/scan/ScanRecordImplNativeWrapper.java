package com.polidea.rxandroidble.internal.scan;


import android.os.Build;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.util.SparseArray;
import com.polidea.rxandroidble.scan.ScanRecord;
import java.util.List;
import java.util.Map;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ScanRecordImplNativeWrapper implements ScanRecord {

    private final android.bluetooth.le.ScanRecord nativeScanRecord;

    public ScanRecordImplNativeWrapper(android.bluetooth.le.ScanRecord nativeScanRecord) {
        this.nativeScanRecord = nativeScanRecord;
    }

    @Override
    public int getAdvertiseFlags() {
        return nativeScanRecord.getAdvertiseFlags();
    }

    @Nullable
    @Override
    public List<ParcelUuid> getServiceUuids() {
        return nativeScanRecord.getServiceUuids();
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
