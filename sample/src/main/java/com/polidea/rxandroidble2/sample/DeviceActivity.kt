package com.polidea.rxandroidble2.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import butterknife.ButterKnife
import butterknife.OnClick
import com.polidea.rxandroidble2.sample.example2_connection.newConnectionExampleActivity
import com.polidea.rxandroidble2.sample.example3_discovery.newServiceDiscoveryExampleActivity

private const val EXTRA_MAC_ADDRESS = "extra_mac_address"

internal fun Context.newDeviceActivity(macAddress: String): Intent =
    Intent(this, DeviceActivity::class.java).apply {
        putExtra(EXTRA_MAC_ADDRESS, macAddress)
    }

class DeviceActivity : AppCompatActivity() {

    private lateinit var macAddress: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)
        ButterKnife.bind(this)

        macAddress = intent.getStringExtra(EXTRA_MAC_ADDRESS)
        supportActionBar!!.subtitle = getString(R.string.mac_address, macAddress)
    }

    @OnClick(R.id.connect)
    fun onConnectClick() {
        startActivity(newConnectionExampleActivity(macAddress))
    }

    @OnClick(R.id.discovery)
    fun onDiscoveryClick() {
        startActivity(newServiceDiscoveryExampleActivity(macAddress))
    }
}
