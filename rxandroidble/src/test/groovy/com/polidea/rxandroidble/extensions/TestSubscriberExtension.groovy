package com.polidea.rxandroidble.extensions

import android.bluetooth.BluetoothGattService
import com.polidea.rxandroidble.RxBleDeviceServices
import com.polidea.rxandroidble.RxBleScanResult
import rx.observers.TestSubscriber

class TestSubscriberExtension {

    static boolean assertAnyOnNext(final TestSubscriber subscriber, Closure<Boolean> closure) {
        subscriber.onNextEvents.any closure
    }

    static boolean assertOneMatches(final TestSubscriber subscriber, Closure<Boolean> closure) {
        subscriber.onNextEvents.count(closure) == 1
    }

    static boolean assertError(final TestSubscriber subscriber, Closure<Boolean> closure) {
        subscriber.onErrorEvents?.any closure
    }

    static boolean assertScanRecord(final TestSubscriber<RxBleScanResult> subscriber, int rssi, String macAddress, byte[] scanRecord) {
        assertAnyOnNext(subscriber, {
            it.rssi == rssi && it.bleDevice.macAddress == macAddress && scanRecord == it.scanRecord.bytes
        })
    }

    static boolean assertScanRecordsByMacWithOrder(final TestSubscriber<RxBleScanResult> subscriber, List<String> expectedDevices) {
        subscriber.assertValueCount(expectedDevices.size())
        expectedDevices == subscriber.onNextEvents*.bleDevice.macAddress
    }

    static boolean assertSubscribed(final TestSubscriber subscriber) {
        !subscriber.isUnsubscribed()
    }

    static boolean assertReceivedOnNextNot(final TestSubscriber subscriber, List expectedList) {
        !subscriber.onNextEvents.equals(expectedList)
    }

    static boolean assertServices(final TestSubscriber<RxBleDeviceServices> subscriber, List<BluetoothGattService> services) {
        assertAnyOnNext(subscriber, {
            it.bluetoothGattServices == services
        })
    }

    static public <T> void assertValues(final TestSubscriber<T> subscriber, List<T> values) {
        subscriber.assertReceivedOnNext(values);
    }
}
