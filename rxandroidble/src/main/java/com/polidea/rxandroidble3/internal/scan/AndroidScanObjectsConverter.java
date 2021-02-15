package com.polidea.rxandroidble3.internal.scan;


import android.annotation.SuppressLint;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import com.polidea.rxandroidble3.ClientComponent;
import com.polidea.rxandroidble3.scan.ScanFilter;
import com.polidea.rxandroidble3.scan.ScanSettings;
import java.util.ArrayList;
import java.util.List;
import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AndroidScanObjectsConverter {

    private final int deviceSdk;

    @Inject
    public AndroidScanObjectsConverter(@Named(ClientComponent.PlatformConstants.INT_DEVICE_SDK) int deviceSdk) {
        this.deviceSdk = deviceSdk;
    }

    // TODO [DS 18.05.2017] Consider a different implementation for Marshmallow
    @SuppressLint("NewApi")
    @RequiresApi(21 /* Build.VERSION_CODES.LOLLIPOP */)
    public android.bluetooth.le.ScanSettings toNativeSettings(ScanSettings scanSettings) {
        final android.bluetooth.le.ScanSettings.Builder builder = new android.bluetooth.le.ScanSettings.Builder();
        if (deviceSdk >= 23 /* Build.VERSION_CODES.M */) {
            setMarshmallowSettings(scanSettings, builder);
        }
        return builder
                .setReportDelay(scanSettings.getReportDelayMillis())
                .setScanMode(scanSettings.getScanMode())
                .build();
    }

    @RequiresApi(23 /* Build.VERSION_CODES.M */)
    private static void setMarshmallowSettings(ScanSettings scanSettings, android.bluetooth.le.ScanSettings.Builder builder) {
        builder
                .setCallbackType(scanSettings.getCallbackType())
                .setMatchMode(scanSettings.getMatchMode())
                .setNumOfMatches(scanSettings.getNumOfMatches());
    }

    @Nullable
    @RequiresApi(21 /* Build.VERSION_CODES.LOLLIPOP */)
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

    @RequiresApi(21 /* Build.VERSION_CODES.LOLLIPOP */)
    private static android.bluetooth.le.ScanFilter toNative(ScanFilter scanFilter) {
        final android.bluetooth.le.ScanFilter.Builder builder = new android.bluetooth.le.ScanFilter.Builder();
        if (scanFilter.getServiceDataUuid() != null) {
            builder.setServiceData(scanFilter.getServiceDataUuid(), scanFilter.getServiceData(), scanFilter.getServiceDataMask());
        }
        if (scanFilter.getDeviceAddress() != null) {
            builder.setDeviceAddress(scanFilter.getDeviceAddress());
        }
        return builder
                .setDeviceName(scanFilter.getDeviceName())
                .setManufacturerData(scanFilter.getManufacturerId(), scanFilter.getManufacturerData(), scanFilter.getManufacturerDataMask())
                .setServiceUuid(scanFilter.getServiceUuid(), scanFilter.getServiceUuidMask())
                .build();
    }
}
