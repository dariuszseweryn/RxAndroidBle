package com.polidea.rxandroidble.exceptions;

import java.util.UUID;

public class BleCharacteristicNotFoundException extends BleException {

    private final UUID charactersisticUUID;

    public BleCharacteristicNotFoundException(UUID charactersisticUUID) {
        super("Characteristic not found with UUID " + charactersisticUUID);
        this.charactersisticUUID = charactersisticUUID;
    }

    public UUID getCharactersisticUUID() {
        return charactersisticUUID;
    }

}
