package com.polidea.rxandroidble3.internal.logger;

import com.polidea.rxandroidble3.LogConstants;
import com.polidea.rxandroidble3.LogOptions;

public class LoggerSetup {

    @LogConstants.LogLevel
    public final int logLevel;
    @LogConstants.MacAddressLogSetting
    public final int macAddressLogSetting;
    @LogConstants.UuidLogSetting
    public final int uuidLogSetting;
    public final boolean shouldLogAttributeValues;
    public final boolean shouldLogScannedPeripherals;
    public final LogOptions.Logger logger;

    public LoggerSetup(
            int logLevel,
            int macAddressLogSetting,
            int uuidLogSetting,
            boolean shouldLogAttributeValues,
            boolean shouldLogScannedPeripherals,
            LogOptions.Logger logger
    ) {
        this.logLevel = logLevel;
        this.macAddressLogSetting = macAddressLogSetting;
        this.uuidLogSetting = uuidLogSetting;
        this.shouldLogAttributeValues = shouldLogAttributeValues;
        this.shouldLogScannedPeripherals = shouldLogScannedPeripherals;
        this.logger = logger;
    }

    public LoggerSetup merge(LogOptions logOptions) {
        int logLevel = logOptions.getLogLevel() != null ? logOptions.getLogLevel() : this.logLevel;
        int macAddressLogSetting =
                logOptions.getMacAddressLogSetting() != null ? logOptions.getMacAddressLogSetting() : this.macAddressLogSetting;
        int uuidLogSetting = logOptions.getUuidLogSetting() != null ? logOptions.getUuidLogSetting() : this.uuidLogSetting;
        boolean shouldLogAttributeValues =
                logOptions.getShouldLogAttributeValues() != null ? logOptions.getShouldLogAttributeValues() : this.shouldLogAttributeValues;
        boolean shouldLogScanResults = logOptions.getShouldLogScannedPeripherals() != null
                ? logOptions.getShouldLogScannedPeripherals()
                : this.shouldLogScannedPeripherals;
        LogOptions.Logger logger = logOptions.getLogger() != null ? logOptions.getLogger() : this.logger;
        return new LoggerSetup(logLevel, macAddressLogSetting, uuidLogSetting, shouldLogAttributeValues, shouldLogScanResults, logger);
    }

    @Override
    public String toString() {
        return "LoggerSetup{"
                + "logLevel=" + logLevel
                + ", macAddressLogSetting=" + macAddressLogSetting
                + ", uuidLogSetting=" + uuidLogSetting
                + ", shouldLogAttributeValues=" + shouldLogAttributeValues
                + ", shouldLogScannedPeripherals=" + shouldLogScannedPeripherals
                + ", logger=" + logger
                + '}';
    }
}
