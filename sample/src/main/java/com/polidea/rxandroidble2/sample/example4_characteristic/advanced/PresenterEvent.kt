package com.polidea.rxandroidble2.sample.example4_characteristic.advanced

import java.util.Arrays

/**
 * A dummy interface to indicate what events may be emitted by the [Presenter]. This would be a whole lot more convenient
 * if used Kotlin's `sealed class` with additional `data class` subclasses.
 */
internal interface PresenterEvent

internal class InfoEvent(val infoText: String) : PresenterEvent {

    override fun toString(): String {
        return ("InfoEvent{"
                + "infoText='" + infoText + '\''.toString()
                + '}'.toString())
    }
}

internal class ResultEvent(val result: ByteArray, val type: Type) : PresenterEvent {

    override fun toString(): String {
        return ("ResultEvent{"
                + "type=" + type
                + ", result=" + Arrays.toString(result)
                + '}'.toString())
    }
}

internal class ErrorEvent(val error: Throwable, val type: Type) : PresenterEvent {

    override fun toString(): String {
        return ("ErrorEvent{"
                + "type=" + type
                + ", error=" + error
                + '}'.toString())
    }
}

internal class CompatibilityModeEvent(val show: Boolean) : PresenterEvent {

    override fun toString(): String {
        return ("CompatibilityModeEvent{"
                + "show=" + show
                + '}'.toString())
    }
}

internal enum class Type {
    READ, WRITE, NOTIFY, INDICATE
}