package com.polidea.rxandroidble2.sample.example4_characteristic.advanced

/**
 * A dummy sealed class hierarchy to indicate what events may be emitted by the [Presenter].
 */
internal sealed class PresenterEvent

internal data class InfoEvent(val infoText: String) : PresenterEvent()

// Can't be a data class because of the ByteArray
internal class ResultEvent(val result: ByteArray, val type: Type) : PresenterEvent()

internal data class ErrorEvent(val error: Throwable, val type: Type) : PresenterEvent()

internal data class CompatibilityModeEvent(val show: Boolean) : PresenterEvent()

internal enum class Type {
    READ, WRITE, NOTIFY, INDICATE
}