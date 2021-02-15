package com.polidea.rxandroidble3.internal.scan

import android.app.PendingIntent
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Build
import com.polidea.rxandroidble3.exceptions.BleScanException
import com.polidea.rxandroidble3.internal.util.RxBleAdapterWrapper
import com.polidea.rxandroidble3.scan.ScanFilter
import com.polidea.rxandroidble3.scan.ScanSettings
import hkhc.electricspock.ElectricSpecification
import org.robolectric.annotation.Config

import static com.polidea.rxandroidble3.scan.ScanCallbackType.CALLBACK_TYPE_ALL_MATCHES

@Config(manifest = Config.NONE, sdk = Build.VERSION_CODES.O)
class BackgroundScannerTest extends ElectricSpecification {
    public static final int SUCCESS_CODE = 0
    BackgroundScannerImpl objectUnderTest
    RxBleAdapterWrapper adapterWrapper
    AndroidScanObjectsConverter androidScanObjectsConverter

    def setup() {
        adapterWrapper = Mock(RxBleAdapterWrapper)
        androidScanObjectsConverter = Mock(AndroidScanObjectsConverter)
        def scanResultConverter = Mock(InternalToExternalScanResultConverter)
        def internalScanResultCreator = Mock(InternalScanResultCreator)
        objectUnderTest = new BackgroundScannerImpl(adapterWrapper,
                androidScanObjectsConverter,
                internalScanResultCreator,
                scanResultConverter
        )
        androidScanObjectsConverter.toNativeFilters(_) >> Collections.emptyList()
        androidScanObjectsConverter.toNativeSettings(_) >> null
    }

    def "should throw BleScanException if scan wasn't able to start"() {
        given:
        def pendingIntent = Mock(PendingIntent)
        def settings = Mock(ScanSettings)
        def scanFilter = emptyFilters()
        adapterWrapper.isBluetoothEnabled() >> true
        1 * adapterWrapper.startLeScan(_, _, _) >> errorCode

        when:
        objectUnderTest.scanBleDeviceInBackground(pendingIntent, settings, scanFilter)

        then:
        def scanException = thrown(BleScanException)
        scanException.reason == errorCode

        where:
        errorCode << [ScanCallback.SCAN_FAILED_ALREADY_STARTED, ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED]
    }

    def "should throw BleScanException if bluetooth is OFF while calling `.scanBleDeviceInBackground()`"() {
        given:
        def pendingIntent = Mock(PendingIntent)
        def settings = Mock(ScanSettings)
        def scanFilter = emptyFilters()
        adapterWrapper.isBluetoothEnabled() >> false

        when:
        objectUnderTest.scanBleDeviceInBackground(pendingIntent, settings, scanFilter)

        then:
        def scanException = thrown(BleScanException)

        and:
        scanException.reason == BleScanException.BLUETOOTH_DISABLED
    }

    def "should not throw if bluetooth is OFF while calling `.stopBackgroundBleScan()`"() {
        given:
        def pendingIntent = Mock(PendingIntent)
        adapterWrapper.isBluetoothEnabled() >> false

        when:
        objectUnderTest.stopBackgroundBleScan(pendingIntent)

        then:
        noExceptionThrown()
    }

    def "should not call RxBleAdapterWrapper.stopLeScan() if bluetooth is OFF while calling `.stopBackgroundBleScan()`"() {
        given:
        def pendingIntent = Mock(PendingIntent)
        adapterWrapper.isBluetoothEnabled() >> false

        when:
        objectUnderTest.stopBackgroundBleScan(pendingIntent)

        then:
        0 * adapterWrapper.stopLeScan(pendingIntent)
    }

    def "should pass callback intent to a wrapper"() {
        given:
        def pendingIntent = Mock(PendingIntent)
        def settings = Mock(ScanSettings)
        def scanFilter = emptyFilters()
        adapterWrapper.isBluetoothEnabled() >> true

        when:
        objectUnderTest.scanBleDeviceInBackground(pendingIntent, settings, scanFilter)

        then:
        1 * adapterWrapper.startLeScan(_, _, pendingIntent) >> SUCCESS_CODE
    }

    private static ScanFilter emptyFilters() {
        return new ScanFilter.Builder().build()
    }

    def "should start background le scan with mapped filters and scan settings"() {
        given:
        def pendingIntent = Mock(PendingIntent)
        def settings = Mock(ScanSettings)
        def scanFilter = emptyFilters()
        def nativeFilters = [Mock(android.bluetooth.le.ScanFilter)]
        def nativeSettings = Mock(android.bluetooth.le.ScanSettings)
        adapterWrapper.isBluetoothEnabled() >> true

        when:
        objectUnderTest.scanBleDeviceInBackground(pendingIntent, settings, scanFilter)

        then:
        androidScanObjectsConverter.toNativeFilters(scanFilter) >> nativeFilters
        androidScanObjectsConverter.toNativeSettings(settings) >> nativeSettings

        1 * adapterWrapper.startLeScan(nativeFilters, nativeSettings, _) >> SUCCESS_CODE
    }

    def "should pass callback intent when stopping scan"() {
        given:
        def pendingIntent = Mock(PendingIntent)
        adapterWrapper.isBluetoothEnabled() >> true

        when:
        objectUnderTest.stopBackgroundBleScan(pendingIntent)

        then:
        1 * adapterWrapper.stopLeScan(pendingIntent)
    }

    def "should throw exception if received result contains an error, error code should be mapped"() {
        when:
        objectUnderTest.onScanResultReceived(prepareIntentWithScanResultError(errorCode))

        then:
        def scanException = thrown(BleScanException)
        scanException.reason == errorCode

        where:
        errorCode << [ScanCallback.SCAN_FAILED_ALREADY_STARTED, ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED]
    }

    def "should map received empty scan result to an empty list"() {
        when:
        def scanResultReceived = objectUnderTest.onScanResultReceived(prepareIntentWithEmptyResult())

        then:
        scanResultReceived.isEmpty()
    }

    def "should map received scan result a list with scan result models"() {
        when:
        def scanResultReceived = objectUnderTest.onScanResultReceived(prepareIntentWithResults(
                Mock(ScanResult), Mock(ScanResult)
        ))

        then:
        scanResultReceived.size() == 2
    }


    private static Intent prepareIntentWithScanResultError(int errorCode) {
        def intent = new Intent()
        intent.putExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, errorCode)
        return intent
    }

    private static Intent prepareIntentWithEmptyResult() {
        def intent = new Intent()
        intent.putExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, BackgroundScannerImpl.NO_ERROR)
        intent.putExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, CALLBACK_TYPE_ALL_MATCHES)
        intent.putExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT, [])
        return intent
    }

    private static Intent prepareIntentWithResults(ScanResult... scanResults) {
        def intent = new Intent()
        intent.putExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, BackgroundScannerImpl.NO_ERROR)
        intent.putExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, CALLBACK_TYPE_ALL_MATCHES)
        intent.putExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT, scanResults.toList())
        return intent
    }
}