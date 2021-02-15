package com.polidea.rxandroidble3

import android.os.DeadObjectException
import com.polidea.rxandroidble3.exceptions.BleDisconnectedException
import com.polidea.rxandroidble3.exceptions.BleException
import com.polidea.rxandroidble3.internal.Priority
import com.polidea.rxandroidble3.internal.QueueOperation
import com.polidea.rxandroidble3.internal.serialization.QueueReleaseInterface
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.subjects.BehaviorSubject

public class MockOperation extends QueueOperation<Object> {

    Priority priority
    public String lastExecutedOnThread
    int executionCount
    Closure<MockOperation> closure
    BehaviorSubject<MockOperation> behaviorSubject = BehaviorSubject.create()

    public static QueueOperation mockOperation(Priority priority, Closure runClosure) {
        return new MockOperation(priority, runClosure)
    }

    public static QueueOperation mockOperation(Priority priority) {
        return new MockOperation(priority, null)
    }

    MockOperation(Priority priority, Closure closure) {
        this.closure = closure
        this.priority = priority
    }

    @Override
    void protectedRun(ObservableEmitter<Object> emitter, QueueReleaseInterface queueReleaseInterface) {
        executionCount++
        lastExecutedOnThread = Thread.currentThread().getName()
        closure?.call(emitter)
        queueReleaseInterface.release()
        behaviorSubject.onNext(this)
    }

    public boolean wasRan() {
        executionCount > 0
    }

    @Override
    Priority definedPriority() {
        return priority
    }

    @Override
    protected BleException provideException(DeadObjectException deadObjectException) {
        return new BleDisconnectedException("MockDeviceAddress")
    }

    public io.reactivex.rxjava3.core.Observable<MockOperation> getFinishedRunningObservable() {
        behaviorSubject
    }
}
