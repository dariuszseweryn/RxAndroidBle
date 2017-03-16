package com.polidea.rxandroidble.internal.cache

import com.polidea.rxandroidble.internal.DeviceComponent


class MockDeviceReferenceProvider implements DeviceComponentWeakReference.Provider {


    private final HashMap<DeviceComponent, List<MockDeviceComponentWeakReference>> devices = new HashMap<>()

    class MockDeviceComponentWeakReference extends DeviceComponentWeakReference {

        MockDeviceComponentWeakReference(DeviceComponent device) {
            super(device)
        }

        public release() {
            clear()
        }

        @Override
        boolean isEmpty() {
            return super.isEmpty()
        }
    }

    @Override
    DeviceComponentWeakReference provide(DeviceComponent component) {
        def reference = new MockDeviceComponentWeakReference(component)
        storeReference(component, reference)
        return reference
    }

    public releaseReferenceFor(DeviceComponent component) {
        devices.get(component)?.each { it.release() }
    }

    private storeReference(DeviceComponent component, MockDeviceComponentWeakReference reference) {

        if (devices.containsKey(component)) {
            devices.get(component).add(reference)
        } else {
            devices.put(component, [reference])
        }
    }
}
