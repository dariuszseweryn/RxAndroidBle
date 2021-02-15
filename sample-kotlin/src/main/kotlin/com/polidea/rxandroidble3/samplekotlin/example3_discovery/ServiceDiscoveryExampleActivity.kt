package com.polidea.rxandroidble3.samplekotlin.example3_discovery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.polidea.rxandroidble3.RxBleDevice
import com.polidea.rxandroidble3.samplekotlin.R
import com.polidea.rxandroidble3.samplekotlin.SampleApplication
import com.polidea.rxandroidble3.samplekotlin.example3_discovery.DiscoveryResultsAdapter.AdapterItem
import com.polidea.rxandroidble3.samplekotlin.example4_characteristic.CharacteristicOperationExampleActivity
import com.polidea.rxandroidble3.samplekotlin.util.isConnected
import com.polidea.rxandroidble3.samplekotlin.util.showSnackbarShort
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_example3.connect
import kotlinx.android.synthetic.main.activity_example3.scan_results

private const val EXTRA_MAC_ADDRESS = "extra_mac_address"

class ServiceDiscoveryExampleActivity : AppCompatActivity() {

    companion object {
        fun newInstance(context: Context, macAddress: String) =
            Intent(context, ServiceDiscoveryExampleActivity::class.java).apply {
                putExtra(EXTRA_MAC_ADDRESS, macAddress)
            }
    }

    private lateinit var bleDevice: RxBleDevice

    private lateinit var macAddress: String

    private val resultsAdapter = DiscoveryResultsAdapter { onAdapterItemClick(it) }

    private val discoveryDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example3)
        connect.setOnClickListener { onConnectToggleClick() }

        macAddress = intent.getStringExtra(EXTRA_MAC_ADDRESS)!!
        supportActionBar!!.subtitle = getString(R.string.mac_address, macAddress)
        bleDevice = SampleApplication.rxBleClient.getBleDevice(macAddress)

        scan_results.apply {
            setHasFixedSize(true)
            adapter = resultsAdapter
        }
    }

    private fun onConnectToggleClick() {
        bleDevice.establishConnection(false)
            .flatMapSingle { it.discoverServices() }
            .take(1) // Disconnect automatically after discovery
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { updateUI() }
            .doFinally { updateUI() }
            .subscribe({ resultsAdapter.swapScanResult(it) }, { showSnackbarShort("Connection error: $it") })
            .let { discoveryDisposable.add(it) }
    }

    private fun onAdapterItemClick(item: AdapterItem) {
        when (item.type) {
            AdapterItem.CHARACTERISTIC -> {
                startActivity(CharacteristicOperationExampleActivity.newInstance(this, macAddress, item.uuid))
                // If you want to check the alternative advanced implementation comment out the line above and uncomment one below
//            startActivity(AdvancedCharacteristicOperationExampleActivity.newInstance(this, macAddress, item.uuid))
            }
            else -> showSnackbarShort(R.string.not_clickable)
        }
    }

    private fun updateUI() {
        connect.isEnabled = !bleDevice.isConnected
    }

    override fun onPause() {
        super.onPause()
        discoveryDisposable.clear()
    }
}
