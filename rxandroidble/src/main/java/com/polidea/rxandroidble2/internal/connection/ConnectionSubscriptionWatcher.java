package com.polidea.rxandroidble2.internal.connection;


/**
 * Interface for all classes that should be called when the user subscribes/unsubscribes to
 * {@link com.polidea.rxandroidble2.RxBleDevice#establishConnection(boolean)}
 *
 * The binding which injects the interface to a {@link ConnectorImpl} is in {@link ConnectionModuleBinder}
 */
public interface ConnectionSubscriptionWatcher {

    /**
     * Method to be called when the user subscribes to an individual
     * {@link com.polidea.rxandroidble2.RxBleDevice#establishConnection(boolean)}
     */
    void onConnectionSubscribed();

    /**
     * Method to be called when the user unsubscribes to an individual
     * {@link com.polidea.rxandroidble2.RxBleDevice#establishConnection(boolean)}
     */
    void onConnectionUnsubscribed();
}
