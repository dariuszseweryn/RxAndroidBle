package com.polidea.rxandroidble.internal;

import com.polidea.rxandroidble.RxBleDevice;

import dagger.Subcomponent;

@DeviceScope
@Subcomponent(modules = DeviceModule.class)
public interface DeviceComponent {

    @Subcomponent.Builder
    interface Builder {
        DeviceComponent build();
        Builder deviceModule(DeviceModule module);
    }

    @DeviceScope
    RxBleDevice provideDevice();
}
