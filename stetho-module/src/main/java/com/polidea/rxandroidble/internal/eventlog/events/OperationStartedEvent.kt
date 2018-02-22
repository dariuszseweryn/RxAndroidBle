package com.polidea.rxandroidble.internal.eventlog.events

import com.facebook.stetho.inspector.network.NetworkEventReporter
import com.polidea.rxandroidble.eventlog.OperationEvent

class OperationStartedEvent(private val event: OperationEvent) : NetworkEventReporter.InspectorResponse, BleInspectorHeaders() {
    override fun reasonPhrase(): String = "Started"
    override fun statusCode(): Int = 200
    override fun requestId(): String = event.operationId.toString()
    override fun url(): String = event.title
    override fun connectionReused(): Boolean = false
    override fun fromDiskCache(): Boolean = false
    override fun connectionId(): Int = 42
}