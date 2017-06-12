package com.polidea.rxandroidble.internal.scan;


import android.annotation.SuppressLint;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanSettings;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AndroidScanObjectsConverter {

    private final int deviceSdk;

    @Inject
    public AndroidScanObjectsConverter(@Named(ClientComponent.PlatformConstants.INT_DEVICE_SDK) int deviceSdk) {
        this.deviceSdk = deviceSdk;
    }

    // TODO [DS 18.05.2017] Consider a different implementation for Marshmallow
    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public android.bluetooth.le.ScanSettings toNativeSettings(ScanSettings scanSettings) {
        final android.bluetooth.le.ScanSettings.Builder builder = new android.bluetooth.le.ScanSettings.Builder();
        if (deviceSdk >= Build.VERSION_CODES.M) {
            setMarshmallowSettings(scanSettings, builder);
        }
        return builder
                .setReportDelay(scanSettings.getReportDelayMillis())
                .setScanMode(scanSettings.getScanMode())
                .build();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void setMarshmallowSettings(ScanSettings scanSettings, android.bluetooth.le.ScanSettings.Builder builder) {
        builder
                .setCallbackType(scanSettings.getCallbackType())
                .setMatchMode(scanSettings.getMatchMode())
                .setNumOfMatches(scanSettings.getNumOfMatches());
    }

    @Nullable
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public List<android.bluetooth.le.ScanFilter> toNativeFilters(ScanFilter... scanFilters) {
        final boolean isFilteringDefined = scanFilters != null && scanFilters.length > 0;
        final List<android.bluetooth.le.ScanFilter> returnList;
        if (isFilteringDefined) {
            returnList = new ArrayList<>(scanFilters.length);
            for (ScanFilter scanFilter : scanFilters) {
                returnList.add(toNative(scanFilter));
            }
        } else {
            returnList = null;
        }
        return returnList;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private android.bluetooth.le.ScanFilter toNative(ScanFilter scanFilter) {
        final android.bluetooth.le.ScanFilter.Builder builder = new android.bluetooth.le.ScanFilter.Builder();
        if (scanFilter.getServiceDataUuid() != null) {
            builder.setServiceData(scanFilter.getServiceDataUuid(), scanFilter.getServiceData(), scanFilter.getServiceDataMask());
        }
        return builder
                .setDeviceAddress(scanFilter.getDeviceAddress())
                .setDeviceName(scanFilter.getDeviceName())
                .setManufacturerData(scanFilter.getManufacturerId(), scanFilter.getManufacturerData(), scanFilter.getManufacturerDataMask())
                .setServiceUuid(scanFilter.getServiceUuid(), scanFilter.getServiceUuidMask())
                .build();
    }
}
