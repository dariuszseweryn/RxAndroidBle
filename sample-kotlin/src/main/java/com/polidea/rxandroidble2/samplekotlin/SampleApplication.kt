package com.polidea.rxandroidble2.samplekotlin

import android.app.Application
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.internal.RxBleLog

class SampleApplication : Application() {

    companion object {
        lateinit var rxBleClient: RxBleClient
            private set
    }

    override fun onCreate() {
        super.onCreate()
        rxBleClient = RxBleClient.create(this)
        RxBleClient.setLogLevel(RxBleLog.VERBOSE)
    }
}
