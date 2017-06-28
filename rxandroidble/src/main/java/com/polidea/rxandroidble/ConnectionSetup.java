package com.polidea.rxandroidble;

/**
 * Container for various connection parameters.
 */
public class ConnectionSetup {
    /**
     * Flag for autoconnection.
     */
    public final boolean autoConnect;
    /**
     * Flag describing the method of operation viability checking. If set to false,
     * a {@link com.polidea.rxandroidble.exceptions.BleIllegalOperationException} will be thrown everytime properties of the characteristic
     * don't match those required by the operation. If set to true, an event will be logged without interrupting the execution.
     */
    public final boolean suppressOperationCheck;

    private ConnectionSetup(boolean autoConnect, boolean suppressOperationCheck) {
        this.autoConnect = autoConnect;
        this.suppressOperationCheck = suppressOperationCheck;
    }

    public static class Builder {

        private boolean autoConnect = false;
        private boolean supressOperationCheck = false;

        public Builder setAutoConnect(boolean autoConnect) {
            this.autoConnect = autoConnect;
            return this;
        }

        public Builder setSupressOperationCheck(boolean suppressOperationCheck) {
            this.supressOperationCheck = suppressOperationCheck;
            return this;
        }

        public ConnectionSetup build() {
            return new ConnectionSetup(autoConnect, supressOperationCheck);
        }
    }
}
