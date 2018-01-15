package com.polidea.rxandroidble.internal.connection;

import com.polidea.rxandroidble.RxBleConnection;

public class NoRetryStrategy implements RxBleConnection.WriteOperationRetryStrategy {

    @Override
    public Boolean call(Integer integer, Throwable throwable) {
        return false;
    }
}
