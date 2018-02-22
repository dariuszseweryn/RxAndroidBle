package com.polidea.rxandroidble.sample;

import android.app.Application;
import android.content.Context;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.eventlog.StethoOperationsLogger;
import com.polidea.rxandroidble.internal.RxBleLog;

public class SampleApplication extends Application {

    private RxBleClient rxBleClient;
    public static final StethoOperationsLogger STETHO_OPERATIONS_LOGGER = new StethoOperationsLogger();

    /**
     * In practise you will use some kind of dependency injection pattern.
     */
    public static RxBleClient getRxBleClient(Context context) {
        SampleApplication application = (SampleApplication) context.getApplicationContext();
        return application.rxBleClient;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        rxBleClient = RxBleClient.create(this);

        /*
         * In order to use a Stetho based operation visualization, provide a logger instance to the Client Module and initialize Stetho.
         *
         * rxBleClient = DaggerClientComponent
         *       .builder()
         *       .clientModule(new ClientComponent.ClientModule(this, STETHO_OPERATIONS_LOGGER))
         *       .build()
         *       .rxBleClient();
         *
         * Stetho.initializeWithDefaults(this);
         */
        RxBleClient.setLogLevel(RxBleLog.VERBOSE);
    }
}
