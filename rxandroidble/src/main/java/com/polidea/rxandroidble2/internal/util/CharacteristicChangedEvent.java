package com.polidea.rxandroidble2.internal.util;


import java.util.Arrays;
import java.util.UUID;

public class CharacteristicChangedEvent extends CharacteristicNotificationId {

    public final byte[] data;

    public CharacteristicChangedEvent(UUID uuid, Integer instanceId, byte[] data) {
        super(uuid, instanceId);
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CharacteristicChangedEvent)) {
            return o instanceof CharacteristicNotificationId && super.equals(o);
        }
        if (!super.equals(o)) {
            return false;
        }

        CharacteristicChangedEvent that = (CharacteristicChangedEvent) o;

        return Arrays.equals(data, that.data);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        return "CharacteristicChangedEvent{"
                + "UUID=" + first.toString()
                + ", instanceId=" + second.toString()
                + ", data=" + Arrays.toString(data)
                + '}';
    }
}
