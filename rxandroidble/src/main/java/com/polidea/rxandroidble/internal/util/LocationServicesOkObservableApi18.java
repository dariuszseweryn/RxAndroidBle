package com.polidea.rxandroidble.internal.util;

import rx.Emitter;
import rx.Observable;
import rx.functions.Action1;
import rx.internal.operators.OnSubscribeCreate;
import bleshadow.javax.inject.Inject;


public class LocationServicesOkObservableApi18 extends Observable<Boolean> {

    @Inject
    LocationServicesOkObservableApi18() {
        super(new OnSubscribeCreate<>(
                new Action1<Emitter<Boolean>>() {
                    @Override
                    public void call(Emitter<Boolean> booleanEmitter) {
                        booleanEmitter.onNext(true);
                    }
                },
                Emitter.BackpressureMode.NONE
        ));
    }
}
