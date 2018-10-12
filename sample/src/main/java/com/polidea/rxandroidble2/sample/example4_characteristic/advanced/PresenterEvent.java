package com.polidea.rxandroidble2.sample.example4_characteristic.advanced;


import androidx.annotation.NonNull;
import java.util.Arrays;

/**
 * A dummy interface to indicate what events may be emitted by the {@link Presenter}. This would be a whole lot more convenient
 * if used Kotlin's `sealed class` with additional `data class` subclasses.
 */
interface PresenterEvent {

}

class InfoEvent implements PresenterEvent {

    @NonNull final String infoText;

    InfoEvent(@NonNull String infoText) {
        this.infoText = infoText;
    }

    @Override
    public String toString() {
        return "InfoEvent{"
                + "infoText='" + infoText + '\''
                + '}';
    }
}

class ResultEvent implements PresenterEvent {

    @NonNull final Type type;

    @NonNull final byte[] result;

    ResultEvent(@NonNull byte[] result, @NonNull Type type) {
        this.result = result;
        this.type = type;
    }

    @Override
    public String toString() {
        return "ResultEvent{"
                + "type=" + type
                + ", result=" + Arrays.toString(result)
                + '}';
    }
}

class ErrorEvent implements PresenterEvent {

    @NonNull final Type type;

    @NonNull final Throwable error;

    ErrorEvent(@NonNull Throwable error, @NonNull Type type) {
        this.error = error;
        this.type = type;
    }

    @Override
    public String toString() {
        return "ErrorEvent{"
                + "type=" + type
                + ", error=" + error
                + '}';
    }
}

class CompatibilityModeEvent implements PresenterEvent {

    final boolean show;

    CompatibilityModeEvent(boolean show) {
        this.show = show;
    }

    @Override
    public String toString() {
        return "CompatibilityModeEvent{"
                + "show=" + show
                + '}';
    }
}

enum Type {
    READ, WRITE, NOTIFY, INDICATE
}