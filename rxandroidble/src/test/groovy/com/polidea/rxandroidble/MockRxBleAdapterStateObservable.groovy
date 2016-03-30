package com.polidea.rxandroidble

import rx.Observable
import rx.subjects.ReplaySubject

class MockRxBleAdapterStateObservable {

    public final ReplaySubject relay = ReplaySubject.create()

    public Observable<RxBleAdapterStateObservable.BleAdapterState> asObservable() {
        Observable.create({ relay.subscribe(it) });
    }

    def disableBluetooth() {
        relay.onNext(RxBleAdapterStateObservable.BleAdapterState.STATE_OFF)
    }
}
