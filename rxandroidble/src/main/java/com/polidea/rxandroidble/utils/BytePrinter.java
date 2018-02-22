package com.polidea.rxandroidble.utils;

public class BytePrinter {

    private static final String HEX_PREFIX = "0x";
    private static char[] hexCharacters = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private BytePrinter() {
        // Util class
    }

    public static String toPrettyFormattedHexString(byte[] byteArray) {

        if (byteArray == null) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();
        boolean isFirst = true;

        for (byte singleByte : byteArray) {

            if (!isFirst) {
                stringBuilder.append(", ");
            }

            char secondChar = hexCharacters[(int) singleByte & 0x0F];
            char firstChar = hexCharacters[(int) singleByte >> 4 & 0x0F];
            stringBuilder.append(HEX_PREFIX).append(firstChar).append(secondChar);
            isFirst = false;
        }

        return stringBuilder.toString();
    }
}
