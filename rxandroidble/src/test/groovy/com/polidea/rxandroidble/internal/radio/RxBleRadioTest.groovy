package com.polidea.rxandroidble.internal.radio

import com.polidea.rxandroidble.MockOperation
import rx.Observable
import rx.Scheduler
import rx.android.plugins.RxAndroidPlugins
import rx.android.plugins.RxAndroidSchedulersHook
import rx.android.schedulers.AndroidSchedulers
import rx.observers.TestSubscriber
import rx.schedulers.Schedulers
import spock.lang.Specification

import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

import static com.polidea.rxandroidble.internal.RxBleRadioOperation.Priority.NORMAL

class RxBleRadioTest extends Specification {
    public static final String RADIO_SCHEDULER_THREAD_NAME = "radio-test-thread"

    RxBleRadioImpl objectUnderTest

    void setup() {
        objectUnderTest = new RxBleRadioImpl(createSchedulerWithNamedThread(RADIO_SCHEDULER_THREAD_NAME))
    }

    def "should run operation instantly if queue is empty and no operation is in progress"() {
        given:
        def operation = MockOperation.mockOperation(NORMAL)

        when:
        objectUnderTest.queue(operation).subscribe()
        waitForOperationsToFinishRunning(operation)

        then:
        operation.wasRan()

        and:
        operation.lastExecutedOnThread == RADIO_SCHEDULER_THREAD_NAME

        and:
        operation.executionCount == 1
    }

    def "should not run second operation until first release radio"() {
        given:
        Semaphore semaphore = new Semaphore(0)
        MockOperation firstOperation = operationReleasingRadioAfterSemaphoreIsReleased(semaphore)
        def secondOperation = MockOperation.mockOperation(NORMAL)

        when:
        objectUnderTest.queue(firstOperation).subscribe()
        objectUnderTest.queue(secondOperation).subscribe()
        waitForThreadsToCompleteWork()

        then:
        firstOperation.wasRan()

        and:
        !secondOperation.wasRan()

        and:
        semaphore.release()
        waitForOperationsToFinishRunning(secondOperation)
        secondOperation.wasRan()
    }

    def "should not run operation if it was unsubscribed before was taken from the queue"() {
        given:
        Semaphore semaphore = new Semaphore(0)
        MockOperation firstOperation = operationReleasingRadioAfterSemaphoreIsReleased(semaphore)
        MockOperation secondOperation = MockOperation.mockOperation(NORMAL)

        when:
        objectUnderTest.queue(firstOperation).subscribe()
        objectUnderTest.queue(secondOperation).subscribe().unsubscribe()
        waitForThreadsToCompleteWork()

        then:
        !secondOperation.wasRan()

        and:
        semaphore.release()
        waitForThreadsToCompleteWork()
        !secondOperation.wasRan()
    }

    def "should emit onNext from operation"() {
        given:
        def expectedData = "some string"
        def testSubscriber = new TestSubscriber()
        def firstOperation = MockOperation.mockOperation(NORMAL, {
            it.onNext(expectedData)
            it.releaseRadio()
        })

        when:
        objectUnderTest.queue(firstOperation).subscribe(testSubscriber)
        waitForThreadsToCompleteWork()

        then:
        testSubscriber.assertValue(expectedData)

        and:
        testSubscriber.assertNotCompleted()
    }

    def "should emit onNext and release radio if completed"() {
        given:
        def expectedData = "some string"
        def testSubscriber = new TestSubscriber()
        def firstOperation = MockOperation.mockOperation(NORMAL, {
            it.onNext(expectedData)
            it.onCompleted()
        })

        when:
        objectUnderTest.queue(firstOperation).subscribe(testSubscriber)
        waitForThreadsToCompleteWork()

        then:
        testSubscriber.assertValue(expectedData)

        and:
        testSubscriber.assertCompleted()
    }

    def "should emit onError and release radio if error occured"() {
        given:
        def firstTestSubscriber = new TestSubscriber()
        def secondTestSubscriber = new TestSubscriber()
        def firstOperation =  MockOperation.mockOperation(NORMAL, {
                    it.onError(new Throwable("First throwable"))
                })

        def secondOperation = new MockOperation(NORMAL, null) {
            @Override
            void protectedRun() {
                // simulate that a not handled exception was thrown somewhere
                throw new Exception("Second throwable")
            }
        }

        when:
        objectUnderTest.queue(firstOperation).subscribe(firstTestSubscriber)
        objectUnderTest.queue(secondOperation).subscribe(secondTestSubscriber)
        waitForThreadsToCompleteWork()

        then:
        firstTestSubscriber.assertError(Throwable)
        secondTestSubscriber.assertError(Throwable)

        and:
        firstTestSubscriber.assertError {
            it.message == "First throwable"
        }
        secondTestSubscriber.assertError {
            it.message == "Second throwable"
        }
    }

    public waitForThreadsToCompleteWork() {
        Thread.sleep(200) // Nasty :<
        true
    }

    public operationReleasingRadioAfterSemaphoreIsReleased(semaphore) {
        MockOperation.mockOperation(NORMAL, {
            semaphore.acquire()
            it.releaseRadio()
        })
    }

    public releaseSemaphoreAndWaitForThreads(Semaphore semaphore) {
        semaphore.release()
        waitForThreadsToCompleteWork()
    }

    private static Scheduler createSchedulerWithNamedThread(String threadName) {
        Schedulers.from(Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            Thread newThread(Runnable r) {
                return new Thread(r, threadName)
            }
        }))
    }

    private static void waitForOperationsToFinishRunning(MockOperation... operations) {
        Observable<MockOperation> observable
        for (MockOperation mockOperation : operations) {
            if (observable == null) {
                observable = mockOperation.getFinishedRunningObservable()
            } else {
                observable = observable.flatMap({ mockOperation.getFinishedRunningObservable() })
            }
        }
        if (observable != null) observable.timeout(1, TimeUnit.SECONDS).toBlocking().first()
    }
}
