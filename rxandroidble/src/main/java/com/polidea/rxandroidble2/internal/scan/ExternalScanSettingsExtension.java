package com.polidea.rxandroidble2.internal.scan;

/**
 * An interface that describes what library extensions should be added to {@link com.polidea.rxandroidble2.scan.ScanSettings}
 */
public interface ExternalScanSettingsExtension<R extends ExternalScanSettingsExtension<R>> {

    boolean shouldCheckLocationProviderState();

    // [DS 18.09.2019] Introduced to be sure that new ScanSettings properties will not break workaround introduced in
    // ScanSettingsBuilderImplApi21
    /**
     * Copies the current ScanSettings with changed callback type.
     *
     * @param callbackType callback type of the copied object
     * @return new ScanSettings object with copied properties and new callback type
     */
    R copyWithCallbackType(int callbackType);

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
    }
}
