package com.polidea.rxandroidble2.internal.scan;

/**
 * An interface that describes what library extensions should be added to {@link com.polidea.rxandroidble2.scan.ScanSettings}
 */
public interface ExternalScanSettingsExtension {

    boolean shouldCheckLocationProviderState();

    interface Builder<T extends Builder<T>> {

        /**
         * Set whether a check if Location Services are (any Location Provider is) turned on before scheduling a BLE scan start.
         * Some Android devices will not return any {@link com.polidea.rxandroidble2.scan.ScanResult} if Location Services are
         * disabled. If set to true and Location Services are off a {@link com.polidea.rxandroidble2.exceptions.BleScanException}
         * will be emitted. <p>Default: true.</p>
         *
         * @param shouldCheck true if Location Services should be checked before the scan
         * @return the builder
         */
        T setShouldCheckLocationServicesState(boolean shouldCheck);
    }
}
