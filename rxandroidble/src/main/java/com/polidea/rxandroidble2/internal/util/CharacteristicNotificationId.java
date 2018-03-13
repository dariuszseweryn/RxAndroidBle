package com.polidea.rxandroidble2.internal.util;


import android.util.Pair;
import java.util.UUID;

public class CharacteristicNotificationId extends Pair<UUID, Integer> {

    public CharacteristicNotificationId(UUID uuid, Integer instanceId) {
        super(uuid, instanceId);
    }

    @Override
    public String toString() {
        return "CharacteristicNotificationId{"
                + "UUID=" + first.toString()
                + ", instanceId=" + second.toString()
                + '}';
    }
}
