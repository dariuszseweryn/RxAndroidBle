package com.polidea.rxandroidble2.samplekotlin.util

import android.bluetooth.BluetoothGattCharacteristic

fun BluetoothGattCharacteristic.hasProperty(property: Int): Boolean = (properties and property) > 0