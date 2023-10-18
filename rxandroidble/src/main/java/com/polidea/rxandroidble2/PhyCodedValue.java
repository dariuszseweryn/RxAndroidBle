package com.polidea.rxandroidble2;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@RequiresApi(26 /* Build.VERSION_CODES.O */)
@Retention(RetentionPolicy.SOURCE)
@IntDef(value = {
        BluetoothDevice.PHY_OPTION_NO_PREFERRED,
        BluetoothDevice.PHY_OPTION_S2,
        BluetoothDevice.PHY_OPTION_S8
})
public @interface PhyCodedValue { }