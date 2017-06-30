package com.polidea.rxandroidble.setup;


@SuppressWarnings("WeakerAccess")
public class NotificationSetup {

    public final boolean isIndication;

    public final boolean setCCCDescriptor;

    private NotificationSetup(boolean isIndication, boolean setCCCDescriptor) {
        this.isIndication = isIndication;
        this.setCCCDescriptor = setCCCDescriptor;
    }

    @Override
    public String toString() {
        return "NotificationSetup{"
                + "isIndication=" + isIndication
                + ", setCCCDescriptor=" + setCCCDescriptor
                + '}';
    }

    public static class Builder {

        private boolean isIndication = false;

        private boolean setCCCDescriptor = true;

        public Builder() {
        }

        public Builder setIndication(boolean isIndication) {
            this.isIndication = isIndication;
            return this;
        }

        public Builder setCCCDescriptor(boolean setCCCDescriptor) {
            this.setCCCDescriptor = setCCCDescriptor;
            return this;
        }

        public NotificationSetup build() {
            return new NotificationSetup(isIndication, setCCCDescriptor);
        }
    }
}
