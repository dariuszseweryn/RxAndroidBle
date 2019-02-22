package com.polidea.rxandroidble2;

import android.util.Log;

import androidx.annotation.Nullable;

public class LogOptions {

    @Nullable
    private Integer logLevel;
    @Nullable
    private Integer macAddressLogSetting;
    @Nullable
    private Integer uuidLogSetting;
    @Nullable
    private Boolean shouldLogAttributeValues;
    @Nullable
    private Logger logger;

    private LogOptions(@Nullable Integer logLevel, @Nullable Integer macAddressLogSetting, @Nullable Integer uuidLogSetting,
                       @Nullable Boolean shouldLogAttributeValues, @Nullable Logger logger) {
        this.logLevel = logLevel;
        this.macAddressLogSetting = macAddressLogSetting;
        this.uuidLogSetting = uuidLogSetting;
        this.shouldLogAttributeValues = shouldLogAttributeValues;
        this.logger = logger;
    }

    public Integer getLogLevel() {
        return logLevel;
    }

    public Integer getMacAddressLogSetting() {
        return macAddressLogSetting;
    }

    public Integer getUuidLogSetting() {
        return uuidLogSetting;
    }

    public Boolean getShouldLogAttributeValues() {
        return shouldLogAttributeValues;
    }

    public Logger getLogger() {
        return logger;
    }

    @Override
    public String toString() {
        return "LogOptions{" +
                "logLevel=" + logLevel +
                ", macAddressLogSetting=" + macAddressLogSetting +
                ", uuidLogSetting=" + uuidLogSetting +
                ", shouldLogAttributeValues=" + shouldLogAttributeValues +
                ", logger=" + logger +
                '}';
    }

    public static class Builder {

        @Nullable
        private Integer logLevel;
        @Nullable
        private Integer macAddressLogSetting;
        @Nullable
        private Integer uuidsLogSetting;
        @Nullable
        private Boolean shouldLogAttributeValues;
        @Nullable
        private Logger logger;

        public Builder setLogLevel(@LogConstants.LogLevel @Nullable Integer logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public Builder setMacAddressLogSetting(@LogConstants.MacAddressLogSetting @Nullable Integer macAddressLogSetting) {
            this.macAddressLogSetting = macAddressLogSetting;
            return this;
        }

        public Builder setUuidsLogSetting(@LogConstants.UuidLogSetting @Nullable Integer uuidsLogSetting) {
            this.uuidsLogSetting = uuidsLogSetting;
            return this;
        }

        public Builder setShouldLogAttributeValues(@Nullable Boolean shouldLogAttributeValues) {
            this.shouldLogAttributeValues = shouldLogAttributeValues;
            return this;
        }

        public Builder setLogger(@Nullable Logger logger) {
            this.logger = logger;
            return this;
        }

        public LogOptions build() {
            return new LogOptions(logLevel, macAddressLogSetting, uuidsLogSetting, shouldLogAttributeValues, logger);
        }
    }

    /**
     * Simple logging interface for log messages from RxAndroidBle
     * <p>
     * {@link Builder#setLogger(Logger)}
     */
    public interface Logger {

        /**
         * @param level one of {@link Log#VERBOSE}, {@link Log#DEBUG}, {@link Log#INFO},
         *              {@link Log#WARN}, {@link Log#ERROR}
         * @param tag   log tag, caller
         * @param msg   message to log
         */
        void log(int level, String tag, String msg);
    }
}
