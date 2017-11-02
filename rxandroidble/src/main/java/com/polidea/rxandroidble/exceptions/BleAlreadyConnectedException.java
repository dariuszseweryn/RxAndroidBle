package com.polidea.rxandroidble.exceptions;

public class BleAlreadyConnectedException extends BleException {

    private final String macAddress;

    public BleAlreadyConnectedException(String macAddress) {
        super("Already connected to device with MAC address " + macAddress);
        this.macAddress = macAddress;
    }

}
