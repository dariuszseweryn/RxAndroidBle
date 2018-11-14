package com.polidea.rxandroidble2.sample.example3_discovery

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Button

import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.sample.DeviceActivity
import com.polidea.rxandroidble2.sample.R
import com.polidea.rxandroidble2.sample.SampleApplication
import com.polidea.rxandroidble2.sample.example4_characteristic.CharacteristicOperationExampleActivity
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity

import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

import com.trello.rxlifecycle2.android.ActivityEvent.PAUSE

class ServiceDiscoveryExampleActivity : RxAppCompatActivity() {

    @BindView(R.id.connect)
    internal var connectButton: Button? = null
    @BindView(R.id.scan_results)
    internal var recyclerView: RecyclerView? = null
    private var adapter: DiscoveryResultsAdapter? = null
    private var bleDevice: RxBleDevice? = null
    private var macAddress: String? = null
    private var connectionDisposable: Disposable? = null

    private val isConnected: Boolean
        get() = bleDevice!!.connectionState == RxBleConnection.RxBleConnectionState.CONNECTED

    @OnClick(R.id.connect)
    fun onConnectToggleClick() {
        connectionDisposable = bleDevice!!.establishConnection(false)
            .flatMapSingle<RxBleDeviceServices>(Function<RxBleConnection, SingleSource<out RxBleDeviceServices>> { it.discoverServices() })
            .take(1) // Disconnect automatically after discovery
            .compose<RxBleDeviceServices>(bindUntilEvent<RxBleDeviceServices>(PAUSE))
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally(Action { this.updateUI() })
            .subscribe(
                Consumer<RxBleDeviceServices> { adapter!!.swapScanResult(it) },
                Consumer<Throwable> { this.onConnectionFailure(it) })

        updateUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example3)
        ButterKnife.bind(this)
        macAddress = intent.getStringExtra(DeviceActivity.EXTRA_MAC_ADDRESS)

        supportActionBar!!.subtitle = getString(R.string.mac_address, macAddress)
        bleDevice = SampleApplication.rxBleClient.getBleDevice(macAddress!!)
        configureResultList()
    }

    private fun configureResultList() {
        recyclerView!!.setHasFixedSize(true)
        val recyclerLayoutManager = LinearLayoutManager(this)
        recyclerView!!.layoutManager = recyclerLayoutManager
        adapter = DiscoveryResultsAdapter()
        recyclerView!!.adapter = adapter
        adapter!!.setOnAdapterItemClickListener { view ->
            val childAdapterPosition = recyclerView!!.getChildAdapterPosition(view)
            val itemAtPosition = adapter!!.getItem(childAdapterPosition)
            onAdapterItemClick(itemAtPosition)
        }
    }

    private fun onAdapterItemClick(item: DiscoveryResultsAdapter.AdapterItem) {

        if (item.type == DiscoveryResultsAdapter.AdapterItem.CHARACTERISTIC) {
            val intent = Intent(this, CharacteristicOperationExampleActivity::class.java)
            // If you want to check the alternative advanced implementation comment out the line above and uncomment one below
            // final Intent intent = new Intent(this, AdvancedCharacteristicOperationExampleActivity.class);
            intent.putExtra(DeviceActivity.EXTRA_MAC_ADDRESS, macAddress)
            intent.putExtra(CharacteristicOperationExampleActivity.EXTRA_CHARACTERISTIC_UUID, item.uuid)
            startActivity(intent)
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
        connectButton!!.isEnabled = !isConnected
    }

    override fun onPause() {
        super.onPause()

        if (connectionDisposable != null) {
            connectionDisposable!!.dispose()
        }
    }
}
