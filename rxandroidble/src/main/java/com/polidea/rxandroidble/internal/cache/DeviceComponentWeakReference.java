package com.polidea.rxandroidble.internal.cache;

import com.polidea.rxandroidble.internal.DeviceComponent;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

class DeviceComponentWeakReference extends WeakReference<DeviceComponent> {

    public interface Provider {

        DeviceComponentWeakReference provide(DeviceComponent rxBleDevice);
    }

    public DeviceComponentWeakReference(DeviceComponent device) {
        super(device);
    }

    @SuppressWarnings("unused")
    public DeviceComponentWeakReference(DeviceComponent r, ReferenceQueue<? super DeviceComponent> q) {
        super(r, q);
    }

    public boolean contains(Object object) {
        final DeviceComponent thisDevice = get();
        return object instanceof DeviceComponent
                && thisDevice != null
                && thisDevice.provideDevice() == ((DeviceComponent) object).provideDevice();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WeakReference)) {
            return false;
        }
        WeakReference aWeakReference = (WeakReference) o;
        final DeviceComponent thisComponent = get();
        final Object otherThing = aWeakReference.get();
        return thisComponent != null
                && otherThing instanceof DeviceComponent
                && thisComponent.provideDevice().equals(((DeviceComponent) otherThing).provideDevice());
    }

    @Override
    public int hashCode() {
        return get() != null ? get().hashCode() : 0;
    }

    public boolean isEmpty() {
        return get() == null;
    }
}
