package com.polidea.rxandroidble.exceptions;

public class BleAlreadyConnectedException extends BleException {

    private final String macAddress;

    public BleAlreadyConnectedException(String macAddress) {
        this.macAddress = macAddress;
    }

    @Override
    public String toString() {
        return "BleAlreadyConnectedException{macAddress=" + macAddress + '}';
    }
}
