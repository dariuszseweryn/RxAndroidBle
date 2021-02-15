package com.polidea.rxandroidble3.sample;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.polidea.rxandroidble3.sample.example2_connection.ConnectionExampleActivity;
import com.polidea.rxandroidble3.sample.example3_discovery.ServiceDiscoveryExampleActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class DeviceActivity extends AppCompatActivity {

    public static final String EXTRA_MAC_ADDRESS = "extra_mac_address";
    private String macAddress;

    @OnClick(R.id.connect)
    public void onConnectClick() {
        final Intent intent = new Intent(this, ConnectionExampleActivity.class);
        intent.putExtra(EXTRA_MAC_ADDRESS, macAddress);
        startActivity(intent);
    }

    @OnClick(R.id.discovery)
    public void onDiscoveryClick() {
        final Intent intent = new Intent(this, ServiceDiscoveryExampleActivity.class);
        intent.putExtra(EXTRA_MAC_ADDRESS, macAddress);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        ButterKnife.bind(this);
        macAddress = getIntent().getStringExtra(EXTRA_MAC_ADDRESS);
        //noinspection ConstantConditions
        getSupportActionBar().setSubtitle(getString(R.string.mac_address, macAddress));
    }
}
