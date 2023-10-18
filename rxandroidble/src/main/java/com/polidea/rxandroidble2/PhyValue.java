package com.polidea.rxandroidble2;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@RequiresApi(26 /* Build.VERSION_CODES.O */)
@Retention(RetentionPolicy.SOURCE)
@IntDef(value = {
        BluetoothDevice.PHY_LE_1M_MASK,
        BluetoothDevice.PHY_LE_2M_MASK,
        BluetoothDevice.PHY_LE_CODED_MASK
})
public @interface PhyValue { }
