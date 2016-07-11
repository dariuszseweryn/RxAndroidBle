package com.polidea.rxandroidble

import com.polidea.rxandroidble.internal.RxBleRadioOperation
import rx.Observable
import rx.subjects.BehaviorSubject

public class MockOperation extends RxBleRadioOperation<Object> {

    RxBleRadioOperation.Priority priority
    public String lastExecutedOnThread
    int executionCount
    Closure<MockOperation> closure
    BehaviorSubject<MockOperation> behaviorSubject = BehaviorSubject.create()

    public static RxBleRadioOperation mockOperation(RxBleRadioOperation.Priority priority, Closure runClosure) {
        return new MockOperation(priority, runClosure)
    }

    public static RxBleRadioOperation mockOperation(RxBleRadioOperation.Priority priority) {
        return new MockOperation(priority, null)
    }

    MockOperation(RxBleRadioOperation.Priority priority, Closure closure) {
        this.closure = closure
        this.priority = priority
    }

    @Override
    void protectedRun() {
        executionCount++
        lastExecutedOnThread = Thread.currentThread().getName()
        closure?.call(this)
        behaviorSubject.onNext(this)
    }

    public boolean wasRan() {
        executionCount > 0
    }

    @Override
    protected RxBleRadioOperation.Priority definedPriority() {
        return priority
    }

    public Observable<MockOperation> getFinishedRunningObservable() {
        behaviorSubject
    }
}
