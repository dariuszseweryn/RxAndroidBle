package com.polidea.rxandroidble.internal.connection

import java.util.concurrent.atomic.AtomicInteger
import rx.Observable
import rx.subjects.PublishSubject
import spock.lang.Specification
import spock.lang.Unroll

public class MtuWatcherTest extends Specification {

    PublishSubject<Observable<Integer>> onMtuChangedObservablePublishSubject = PublishSubject.create()

    RxBleGattCallback mockGattCallback = Mock RxBleGattCallback

    MtuWatcher objectUnderTest

    private void setupObjectUnderTest(int minimumGattMtu) {
        mockGattCallback.getOnMtuChanged() >> onMtuChangedObservablePublishSubject.switchMap { it }
        objectUnderTest = new MtuWatcher(mockGattCallback, new AtomicInteger(), minimumGattMtu)
    }

    @Unroll
    def "should return initial value before onMtuChanged emits"() {

        given:
        setupObjectUnderTest(initialMtu)

        expect:
        objectUnderTest.getMtu() == initialMtu

        where:
        initialMtu << [10, 3000]
    }

    @Unroll
    def "after subscription should return new MTU after onMtuChanged emits"() {

        given:
        setupObjectUnderTest(10)
        objectUnderTest.subscribe()
        onMtuChangedObservablePublishSubject.onNext(Observable.just(newMtu))

        expect:
        objectUnderTest.getMtu() == newMtu

        where:
        newMtu << [50, 255]
    }

    @Unroll
    def "after subscription should return new MTU after onMtuChanges emits (even if onMtuChanges emitted an error before)"() {

        given:
        setupObjectUnderTest(10)
        objectUnderTest.subscribe()
        onMtuChangedObservablePublishSubject.onNext(Observable.error(new Throwable("test")))
        onMtuChangedObservablePublishSubject.onNext(Observable.just(newMtu))

        expect:
        objectUnderTest.getMtu() == newMtu

        where:
        newMtu << [60, 900]
    }
}