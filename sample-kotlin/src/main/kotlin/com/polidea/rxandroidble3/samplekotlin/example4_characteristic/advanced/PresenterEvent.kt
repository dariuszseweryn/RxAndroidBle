package com.polidea.rxandroidble2.samplekotlin.example4_characteristic.advanced

/**
 * Operation type for result or error.
 */
internal enum class Type {

    READ, WRITE, NOTIFY, INDICATE
}

/**
 * A dummy sealed class hierarchy to indicate what events may be emitted by the [Presenter].
 */
internal sealed class PresenterEvent

internal data class InfoEvent(val infoText: String) : PresenterEvent()

internal data class ResultEvent(val result: List<Byte>, val type: Type) : PresenterEvent()

internal data class ErrorEvent(val error: Throwable, val type: Type) : PresenterEvent()

internal data class CompatibilityModeEvent(val isCompatibility: Boolean) : PresenterEvent()
