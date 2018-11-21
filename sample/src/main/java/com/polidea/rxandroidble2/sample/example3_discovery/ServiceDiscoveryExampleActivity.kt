package com.polidea.rxandroidble2.sample.example3_discovery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.Button
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.sample.R
import com.polidea.rxandroidble2.sample.SampleApplication
import com.polidea.rxandroidble2.sample.example4_characteristic.newCharacteristicOperationExampleActivity
import com.polidea.rxandroidble2.sample.util.isConnected
import com.trello.rxlifecycle2.android.ActivityEvent.PAUSE
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

private const val EXTRA_MAC_ADDRESS = "extra_mac_address"

internal fun Context.newServiceDiscoveryExampleActivity(macAddress: String) =
    Intent(this, ServiceDiscoveryExampleActivity::class.java).apply {
        putExtra(EXTRA_MAC_ADDRESS, macAddress)
    }

class ServiceDiscoveryExampleActivity : RxAppCompatActivity() {

    @BindView(R.id.connect)
    internal lateinit var connectButton: Button

    @BindView(R.id.scan_results)
    internal lateinit var recyclerView: RecyclerView

    private var resultsAdapter = DiscoveryResultsAdapter()

    private lateinit var bleDevice: RxBleDevice

    private lateinit var macAddress: String

    private var connectionDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example3)
        ButterKnife.bind(this)

        macAddress = intent.getStringExtra(EXTRA_MAC_ADDRESS)
        supportActionBar!!.subtitle = getString(R.string.mac_address, macAddress)
        bleDevice = SampleApplication.rxBleClient.getBleDevice(macAddress)
        configureResultList()
    }

    @OnClick(R.id.connect)
    fun onConnectToggleClick() {
        connectionDisposable = bleDevice.establishConnection(false)
            .flatMapSingle { it.discoverServices() }
            .take(1) // Disconnect automatically after discovery
            .compose(bindUntilEvent(PAUSE))
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { updateUI() }
            .subscribe(
                { resultsAdapter.swapScanResult(it) },
                { onConnectionFailure(it) }
            )
        updateUI()
    }

    private fun configureResultList() {
        with(recyclerView) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@ServiceDiscoveryExampleActivity)
            adapter = resultsAdapter
        }
        resultsAdapter.onAdapterItemClickListener = View.OnClickListener { view ->
            val childAdapterPosition = recyclerView.getChildAdapterPosition(view)
            val itemAtPosition = resultsAdapter.getItem(childAdapterPosition)
            onAdapterItemClick(itemAtPosition)
        }
    }

    private fun onAdapterItemClick(item: DiscoveryResultsAdapter.AdapterItem) {
        if (item.type == DiscoveryResultsAdapter.AdapterItem.CHARACTERISTIC) {
            startActivity(newCharacteristicOperationExampleActivity(macAddress, item.uuid))
            // If you want to check the alternative advanced implementation comment out the line above and uncomment one below
//            startActivity(newAdvancedCharacteristicOperationExampleActivity(macAddress, item.uuid))
        } else {
            Snackbar.make(findViewById<View>(android.R.id.content), R.string.not_clickable, Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun onConnectionFailure(throwable: Throwable) {
        Snackbar.make(findViewById<View>(android.R.id.content), "Connection error: $throwable", Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun updateUI() {
        connectButton.isEnabled = !bleDevice.isConnected
    }

    override fun onPause() {
        super.onPause()
        connectionDisposable?.dispose()
    }
}
