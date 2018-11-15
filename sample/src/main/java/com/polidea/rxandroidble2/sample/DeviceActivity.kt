package com.polidea.rxandroidble2.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import butterknife.ButterKnife
import butterknife.OnClick
import com.polidea.rxandroidble2.sample.example2_connection.ConnectionExampleActivity
import com.polidea.rxandroidble2.sample.example3_discovery.ServiceDiscoveryExampleActivity

internal fun Context.newDeviceActivity(): Intent = Intent(this, DeviceActivity::class.java)

class DeviceActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MAC_ADDRESS = "extra_mac_address"
    }

    private var macAddress: String? = null

    @OnClick(R.id.connect)
    fun onConnectClick() {
        val intent = Intent(this, ConnectionExampleActivity::class.java)
        intent.putExtra(EXTRA_MAC_ADDRESS, macAddress)
        startActivity(intent)
    }

    @OnClick(R.id.discovery)
    fun onDiscoveryClick() {
        val intent = Intent(this, ServiceDiscoveryExampleActivity::class.java)
        intent.putExtra(EXTRA_MAC_ADDRESS, macAddress)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)
        ButterKnife.bind(this)
        macAddress = intent.getStringExtra(EXTRA_MAC_ADDRESS)

        supportActionBar!!.subtitle = getString(R.string.mac_address, macAddress)
    }
}
