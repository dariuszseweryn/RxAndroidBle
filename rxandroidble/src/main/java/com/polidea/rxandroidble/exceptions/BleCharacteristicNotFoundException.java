package com.polidea.rxandroidble.exceptions;

import java.util.UUID;

public class BleCharacteristicNotFoundException extends BleException {

    private final UUID charactersisticUUID;

    public BleCharacteristicNotFoundException(UUID charactersisticUUID) {
        this.charactersisticUUID = charactersisticUUID;
    }

    public UUID getCharactersisticUUID() {
        return charactersisticUUID;
    }

    @Override
    public String toString() {
        return "BleCharacteristicNotFoundException{" + "charactersisticUUID=" + charactersisticUUID + '}';
    }
}
