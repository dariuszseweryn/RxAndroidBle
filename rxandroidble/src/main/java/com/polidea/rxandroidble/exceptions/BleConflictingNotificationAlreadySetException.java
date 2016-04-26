package com.polidea.rxandroidble.exceptions;

import java.util.UUID;

public class BleConflictingNotificationAlreadySetException extends BleException {

    private final UUID characteristicUuid;

    private final boolean alreadySetIsIndication;

    public BleConflictingNotificationAlreadySetException(UUID characteristicUuid, boolean alreadySetIsIndication) {
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

    @Override
    public String toString() {
        return "BleCharacteristicNotificationOfOtherTypeAlreadySetException{" +
                "characteristicUuid=" + characteristicUuid.toString() +
                ", typeAlreadySet=" + (alreadySetIsIndication ? "indication" : "notification") +
                '}';
    }
}
