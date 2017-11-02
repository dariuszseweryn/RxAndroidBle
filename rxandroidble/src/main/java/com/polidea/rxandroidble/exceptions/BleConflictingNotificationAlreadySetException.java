package com.polidea.rxandroidble.exceptions;

import java.util.UUID;

public class BleConflictingNotificationAlreadySetException extends BleException {

    private final UUID characteristicUuid;

    private final boolean alreadySetIsIndication;

    public BleConflictingNotificationAlreadySetException(UUID characteristicUuid, boolean alreadySetIsIndication) {
        super("Characteristic " + characteristicUuid
                + " notification already set to " + (alreadySetIsIndication ? "indication" : "notification"));
        this.characteristicUuid = characteristicUuid;
        this.alreadySetIsIndication = alreadySetIsIndication;
    }

    public UUID getCharacteristicUuid() {
        return characteristicUuid;
    }

    public boolean indicationAlreadySet() {
        return alreadySetIsIndication;
    }

    public boolean notificationAlreadySet() {
        return !alreadySetIsIndication;
    }

}
