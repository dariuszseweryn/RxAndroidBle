package com.polidea.rxandroidble3.exceptions;


/**
 * An exception being emitted from an {@link io.reactivex.rxjava3.core.Observable} returned by the function
 * {@link com.polidea.rxandroidble3.RxBleDevice#establishConnection(boolean)} or other establishConnection() overloads when this kind
 * of observable was already subscribed and {@link com.polidea.rxandroidble3.RxBleConnection} is currently being established or active.
 *
 * <p>
 *     To prevent this exception from being emitted one must either:<br>
 *     * always unsubscribe from the above mentioned Observable before subscribing again<br>
 *     * {@link io.reactivex.rxjava3.core.Observable#share()} or {@link io.reactivex.rxjava3.core.Observable#publish()} the above mentioned
 *     Observable so it will be subscribed only once
 * </p>
 */
public class BleAlreadyConnectedException extends BleException {

    public BleAlreadyConnectedException(String macAddress) {
        super("Already connected to device with MAC address " + macAddress);
    }
}
