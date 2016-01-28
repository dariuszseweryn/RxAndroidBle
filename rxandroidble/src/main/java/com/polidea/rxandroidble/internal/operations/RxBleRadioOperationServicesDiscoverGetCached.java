package com.polidea.rxandroidble.internal.operations;

import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import java.util.concurrent.atomic.AtomicReference;

public class RxBleRadioOperationServicesDiscoverGetCached extends RxBleRadioOperation<RxBleDeviceServices> {

    private final AtomicReference<RxBleDeviceServices> servicesAtomicReference;

    public RxBleRadioOperationServicesDiscoverGetCached(AtomicReference<RxBleDeviceServices> servicesAtomicReference) {
        this.servicesAtomicReference = servicesAtomicReference;
    }

    @Override
    public void run() {
        onNext(servicesAtomicReference.get());
        onCompleted();
        releaseRadio();
    }
}
