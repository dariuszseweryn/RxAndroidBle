package com.polidea.rxandroidble.internal.eventlog.events

import com.facebook.stetho.inspector.network.NetworkEventReporter
import com.polidea.rxandroidble.eventlog.OperationAttribute

abstract class BleInspectorHeaders(headersArray: Array<out OperationAttribute> = emptyArray()) : NetworkEventReporter.InspectorHeaders {
    private val headers: MutableList<OperationAttribute> = headersArray.toMutableList()
    override fun headerCount(): Int = headers.size
    override fun firstHeaderValue(name: String?): String? = null
    override fun headerName(index: Int): String = headers[index].name
    override fun headerValue(index: Int): String = headers[index].value
}

