package com.polidea.rxandroidble2.samplekotlin.example1_scanning

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Button
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.samplekotlin.R
import com.polidea.rxandroidble2.samplekotlin.SampleApplication
import com.polidea.rxandroidble2.samplekotlin.newDeviceActivity
import com.polidea.rxandroidble2.samplekotlin.util.showError
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

private const val REQUEST_PERMISSION_COARSE_LOCATION = 101

class ScanActivity : AppCompatActivity() {

    @BindView(R.id.scan_toggle_btn)
    internal lateinit var scanToggleButton: Button

    @BindView(R.id.scan_results)
    internal lateinit var recyclerView: RecyclerView

    private val rxBleClient = SampleApplication.rxBleClient

    private var scanDisposable: Disposable? = null

    private val resultsAdapter = ScanResultsAdapter { startActivity(newDeviceActivity(it.bleDevice.macAddress)) }

    private var hasClickedScan = false

    private val isScanning: Boolean
        get() = scanDisposable != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example1)
        ButterKnife.bind(this)
        configureResultList()
    }

    private fun configureResultList() {
        with(recyclerView) {
            setHasFixedSize(true)
            itemAnimator = null
            layoutManager = LinearLayoutManager(this@ScanActivity)
            adapter = resultsAdapter
        }
    }

    private fun onAdapterItemClick(scanResult: ScanResult) {
        startActivity(newDeviceActivity(scanResult.bleDevice.macAddress))
    }

    @OnClick(R.id.background_scan_btn)
    fun onBackgroundScanRequested() {
        // TODO
//        startActivity(newBackgroundScanActivity())
    }

    @OnClick(R.id.scan_toggle_btn)
    fun onScanToggleClick() {
        if (isScanning) {
            scanDisposable?.dispose()
        } else {
            if (checkLocationPermission()) {
                scanBleDevices()
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally { dispose() }
                    .subscribe({ resultsAdapter.addScanResult(it) }, { onScanFailure(it) })
                    .let { scanDisposable = it }
            } else {
                hasClickedScan = true
                requestLocationPermission(REQUEST_PERMISSION_COARSE_LOCATION)
            }
        }
        updateButtonUIState()
    }

    private fun scanBleDevices(): Observable<ScanResult> {
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        val scanFilter = ScanFilter.Builder()
//            .setDeviceAddress("B4:99:4C:34:DC:8B")
            // add custom filters if needed
            .build()

        return rxBleClient.scanBleDevices(scanSettings, scanFilter)
    }

    private fun dispose() {
        scanDisposable = null
        resultsAdapter.clearScanResults()
        updateButtonUIState()
    }

    private fun onScanFailure(throwable: Throwable) {
        if (throwable is BleScanException) showError(throwable)
    }

    private fun updateButtonUIState() {
        scanToggleButton.setText(if (isScanning) R.string.button_stop_scan else R.string.button_start_scan)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSION_COARSE_LOCATION &&
            isLocationPermissionGranted(permissions, grantResults) &&
            hasClickedScan
        ) {
            hasClickedScan = false
            scanBleDevices()
        }
    }

    public override fun onPause() {
        super.onPause()
        // Stop scanning in onPause callback.
        if (isScanning) scanDisposable?.dispose()
    }
}

private fun Context.checkLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED

private fun Activity.requestLocationPermission(requestCode: Int) {
    ActivityCompat.requestPermissions(this, arrayOf(ACCESS_COARSE_LOCATION), requestCode)
}

private fun isLocationPermissionGranted(permissions: Array<String>, grantResults: IntArray): Boolean {
    permissions.forEachIndexed { index, permission ->
        if (permission == ACCESS_COARSE_LOCATION && grantResults[index] == PERMISSION_GRANTED) return true
    }
    return false
}