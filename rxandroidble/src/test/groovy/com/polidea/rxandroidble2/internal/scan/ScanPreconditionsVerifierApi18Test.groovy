package com.polidea.rxandroidble2.internal.scan

import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.internal.util.LocationServicesStatus
import com.polidea.rxandroidble2.internal.util.RxBleAdapterWrapper
import spock.lang.Specification
import spock.lang.Unroll

class ScanPreconditionsVerifierApi18Test extends Specification {

    private RxBleAdapterWrapper mockRxBleAdapterWrapper = Mock RxBleAdapterWrapper

    private LocationServicesStatus mockLocationServicesStatus = Mock LocationServicesStatus

    private ScanPreconditionsVerifierApi18 objectUnderTest = new ScanPreconditionsVerifierApi18(mockRxBleAdapterWrapper, mockLocationServicesStatus)

    private static final boolean[] TRUE_FALSE = [true, false]

    def "should perform checks in proper order"() {

        when:
        objectUnderTest.verify(true)

        then:
        1 * mockRxBleAdapterWrapper.hasBluetoothAdapter() >> true

        then:
        1 * mockRxBleAdapterWrapper.isBluetoothEnabled() >> true

        then:
        1 * mockLocationServicesStatus.isLocationPermissionOk() >> true

        then:
        1 * mockLocationServicesStatus.isLocationProviderOk() >> true
    }

    @Unroll
    def "should not throw any exception if all checks return true"() {

        given:
        mockRxBleAdapterWrapper.hasBluetoothAdapter() >> true
        mockRxBleAdapterWrapper.isBluetoothEnabled() >> true
        mockLocationServicesStatus.isLocationPermissionOk() >> true
        mockLocationServicesStatus.isLocationProviderOk() >> true

        when:
        objectUnderTest.verify(checkLocationServices)

        then:
        notThrown Throwable

        where:
        checkLocationServices << TRUE_FALSE
    }

    @Unroll
    def "should throw BleScanException.BLUETOOTH_NOT_AVAILABLE if no adapter is available"() {

        given:
        mockRxBleAdapterWrapper.hasBluetoothAdapter() >> false
        mockRxBleAdapterWrapper.isBluetoothEnabled() >> isBluetoothEnabled
        mockLocationServicesStatus.isLocationPermissionOk() >> isLocationPermissionOk
        mockLocationServicesStatus.isLocationProviderOk() >> isLocationProviderOk

        when:
        objectUnderTest.verify(checkLocationServices)

        then:
        BleScanException e = thrown(BleScanException)
        e.reason == BleScanException.BLUETOOTH_NOT_AVAILABLE

        where:
        [isBluetoothEnabled, isLocationPermissionOk, isLocationProviderOk, checkLocationServices] << [TRUE_FALSE, TRUE_FALSE, TRUE_FALSE, TRUE_FALSE].combinations()
    }

    @Unroll
    def "should throw BleScanException.BLUETOOTH_DISABLED if adapter is not enabled"() {

        given:
        mockRxBleAdapterWrapper.hasBluetoothAdapter() >> true
        mockRxBleAdapterWrapper.isBluetoothEnabled() >> false
        mockLocationServicesStatus.isLocationPermissionOk() >> isLocationPermissionOk
        mockLocationServicesStatus.isLocationProviderOk() >> isLocationProviderOk

        when:
        objectUnderTest.verify(checkLocationServices)

        then:
        BleScanException e = thrown(BleScanException)
        e.reason == BleScanException.BLUETOOTH_DISABLED

        where:
        [isLocationPermissionOk, isLocationProviderOk, checkLocationServices] << [TRUE_FALSE, TRUE_FALSE, TRUE_FALSE].combinations()
    }

    @Unroll
    def "should throw BleScanException.LOCATION_PERMISSION_MISSING if location permission is not granted"() {

        given:
        mockRxBleAdapterWrapper.hasBluetoothAdapter() >> true
        mockRxBleAdapterWrapper.isBluetoothEnabled() >> true
        mockLocationServicesStatus.isLocationPermissionOk() >> false
        mockLocationServicesStatus.isLocationProviderOk() >> isLocationProviderOk

        when:
        objectUnderTest.verify(checkLocationServices)

        then:
        BleScanException e = thrown(BleScanException)
        e.reason == BleScanException.LOCATION_PERMISSION_MISSING

        where:
        [isLocationProviderOk, checkLocationServices] << [TRUE_FALSE, TRUE_FALSE].combinations()
    }

    def "should throw BleScanException.LOCATION_SERVICES_DISABLED if location services are not enabled"() {

        given:
        mockRxBleAdapterWrapper.hasBluetoothAdapter() >> true
        mockRxBleAdapterWrapper.isBluetoothEnabled() >> true
        mockLocationServicesStatus.isLocationPermissionOk() >> true
        mockLocationServicesStatus.isLocationProviderOk() >> false

        when:
        objectUnderTest.verify(true)

        then:
        BleScanException e = thrown(BleScanException)
        e.reason == BleScanException.LOCATION_SERVICES_DISABLED
    }

    def "should not check if isLocationProviderOk if checkLocationServices is false"() {

        given:
        mockRxBleAdapterWrapper.hasBluetoothAdapter() >> true
        mockRxBleAdapterWrapper.isBluetoothEnabled() >> true
        mockLocationServicesStatus.isLocationPermissionOk() >> true
        0 * mockLocationServicesStatus.isLocationProviderOk() >> false

        when:
        objectUnderTest.verify(false)

        then:
        noExceptionThrown()
    }
}