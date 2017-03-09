package com.polidea.rxandroidble.internal.cache;

import com.polidea.rxandroidble.RxBleDevice;

import java.util.Map;

class CacheEntry implements Map.Entry<String, RxBleDevice> {

    private final String string;
    private final DeviceWeakReference deviceWeakReference;

    CacheEntry(String string, DeviceWeakReference deviceWeakReference) {
        this.string = string;
        this.deviceWeakReference = deviceWeakReference;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CacheEntry)) {
            return false;
        }

        CacheEntry that = (CacheEntry) o;

        return string.equals(that.getKey()) && deviceWeakReference.equals(that.deviceWeakReference);

    }

    @Override
    public String getKey() {
        return string;
    }

    @Override
    public RxBleDevice getValue() {
        return deviceWeakReference.get();
    }

    @Override
    public int hashCode() {
        int result = string.hashCode();
        result = 31 * result + deviceWeakReference.hashCode();
        return result;
    }

    @Override
    public RxBleDevice setValue(RxBleDevice object) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
