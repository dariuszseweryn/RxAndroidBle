package com.polidea.rxandroidble2.internal.scan;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * An interface that describes what library extensions should be added to {@link com.polidea.rxandroidble2.scan.ScanSettings}
 */
public interface ExternalScanSettingsExtension {

    enum API0 {
        AUTO,
        AUTO_NO_FALLBACK,
        NATIVE,
        EMULATED,
    }

    enum API1_SETTINGS {
        AUTO,
        NATIVE,
        EMULATED
    }

    enum API1_FILTERS {
        AUTO,
        NATIVE,
        EMULATED
    }

    // Fallback strategy? defaultStrategy = API2_AUTO | API2_EMULATED_ALL
    int API2_AUTO = 1; // default for api21/23? use default? use fallback?
    int API2_EMULATED_SETTINGS = 1 << 1;
    int API2_EMULATED_FILTERS = 1 << 2;
    int API2_EMULATED_ALL = 1 << 3;

    // CustomScanOperation?


    int EMULATE_SETTINGS = 1 << 1;
    int EMUALTE_FILTERS = 1 << 2;

    @IntDef(flag = true, value = {EMULATE_SETTINGS, EMUALTE_FILTERS})
    @Retention(RetentionPolicy.SOURCE)
    @interface EmulationFlags {

    }

    boolean shouldCheckLocationProviderState();

    @Nullable
    int[] getEmulationFlags();

    interface Builder<T extends Builder<T>> {

        /**
         * Set whether a check if Location Services are (any Location Provider is) turned on before scheduling a BLE scan start.
         * Some Android devices will not return any {@link com.polidea.rxandroidble2.scan.ScanResult} if Location Services are
         * disabled.
         *
         * @param shouldCheck true if Location Services should be checked before the scan
         * @return the builder
         */
        T setShouldCheckLocationServicesState(boolean shouldCheck);

        T setEmulationFlag(@EmulationFlags int... emulationFlag);
    }
}
