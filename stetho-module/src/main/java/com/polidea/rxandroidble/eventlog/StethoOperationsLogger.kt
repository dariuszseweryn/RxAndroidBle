package com.polidea.rxandroidble.eventlog

import android.util.Log
import com.facebook.stetho.inspector.network.DefaultResponseHandler
import com.facebook.stetho.inspector.network.NetworkEventReporter
import com.facebook.stetho.inspector.network.NetworkEventReporterImpl
import com.polidea.rxandroidble.internal.eventlog.events.OperationEnqueuedEvent
import com.polidea.rxandroidble.internal.eventlog.events.OperationStartedEvent

private const val TAG = "StethoOperationsLogger"
// TODO [PU] Documentation here
class StethoOperationsLogger : OperationEventLogger {

    private var networkEventReporter: NetworkEventReporter = NetworkEventReporterImpl.get()

    override fun isAttached(): Boolean = networkEventReporter.isEnabled

    override fun onOperationEnqueued(event: OperationEvent) = whenEnabled {
        Log.v(TAG, "onOperationEnqueued $event")
        networkEventReporter.requestWillBeSent(OperationEnqueuedEvent(event))
    }

    override fun onOperationStarted(event: OperationEvent) = whenEnabled {
        Log.v(TAG, "onOperationStarted $event")
        networkEventReporter.responseHeadersReceived(OperationStartedEvent(event))
    }

    override fun onOperationFailed(event: OperationEvent, message: String) = whenEnabled {
        Log.v(TAG, "onOperationFailed $event")
        networkEventReporter.responseReadFailed(event.operationId.toString(), message)
    }

    override fun onOperationFinished(event: OperationEvent) {
        onOperationFinished(event, null)
    }

    override fun onOperationFinished(event: OperationEvent, result: String?) = whenEnabled {
        Log.v(TAG, "onOperationFinished $event")
        val operationId = event.operationId.toString()

        if (result == null) {
            networkEventReporter.responseReadFinished(operationId)
        } else {
            result
                    .byteInputStream()
                    .use {
                        networkEventReporter.interpretResponseStream(
                                operationId,
                                "text/plain",
                                null,
                                it,
                                DefaultResponseHandler(networkEventReporter, operationId)
                        )!!.bufferedReader().readText()
                    }
        }
    }

    override fun onAtomicOperation(event: OperationEvent) {
        onAtomicOperation(event, null)
    }

    override fun onAtomicOperation(event: OperationEvent, result: String?) {
        onOperationEnqueued(event)
        onOperationStarted(event)
        onOperationFinished(event, result)
    }

    private fun whenEnabled(function: () -> Unit) {
        if (networkEventReporter.isEnabled) {
            function()
        }
    }
}

