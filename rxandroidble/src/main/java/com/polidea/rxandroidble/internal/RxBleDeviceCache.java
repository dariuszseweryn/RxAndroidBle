package com.polidea.rxandroidble.internal;

import android.support.annotation.Nullable;

import com.polidea.rxandroidble.RxBleDevice;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import rx.Observable;
import rx.functions.Func0;

public class RxBleDeviceCache implements Map<String, RxBleDevice> {

    private static class CacheEntry implements Map.Entry<String, RxBleDevice> {

        private final String string;

        private final DeviceWeakReference deviceWeakReference;

        private CacheEntry(String string, RxBleDevice device) {
            this.string = string;
            this.deviceWeakReference = new DeviceWeakReference(device);
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

    private static class DeviceWeakReference extends WeakReference<RxBleDevice> {

        public DeviceWeakReference(RxBleDevice r) {
            super(r);
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

        public boolean isEmpty() {
            return get() == null;
        }
    }

    private final HashMap<String, DeviceWeakReference> cache = new HashMap<>();

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return cache.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        final Collection<DeviceWeakReference> values = cache.values();
        for (DeviceWeakReference weakReference : values) {
            if (weakReference.contains(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Entry<String, RxBleDevice>> entrySet() {
        return Observable.from(cache.entrySet())
                .filter(stringDeviceWeakReferenceEntry -> !stringDeviceWeakReferenceEntry.getValue().isEmpty())
                .map(stringDeviceWeakReferenceEntry -> new CacheEntry(
                        stringDeviceWeakReferenceEntry.getKey(),
                        stringDeviceWeakReferenceEntry.getValue().get()
                ))
                .collect((Func0<HashSet<Entry<String, RxBleDevice>>>) HashSet::new, HashSet::add)
                .toBlocking().first();
    }

    @Nullable
    @Override
    public RxBleDevice get(Object key) {
        final DeviceWeakReference deviceWeakReference = cache.get(key);
        return deviceWeakReference != null ? deviceWeakReference.get() : null;
    }

    @Override
    public boolean isEmpty() {
        return cache.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return cache.keySet();
    }

    @Override
    public RxBleDevice put(String key, RxBleDevice value) {
        cache.put(key, new DeviceWeakReference(value));
        evictEmptyReferences();
        return value;
    }

    @Override
    public void putAll(Map<? extends String, ? extends RxBleDevice> map) {
        for (Entry<? extends String, ? extends RxBleDevice> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public RxBleDevice remove(Object key) {
        final DeviceWeakReference deviceWeakReference = cache.remove(key);
        evictEmptyReferences();
        return deviceWeakReference != null ? deviceWeakReference.get() : null;
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public Collection<RxBleDevice> values() {
        return Observable.from(cache.values())
                .filter(deviceWeakReference -> !deviceWeakReference.isEmpty())
                .map(Reference::get)
                .toList()
                .toBlocking()
                .first();
    }

    private void evictEmptyReferences() {

        for (Iterator<Entry<String, DeviceWeakReference>> iterator = cache.entrySet().iterator(); iterator.hasNext(); ) {
            final Entry<String, DeviceWeakReference> next = iterator.next();

            if (next.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }
}