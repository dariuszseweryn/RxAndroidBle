package com.polidea.rxandroidble2.helpers;


import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import com.polidea.rxandroidble2.internal.RxBleLog;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

/**
 * A helper class intended for interpreting Integers, Floats and Strings that may be stored in byte arrays retrieved by
 * {@link com.polidea.rxandroidble2.RxBleConnection#readCharacteristic(UUID)} or
 * {@link com.polidea.rxandroidble2.RxBleConnection#setupIndication(UUID)}. If the data in emitted byte arrays is stored using
 * a standardised Bluetooth Specification format
 *
 * This class is a copied from {@link android.bluetooth.BluetoothGattCharacteristic#getIntValue(int, int)},
 * {@link android.bluetooth.BluetoothGattCharacteristic#getFloatValue(int, int)} and
 * {@link android.bluetooth.BluetoothGattCharacteristic#getStringValue(int)}
 */
public class ValueInterpreter {

    @IntDef({FORMAT_UINT8, FORMAT_UINT16, FORMAT_UINT32, FORMAT_SINT8, FORMAT_SINT16, FORMAT_SINT32})
    @Retention(RetentionPolicy.SOURCE)
    public @interface IntFormatType {

    }

    @IntDef({FORMAT_SFLOAT, FORMAT_FLOAT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FloatFormatType {

    }

    /**
     * Characteristic value format type uint8
     */
    public static final int FORMAT_UINT8 = 0x11;

    /**
     * Characteristic value format type uint16
     */
    public static final int FORMAT_UINT16 = 0x12;

    /**
     * Characteristic value format type uint32
     */
    public static final int FORMAT_UINT32 = 0x14;

    /**
     * Characteristic value format type sint8
     */
    public static final int FORMAT_SINT8 = 0x21;

    /**
     * Characteristic value format type sint16
     */
    public static final int FORMAT_SINT16 = 0x22;

    /**
     * Characteristic value format type sint32
     */
    public static final int FORMAT_SINT32 = 0x24;

    /**
     * Characteristic value format type sfloat (16-bit float)
     */
    public static final int FORMAT_SFLOAT = 0x32;

    /**
     * Characteristic value format type float (32-bit float)
     */
    public static final int FORMAT_FLOAT = 0x34;

    private ValueInterpreter() {

    }

    /**
     * Return the integer value interpreted from the passed byte array.
     *
     * <p>The formatType parameter determines how the value
     * is to be interpreted. For example, setting formatType to
     * {@link #FORMAT_UINT16} specifies that the first two bytes of the
     * characteristic value at the given offset are interpreted to generate the
     * return value.
     *
     * @param value The byte array from which to interpret value.
     * @param formatType The format type used to interpret the value.
     * @param offset Offset at which the integer value can be found.
     * @return The value at a given offset or null if offset exceeds value size.
     */
    public static Integer getIntValue(@NonNull byte[] value, @IntFormatType int formatType, @IntRange(from = 0) int offset) {
        if ((offset + getTypeLen(formatType)) > value.length) {
            RxBleLog.w(
                    "Int formatType (0x%x) is longer than remaining bytes (%d) - returning null", formatType, value.length - offset
            );
            return null;
        }

        switch (formatType) {
            case FORMAT_UINT8:
                return unsignedByteToInt(value[offset]);

            case FORMAT_UINT16:
                return unsignedBytesToInt(value[offset], value[offset + 1]);

            case FORMAT_UINT32:
                return unsignedBytesToInt(value[offset],   value[offset + 1],
                        value[offset + 2], value[offset + 3]);
            case FORMAT_SINT8:
                return unsignedToSigned(unsignedByteToInt(value[offset]), 8);

            case FORMAT_SINT16:
                return unsignedToSigned(unsignedBytesToInt(value[offset],
                        value[offset + 1]), 16);

            case FORMAT_SINT32:
                return unsignedToSigned(unsignedBytesToInt(value[offset],
                        value[offset + 1], value[offset + 2], value[offset + 3]), 32);
            default:
                RxBleLog.w("Passed an invalid integer formatType (0x%x) - returning null", formatType);
                return null;
        }
    }

    /**
     * Return the float value interpreted from the passed byte array.
     *
     * @param value The byte array from which to interpret value.
     * @param formatType The format type used to interpret the value.
     * @param offset Offset at which the float value can be found.
     * @return The value at a given offset or null if the requested offset exceeds the value size.
     */
    public static Float getFloatValue(@NonNull byte[] value, @FloatFormatType int formatType, @IntRange(from = 0) int offset) {
        if ((offset + getTypeLen(formatType)) > value.length) {
            RxBleLog.w(
                    "Float formatType (0x%x) is longer than remaining bytes (%d) - returning null", formatType, value.length - offset
            );
            return null;
        }

        switch (formatType) {
            case FORMAT_SFLOAT:
                return bytesToFloat(value[offset], value[offset + 1]);

            case FORMAT_FLOAT:
                return bytesToFloat(value[offset],   value[offset + 1],
                        value[offset + 2], value[offset + 3]);
            default:
                RxBleLog.w("Passed an invalid float formatType (0x%x) - returning null", formatType);
                return null;
        }
    }

    /**
     * Return the string value interpreted from the passed byte array.
     *
     * @param offset Offset at which the string value can be found.
     * @return The value at a given offset
     */
    public static String getStringValue(@NonNull byte[] value, @IntRange(from = 0) int offset) {
        if (offset > value.length) {
            RxBleLog.w("Passed offset that exceeds the length of the byte array - returning null");
            return null;
        }
        byte[] strBytes = new byte[value.length - offset];
        for (int i = 0; i != (value.length - offset); ++i) {
            strBytes[i] = value[offset + i];
        }
        return new String(strBytes);
    }

    /**
     * Returns the size of a give value type.
     */
    private static int getTypeLen(int formatType) {
        return formatType & 0xF;
    }

    /**
     * Convert a signed byte to an unsigned int.
     */
    private static int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }

    /**
     * Convert signed bytes to a 16-bit unsigned int.
     */
    private static int unsignedBytesToInt(byte b0, byte b1) {
        return (unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8));
    }

    /**
     * Convert signed bytes to a 32-bit unsigned int.
     */
    private static int unsignedBytesToInt(byte b0, byte b1, byte b2, byte b3) {
        return (unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8))
                + (unsignedByteToInt(b2) << 16) + (unsignedByteToInt(b3) << 24);
    }

    /**
     * Convert signed bytes to a 16-bit short float value.
     */
    private static float bytesToFloat(byte b0, byte b1) {
        int mantissa = unsignedToSigned(unsignedByteToInt(b0)
                + ((unsignedByteToInt(b1) & 0x0F) << 8), 12);
        int exponent = unsignedToSigned(unsignedByteToInt(b1) >> 4, 4);
        return (float) (mantissa * Math.pow(10, exponent));
    }

    /**
     * Convert signed bytes to a 32-bit short float value.
     */
    private static float bytesToFloat(byte b0, byte b1, byte b2, byte b3) {
        int mantissa = unsignedToSigned(unsignedByteToInt(b0)
                + (unsignedByteToInt(b1) << 8)
                + (unsignedByteToInt(b2) << 16), 24);
        return (float) (mantissa * Math.pow(10, b3));
    }

    /**
     * Convert an unsigned integer value to a two's-complement encoded
     * signed value.
     */
    private static int unsignedToSigned(int unsigned, int size) {
        if ((unsigned & (1 << size - 1)) != 0) {
            unsigned = -1 * ((1 << size - 1) - (unsigned & ((1 << size - 1) - 1)));
        }
        return unsigned;
    }
}
