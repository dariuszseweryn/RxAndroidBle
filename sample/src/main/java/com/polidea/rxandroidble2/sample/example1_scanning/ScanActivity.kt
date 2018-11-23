package com.polidea.rxandroidble2.sample.example1_scanning

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.Button
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.sample.R
import com.polidea.rxandroidble2.sample.SampleApplication
import com.polidea.rxandroidble2.sample.example1a_background_scanning.newBackgroundScanActivity
import com.polidea.rxandroidble2.sample.newDeviceActivity
import com.polidea.rxandroidble2.sample.util.checkLocationPermissionGranted
import com.polidea.rxandroidble2.sample.util.handleException
import com.polidea.rxandroidble2.sample.util.isRequestLocationPermissionGranted
import com.polidea.rxandroidble2.sample.util.requestLocationPermission
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

class ScanActivity : AppCompatActivity() {

    @BindView(R.id.scan_toggle_btn)
    internal lateinit var scanToggleButton: Button

    @BindView(R.id.scan_results)
    internal lateinit var recyclerView: RecyclerView

    private val rxBleClient = SampleApplication.rxBleClient

    private var scanDisposable: Disposable? = null

    private var resultsAdapter = ScanResultsAdapter()

    private var hasClickedScan = false

    private val isScanning: Boolean
        get() = scanDisposable != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example1)
        ButterKnife.bind(this)
        configureResultList()
    }

    @OnClick(R.id.background_scan_btn)
    fun onBackgroundScanRequested() {
        startActivity(newBackgroundScanActivity())
    }

    @OnClick(R.id.scan_toggle_btn)
    fun onScanToggleClick() {
        if (isScanning) {
            scanDisposable?.dispose()
        } else {
            if (checkLocationPermissionGranted()) {
                scanBleDevices()
            } else {
                hasClickedScan = true
                requestLocationPermission()
            }
        }
        updateButtonUIState()
    }

    private fun scanBleDevices() {
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        val scanFilter = ScanFilter.Builder()
//            .setDeviceAddress("B4:99:4C:34:DC:8B")
            // add custom filters if needed
            .build()

        rxBleClient.scanBleDevices(scanSettings, scanFilter)
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { dispose() }
            .subscribe({ resultsAdapter.addScanResult(it) }, { onScanFailure(it) })
            .let { scanDisposable = it }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (isRequestLocationPermissionGranted(requestCode, permissions, grantResults) && hasClickedScan) {
            hasClickedScan = false
            scanBleDevices()
        }
    }

    public override fun onPause() {
        super.onPause()
        // Stop scanning in onPause callback. You can use rxlifecycle for convenience. Examples are provided later.
        if (isScanning) scanDisposable?.dispose()
    }

    private fun configureResultList() {
        with(recyclerView) {
            setHasFixedSize(true)
            itemAnimator = null
            layoutManager = LinearLayoutManager(this@ScanActivity)
            adapter = resultsAdapter
        }
        resultsAdapter.onAdapterItemClickListener = View.OnClickListener { view ->
            recyclerView.getChildAdapterPosition(view).let {
                val itemAtPosition = resultsAdapter.itemAtPosition(it)
                onAdapterItemClick(itemAtPosition)
            }
        }
    }

    private fun onAdapterItemClick(scanResult: ScanResult) {
        @Suppress("ReplaceSingleLineLet")
        scanResult.bleDevice.macAddress.let { startActivity(newDeviceActivity(it)) }
    }

    private fun onScanFailure(throwable: Throwable) {
        if (throwable is BleScanException) handleException(throwable)
    }

    private fun dispose() {
        scanDisposable = null
        resultsAdapter.clearScanResults()
        updateButtonUIState()
    }

    private fun updateButtonUIState() {
        scanToggleButton.setText(if (isScanning) R.string.stop_scan else R.string.start_scan)
    }
}
