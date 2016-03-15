package com.polidea.rxandroidble.internal

import com.polidea.rxandroidble.RxBleDevice
import com.polidea.rxandroidble.internal.cache.RxBleDeviceCache
import spock.lang.Specification

class RxBleDeviceCacheTest extends Specification {
    def deviceReferenceProvider = new MockDeviceReferenceProvider()
    def objectUnderTest = new RxBleDeviceCache(deviceReferenceProvider)

    def "should get cached item if is hard referenced"() {
        given:
        def cacheKey = "someKey"
        def cachedDevice = Mock RxBleDevice

        when:
        objectUnderTest.put(cacheKey, cachedDevice)
        def deviceFromCache = objectUnderTest.get(cacheKey)

        then:
        assertCacheSizeIs 1
        assertCacheIsNotEmpty()
        assertCacheContainsKey(cacheKey)
        assertCacheContainsValue(cachedDevice)
        deviceFromCache == cachedDevice
    }

    def "should get all cached items if are hard referenced"() {
        given:
        def cacheKey = "someKey"
        def cachedDevice = Mock RxBleDevice
        def secondCacheKey = "someSecondKey"
        def secondCachedDevice = Mock RxBleDevice

        when:
        objectUnderTest.putAll(["someKey": cachedDevice, "someSecondKey": secondCachedDevice])

        then:
        def deviceFromCache = objectUnderTest.get(cacheKey)
        def secondDeviceFromCache = objectUnderTest.get(secondCacheKey)
        assertCacheIsNotEmpty()
        assertCacheSizeIs 2
        deviceFromCache == cachedDevice
        secondDeviceFromCache == secondCachedDevice
    }

    def "should return null if cached item was garbage collected"() {
        given:
        def cacheKey = "someKey"
        def cachedDevice = Mock RxBleDevice
        objectUnderTest.put(cacheKey, cachedDevice)
        deviceReferenceProvider.releaseReferenceFor cachedDevice

        when:
        def deviceFromCache = objectUnderTest.get(cacheKey)

        then:
        assertCacheSizeIs 0
        assertCacheIsEmpty()
        !assertCacheContainsKey(cacheKey)
        !assertCacheContainsValue(cachedDevice)
        deviceFromCache == null
    }

    def "should not contain value after cache was cleared"() {
        given:
        def cacheKey = "someKey"
        def cachedDevice = Mock RxBleDevice
        objectUnderTest.put(cacheKey, cachedDevice)

        when:
        objectUnderTest.clear()

        then:
        def deviceFromCache = objectUnderTest.get(cacheKey)
        assertCacheIsEmpty()
        deviceFromCache == null
        !assertCacheContainsKey(cacheKey)
        !assertCacheContainsValue(cachedDevice)
    }

    def "should return null if entry was removed"() {
        given:
        def cacheKey = "someKey"
        def cachedDevice = Mock RxBleDevice
        objectUnderTest.put(cacheKey, cachedDevice)

        when:
        objectUnderTest.remove(cacheKey)

        then:
        def deviceFromCache = objectUnderTest.get(cacheKey)
        assertCacheIsEmpty()
        !assertCacheContainsKey(cacheKey)
        !assertCacheContainsValue(cachedDevice)
        deviceFromCache == null
    }

    public assertCacheContainsValue(RxBleDevice cachedDevice) {
        objectUnderTest.containsValue(cachedDevice)
    }

    public assertCacheContainsKey(String cacheKey) {
        objectUnderTest.containsKey(cacheKey)
    }

    public assertCacheIsNotEmpty() {
        !assertCacheIsEmpty()
    }

    public assertCacheIsEmpty() {
        objectUnderTest.isEmpty()
    }

    public assertCacheSizeIs(int expectedCacheSize) {
        objectUnderTest.size() == expectedCacheSize
    }
}
