package com.polidea.rxandroidble.internal.eventlog.events

import com.facebook.stetho.inspector.network.NetworkEventReporter
import com.polidea.rxandroidble.eventlog.OperationEvent

class OperationEnqueuedEvent(private val event: OperationEvent)
    : NetworkEventReporter.InspectorRequest, BleInspectorHeaders(event.attributes.attributes.toTypedArray()) {

    override fun body(): ByteArray? = event.payload
    override fun url(): String = event.title
    override fun method(): String = event.operationName
    override fun id(): String = event.operationId.toString()
    override fun friendlyNameExtra(): Int? = null
    override fun friendlyName(): String = ""
}