package com.polidea.rxandroidble.internal.cache;

import com.polidea.rxandroidble.RxBleDevice;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

class DeviceWeakReference extends WeakReference<RxBleDevice> {

    public interface Provider {

        DeviceWeakReference provide(RxBleDevice rxBleDevice);
    }

    public DeviceWeakReference(RxBleDevice device) {
        super(device);
    }

    @SuppressWarnings("unused")
    public DeviceWeakReference(RxBleDevice r, ReferenceQueue<? super RxBleDevice> q) {
        super(r, q);
    }

    public boolean contains(Object object) {
        final RxBleDevice thisDevice = get();
        return thisDevice != null && thisDevice == object;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WeakReference)) {
            return false;
        }
        WeakReference aWeakReference = (WeakReference) o;
        final RxBleDevice thisDevice = get();
        final Object otherThing = aWeakReference.get();
        return thisDevice != null && thisDevice.equals(otherThing);
    }

    @Override
    public int hashCode() {
        return get() != null ? get().hashCode() : 0;
    }

    public boolean isEmpty() {
        return get() == null;
    }
}
