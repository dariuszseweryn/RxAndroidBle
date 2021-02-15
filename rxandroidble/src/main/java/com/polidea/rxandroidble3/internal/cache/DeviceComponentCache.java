package com.polidea.rxandroidble3.internal.cache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.polidea.rxandroidble3.ClientScope;
import com.polidea.rxandroidble3.internal.DeviceComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import bleshadow.javax.inject.Inject;

@ClientScope
public class DeviceComponentCache implements Map<String, DeviceComponent> {

    private final HashMap<String, DeviceComponentWeakReference> cache = new HashMap<>();
    private final DeviceComponentWeakReference.Provider deviceComponentReferenceProvider;

    @Inject
    public DeviceComponentCache() {
        this(new DeviceComponentWeakReference.Provider() {
            @Override
            public DeviceComponentWeakReference provide(DeviceComponent device) {
                return new DeviceComponentWeakReference(device);
            }
        });
    }

    DeviceComponentCache(DeviceComponentWeakReference.Provider provider) {
        deviceComponentReferenceProvider = provider;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return cache.containsKey(key) && get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        final Collection<DeviceComponentWeakReference> values = cache.values();
        for (DeviceComponentWeakReference weakReference : values) {
            if (weakReference.contains(value)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public Set<Entry<String, DeviceComponent>> entrySet() {
        final HashSet<Entry<String, DeviceComponent>> components = new HashSet<>();

        for (Entry<String, DeviceComponentWeakReference> entry : cache.entrySet()) {
            final DeviceComponentWeakReference entryValue = entry.getValue();

            if (!entryValue.isEmpty()) {
                components.add(new CacheEntry(entry.getKey(), deviceComponentReferenceProvider.provide(entryValue.get())));
            }
        }

        return components;
    }

    @Nullable
    @Override
    public DeviceComponent get(Object key) {
        final DeviceComponentWeakReference deviceComponentWeakReference = cache.get(key);
        return deviceComponentWeakReference != null ? deviceComponentWeakReference.get() : null;
    }

    @Override
    public boolean isEmpty() {
        evictEmptyReferences();
        return cache.isEmpty();
    }

    @NonNull
    @Override
    public Set<String> keySet() {
        return cache.keySet();
    }

    @Override
    public DeviceComponent put(String key, DeviceComponent value) {
        cache.put(key, deviceComponentReferenceProvider.provide(value));
        evictEmptyReferences();
        return value;
    }

    @Override
    public void putAll(@NonNull Map<? extends String, ? extends DeviceComponent> map) {
        for (Entry<? extends String, ? extends DeviceComponent> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public DeviceComponent remove(Object key) {
        final DeviceComponentWeakReference deviceComponentWeakReference = cache.remove(key);
        evictEmptyReferences();
        return deviceComponentWeakReference != null ? deviceComponentWeakReference.get() : null;
    }

    @Override
    public int size() {
        evictEmptyReferences();
        return cache.size();
    }

    @NonNull
    @Override
    public Collection<DeviceComponent> values() {
        final ArrayList<DeviceComponent> components = new ArrayList<>();

        for (DeviceComponentWeakReference deviceComponentWeakReference : cache.values()) {
            if (!deviceComponentWeakReference.isEmpty()) {
                components.add(deviceComponentWeakReference.get());
            }
        }

        return components;
    }

    private void evictEmptyReferences() {

        for (Iterator<Entry<String, DeviceComponentWeakReference>> iterator = cache.entrySet().iterator(); iterator.hasNext();) {
            final Entry<String, DeviceComponentWeakReference> next = iterator.next();

            if (next.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }
}