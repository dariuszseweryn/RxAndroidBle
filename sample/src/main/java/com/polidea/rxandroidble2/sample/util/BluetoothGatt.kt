package com.polidea.rxandroidble2.sample.util

import android.bluetooth.BluetoothGattCharacteristic

internal fun BluetoothGattCharacteristic?.hasProperty(property: Int): Boolean =
    this?.run { properties and property > 0 } ?: false