package com.polidea.rxandroidble.internal.cache;

import com.polidea.rxandroidble.internal.DeviceComponent;

import java.util.Map;

class CacheEntry implements Map.Entry<String, DeviceComponent> {

    private final String string;
    private final DeviceComponentWeakReference deviceComponentWeakReference;

    CacheEntry(String string, DeviceComponentWeakReference deviceComponentWeakReference) {
        this.string = string;
        this.deviceComponentWeakReference = deviceComponentWeakReference;
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

        return string.equals(that.getKey()) && deviceComponentWeakReference.equals(that.deviceComponentWeakReference);

    }

    @Override
    public String getKey() {
        return string;
    }

    @Override
    public DeviceComponent getValue() {
        return deviceComponentWeakReference.get();
    }

    @Override
    public int hashCode() {
        int result = string.hashCode();
        result = 31 * result + deviceComponentWeakReference.hashCode();
        return result;
    }

    @Override
    public DeviceComponent setValue(DeviceComponent object) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
