package com.polidea.rxandroidble.internal.scan

import com.polidea.rxandroidble.exceptions.BleScanException
import com.polidea.rxandroidble.internal.util.LocationServicesStatus
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper
import spock.lang.Specification
import spock.lang.Unroll

public class ScanPreconditionsVerifierApi18Test extends Specification {

    private RxBleAdapterWrapper mockRxBleAdapterWrapper = Mock RxBleAdapterWrapper

    private LocationServicesStatus mockLocationServicesStatus = Mock LocationServicesStatus

    private ScanPreconditionsVerifierApi18 objectUnderTest = new ScanPreconditionsVerifierApi18(mockRxBleAdapterWrapper, mockLocationServicesStatus)

    private static final boolean[] TRUE_FALSE = [true, false]

    def "should perform checks in proper order"() {

        when:
        objectUnderTest.verify()

        then:
        1 * mockRxBleAdapterWrapper.hasBluetoothAdapter() >> true

        then:
        1 * mockRxBleAdapterWrapper.isBluetoothEnabled() >> true

        then:
        1 * mockLocationServicesStatus.isLocationPermissionOk() >> true

        then:
        1 * mockLocationServicesStatus.isLocationProviderOk() >> true
    }

    def "should not throw any exception if all checks return true"() {

        given:
        mockRxBleAdapterWrapper.hasBluetoothAdapter() >> true
        mockRxBleAdapterWrapper.isBluetoothEnabled() >> true
        mockLocationServicesStatus.isLocationPermissionOk() >> true
        mockLocationServicesStatus.isLocationProviderOk() >> true

        when:
        objectUnderTest.verify()

        then:
        notThrown Throwable
    }

    @Unroll
    def "should throw BleScanException.BLUETOOTH_NOT_AVAILABLE if no adapter is available"() {

        given:
        mockRxBleAdapterWrapper.hasBluetoothAdapter() >> false
        mockRxBleAdapterWrapper.isBluetoothEnabled() >> isBluetoothEnabled
        mockLocationServicesStatus.isLocationPermissionOk() >> isLocationPermissionOk
        mockLocationServicesStatus.isLocationProviderOk() >> isLocationProviderOk

        when:
        objectUnderTest.verify()

        then:
        BleScanException e = thrown(BleScanException)
        e.reason == BleScanException.BLUETOOTH_NOT_AVAILABLE

        where:
        [isBluetoothEnabled, isLocationPermissionOk, isLocationProviderOk] << [TRUE_FALSE, TRUE_FALSE, TRUE_FALSE].combinations()
    }

    @Unroll
    def "should throw BleScanException.BLUETOOTH_DISABLED if adapter is not enabled"() {

        given:
        mockRxBleAdapterWrapper.hasBluetoothAdapter() >> true
        mockRxBleAdapterWrapper.isBluetoothEnabled() >> false
        mockLocationServicesStatus.isLocationPermissionOk() >> isLocationPermissionOk
        mockLocationServicesStatus.isLocationProviderOk() >> isLocationProviderOk

        when:
        objectUnderTest.verify()

        then:
        BleScanException e = thrown(BleScanException)
        e.reason == BleScanException.BLUETOOTH_DISABLED

        where:
        [isLocationPermissionOk, isLocationProviderOk] << [TRUE_FALSE, TRUE_FALSE].combinations()
    }

    @Unroll
    def "should throw BleScanException.LOCATION_PERMISSION_MISSING if location permission is not granted"() {

        given:
        mockRxBleAdapterWrapper.hasBluetoothAdapter() >> true
        mockRxBleAdapterWrapper.isBluetoothEnabled() >> true
        mockLocationServicesStatus.isLocationPermissionOk() >> false
        mockLocationServicesStatus.isLocationProviderOk() >> isLocationProviderOk

        when:
        objectUnderTest.verify()

        then:
        BleScanException e = thrown(BleScanException)
        e.reason == BleScanException.LOCATION_PERMISSION_MISSING

        where:
        [isLocationProviderOk] << [TRUE_FALSE].combinations()
    }

    def "should throw BleScanException.LOCATION_SERVICES_DISABLED if location services are not enabled"() {

        given:
        mockRxBleAdapterWrapper.hasBluetoothAdapter() >> true
        mockRxBleAdapterWrapper.isBluetoothEnabled() >> true
        mockLocationServicesStatus.isLocationPermissionOk() >> true
        mockLocationServicesStatus.isLocationProviderOk() >> false

        when:
        objectUnderTest.verify()

        then:
        BleScanException e = thrown(BleScanException)
        e.reason == BleScanException.LOCATION_SERVICES_DISABLED
    }
}