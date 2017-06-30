package com.polidea.rxandroidble.setup;


import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@SuppressWarnings("WeakerAccess")
public class WriteSetup {

    public static final int WRITE_TYPE_DEFAULT = 0;

    public static final int WRITE_TYPE_NO_RESPONSE = 1;

    public static final int WRITE_TYPE_SIGNED = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({WRITE_TYPE_DEFAULT,
            WRITE_TYPE_NO_RESPONSE,
            WRITE_TYPE_SIGNED})
    public @interface WriteType {

    }

    public final int writeType;

    private WriteSetup(int writeType) {
        this.writeType = writeType;
    }

    public static class Builder {

        private int writeType = WRITE_TYPE_DEFAULT;

        public Builder() {
        }

        public Builder setWriteType(@WriteType int writeType) {
            this.writeType = writeType;
            return this;
        }

        public WriteSetup build() {
            return new WriteSetup(writeType);
        }
    }
}
