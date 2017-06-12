package com.polidea.rxandroidble.internal.cache;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.polidea.rxandroidble.ClientScope;
import com.polidea.rxandroidble.internal.DeviceComponent;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func1;

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
        return Observable.from(cache.entrySet())
                .filter(new Func1<Entry<String, DeviceComponentWeakReference>, Boolean>() {
                    @Override
                    public Boolean call(Entry<String, DeviceComponentWeakReference> stringDeviceWeakReferenceEntry) {
                        return !stringDeviceWeakReferenceEntry.getValue().isEmpty();
                    }
                })
                .map(new Func1<Entry<String, DeviceComponentWeakReference>, CacheEntry>() {
                    @Override
                    public CacheEntry call(Entry<String, DeviceComponentWeakReference> stringDeviceWeakReferenceEntry) {
                        return new CacheEntry(
                                stringDeviceWeakReferenceEntry.getKey(),
                                deviceComponentReferenceProvider.provide(stringDeviceWeakReferenceEntry.getValue().get())
                        );
                    }
                })
                .collect(
                        new Func0<HashSet<Entry<String, DeviceComponent>>>() {
                            @Override
                            public HashSet<Entry<String, DeviceComponent>> call() {
                                return new HashSet<>();
                            }
                        },
                        new Action2<HashSet<Entry<String, DeviceComponent>>, CacheEntry>() {
                            @Override
                            public void call(HashSet<Entry<String, DeviceComponent>> entries, CacheEntry e) {
                                entries.add(e);
                            }
                        }
                )
                .toBlocking().first();
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
        return Observable.from(cache.values())
                .filter(new Func1<DeviceComponentWeakReference, Boolean>() {
                    @Override
                    public Boolean call(DeviceComponentWeakReference deviceComponentWeakReference) {
                        return !deviceComponentWeakReference.isEmpty();
                    }
                })
                .map(new Func1<DeviceComponentWeakReference, DeviceComponent>() {
                    @Override
                    public DeviceComponent call(DeviceComponentWeakReference deviceComponentWeakReference) {
                        return deviceComponentWeakReference.get();
                    }
                })
                .toList()
                .toBlocking()
                .first();
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