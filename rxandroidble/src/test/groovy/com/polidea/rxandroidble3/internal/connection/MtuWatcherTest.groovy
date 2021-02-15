package com.polidea.rxandroidble3.internal.connection

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import com.polidea.rxandroidble3.exceptions.BleGattException
import com.polidea.rxandroidble3.exceptions.BleGattOperationType
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import spock.lang.Specification
import spock.lang.Unroll

class MtuWatcherTest extends Specification {

    PublishSubject<Observable<Integer>> onMtuChangedObservablePublishSubject = PublishSubject.create()

    RxBleGattCallback mockGattCallback = Mock RxBleGattCallback

    BluetoothGatt mockBluetoothGatt = Mock BluetoothGatt

    BluetoothDevice mockBluetoothDevice = Mock BluetoothDevice

    String mockAddress = "deviceAddress"

    MtuWatcher objectUnderTest

    private void setupObjectUnderTest(int minimumGattMtu) {
        mockBluetoothGatt.getDevice() >> mockBluetoothDevice
        mockBluetoothDevice.getAddress() >> mockAddress
        mockGattCallback.getOnMtuChanged() >> onMtuChangedObservablePublishSubject.switchMap { it }
        objectUnderTest = new MtuWatcher(mockGattCallback, minimumGattMtu)
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
        objectUnderTest.onConnectionSubscribed()
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
        objectUnderTest.onConnectionSubscribed()
        onMtuChangedObservablePublishSubject.onNext(Observable.error(new BleGattException(mockBluetoothGatt, 0, BleGattOperationType.ON_MTU_CHANGED)))
        onMtuChangedObservablePublishSubject.onNext(Observable.just(newMtu))

        expect:
        objectUnderTest.getMtu() == newMtu

        where:
        newMtu << [60, 900]
    }

    def "should subscribe to RxBleGattCallback.getOnMtuChanged() accordingly"() {

        given:
        setupObjectUnderTest(10)

        expect:
        !onMtuChangedObservablePublishSubject.hasObservers()

        when:
        objectUnderTest.onConnectionSubscribed()

        then:
        onMtuChangedObservablePublishSubject.hasObservers()

        when:
        objectUnderTest.onConnectionUnsubscribed()

        then:
        !onMtuChangedObservablePublishSubject.hasObservers()
    }
}