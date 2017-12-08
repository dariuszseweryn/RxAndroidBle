package com.polidea.rxandroidble.setup;


@SuppressWarnings("WeakerAccess")
public class SetupNotification {

    public final TrueFalseAuto indicationMode;
    public final TrueFalseAuto cccDescriptorWriteMode;

    private SetupNotification(TrueFalseAuto indicationMode, TrueFalseAuto cccDescriptorWriteMode) {
        this.indicationMode = indicationMode;
        this.cccDescriptorWriteMode = cccDescriptorWriteMode;
    }

    @Override
    public String toString() {
        return "NotificationSetup{"
                + "indicationMode=" + indicationMode
                + ", cccDescriptorWriteMode=" + cccDescriptorWriteMode
                + '}';
    }

    public static class Builder {

        private TrueFalseAuto indicationMode = TrueFalseAuto.AUTO;
        private TrueFalseAuto cccDescriptorWriteMode = TrueFalseAuto.AUTO;

        public Builder() {
        }

        public Builder setIndication(TrueFalseAuto indicationMode) {
            this.indicationMode = indicationMode;
            return this;
        }

        public Builder setCCCDescriptorWriteMode(TrueFalseAuto cccDescriptorWriteMode) {
            this.cccDescriptorWriteMode = cccDescriptorWriteMode;
            return this;
        }

        public SetupNotification build() {
            return new SetupNotification(indicationMode, cccDescriptorWriteMode);
        }
    }
}
