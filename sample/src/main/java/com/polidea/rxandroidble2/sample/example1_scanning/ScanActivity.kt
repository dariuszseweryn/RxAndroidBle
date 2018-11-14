package com.polidea.rxandroidble2.sample.example1_scanning

import android.content.Intent
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
import com.polidea.rxandroidble2.sample.DeviceActivity
import com.polidea.rxandroidble2.sample.R
import com.polidea.rxandroidble2.sample.SampleApplication
import com.polidea.rxandroidble2.sample.example1a_background_scanning.BackgroundScanActivity
import com.polidea.rxandroidble2.sample.util.LocationPermission
import com.polidea.rxandroidble2.sample.util.ScanExceptionHandler
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

class ScanActivity : AppCompatActivity() {

    @BindView(R.id.scan_toggle_btn)
    internal var scanToggleButton: Button? = null
    @BindView(R.id.scan_results)
    internal var recyclerView: RecyclerView? = null
    private val rxBleClient = SampleApplication.rxBleClient
    private var scanDisposable: Disposable? = null
    private var resultsAdapter: ScanResultsAdapter? = null
    private var hasClickedScan: Boolean = false

    private val isScanning: Boolean
        get() = scanDisposable != null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example1)
        ButterKnife.bind(this)
        configureResultList()
    }

    @OnClick(R.id.background_scan_btn)
    fun onBackgroundScanRequested() {
        startActivity(Intent(this, BackgroundScanActivity::class.java))
    }

    @OnClick(R.id.scan_toggle_btn)
    fun onScanToggleClick() {

        if (isScanning) {
            scanDisposable!!.dispose()
        } else {
            if (LocationPermission.checkLocationPermissionGranted(this)) {
                scanBleDevices()
            } else {
                hasClickedScan = true
                LocationPermission.requestLocationPermission(this)
            }
        }

        updateButtonUIState()
    }

    private fun scanBleDevices() {
        scanDisposable = rxBleClient.scanBleDevices(
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build(),
            ScanFilter.Builder()
                //                            .setDeviceAddress("B4:99:4C:34:DC:8B")
                // add custom filters if needed
                .build()
        )
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { dispose() }
            .subscribe(
                { resultsAdapter!!.addScanResult(it) },
                { this.onScanFailure(it) }
            )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (LocationPermission.isRequestLocationPermissionGranted(
                requestCode,
                permissions,
                grantResults
            ) && hasClickedScan
        ) {
            hasClickedScan = false
            scanBleDevices()
        }
    }

    public override fun onPause() {
        super.onPause()

        if (isScanning) {
            /*
             * Stop scanning in onPause callback. You can use rxlifecycle for convenience. Examples are provided later.
             */
            scanDisposable!!.dispose()
        }
    }

    private fun configureResultList() {
        recyclerView!!.setHasFixedSize(true)
        recyclerView!!.itemAnimator = null
        val recyclerLayoutManager = LinearLayoutManager(this)
        recyclerView!!.layoutManager = recyclerLayoutManager
        resultsAdapter = ScanResultsAdapter()
        recyclerView!!.adapter = resultsAdapter
        resultsAdapter!!.setOnAdapterItemClickListener { view: View ->
                val childAdapterPosition = recyclerView!!.getChildAdapterPosition(view)
                val itemAtPosition = resultsAdapter!!.getItemAtPosition(childAdapterPosition)
                onAdapterItemClick(itemAtPosition)
        }
    }

    private fun onAdapterItemClick(scanResults: ScanResult) {
        val macAddress = scanResults.bleDevice.macAddress
        val intent = Intent(this, DeviceActivity::class.java)
        intent.putExtra(DeviceActivity.EXTRA_MAC_ADDRESS, macAddress)
        startActivity(intent)
    }

    private fun onScanFailure(throwable: Throwable) {
        if (throwable is BleScanException) {
            ScanExceptionHandler.handleException(this, throwable)
        }
    }

    private fun dispose() {
        scanDisposable = null
        resultsAdapter!!.clearScanResults()
        updateButtonUIState()
    }

    private fun updateButtonUIState() {
        scanToggleButton!!.setText(if (isScanning) R.string.stop_scan else R.string.start_scan)
    }
}
