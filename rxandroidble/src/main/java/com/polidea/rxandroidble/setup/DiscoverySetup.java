package com.polidea.rxandroidble.setup;


import android.support.annotation.NonNull;
import com.polidea.rxandroidble.TimePeriod;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class DiscoverySetup {

//    TODO: [DS] 30.06.2017 Consider a switch for https://github.com/Polidea/RxAndroidBle/issues/53 BluetoothGatt.refresh()
//    public final boolean forceDiscoveryWithoutCache;

    public final TimePeriod timeoutTimePeriod;

    private DiscoverySetup(@NonNull TimePeriod timeoutTimePeriod) {
        this.timeoutTimePeriod = timeoutTimePeriod;
    }

    public static class Builder {

        private TimePeriod timeoutTimePeriod = new TimePeriod(20L, TimeUnit.SECONDS);

        public Builder() {
        }

        public void setTimeoutTimePeriod(TimePeriod timeoutTimePeriod) {
            this.timeoutTimePeriod = timeoutTimePeriod;
        }

        public DiscoverySetup build() {
            return new DiscoverySetup(timeoutTimePeriod);
        }
    }
}
