/**
 * Parsing thanks to
 * https://github.com/adafruit/Bluefruit_LE_Connect_Android/blob/
 * master/app/src/main/java/com/adafruit/bluefruit/le/connect/ble/StandardUUIDs.java
 * Bluefruit LE Connect for Android
 * <p>
 * <p>
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2015 Adafruit Industries
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.polidea.rxandroidble3.utils;

import android.annotation.SuppressLint;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GattStatusParser {

    private static final Map<Integer, String> GATT_STATUS;

    static {
        @SuppressLint("UseSparseArrays") Map<Integer, String> aMap = new HashMap<>();
        aMap.put(0x00, "GATT_SUCCESS");
        aMap.put(0x01, "GATT_INVALID_HANDLE");
        aMap.put(0x02, "GATT_READ_NOT_PERMIT");
        aMap.put(0x03, "GATT_WRITE_NOT_PERMIT");
        aMap.put(0x04, "GATT_INVALID_PDU");
        aMap.put(0x05, "GATT_INSUF_AUTHENTICATION");
        aMap.put(0x06, "GATT_REQ_NOT_SUPPORTED");
        aMap.put(0x07, "GATT_INVALID_OFFSET");
        aMap.put(0x08, "GATT_INSUF_AUTHORIZATION or GATT_CONN_TIMEOUT");
        aMap.put(0x09, "GATT_PREPARE_Q_FULL");
        aMap.put(0x0a, "GATT_NOT_FOUND");
        aMap.put(0x0b, "GATT_NOT_LONG");
        aMap.put(0x0c, "GATT_INSUF_KEY_SIZE");
        aMap.put(0x0d, "GATT_INVALID_ATTR_LEN");
        aMap.put(0x0e, "GATT_ERR_UNLIKELY");
        aMap.put(0x0f, "GATT_INSUF_ENCRYPTION");
        aMap.put(0x10, "GATT_UNSUPPORT_GRP_TYPE");
        aMap.put(0x11, "GATT_INSUF_RESOURCE");

        aMap.put(0x13, "GATT_CONN_TERMINATE_PEER_USER");
        aMap.put(0x16, "GATT_CONN_TERMINATE_LOCAL_HOST");
        aMap.put(0x22, "GATT_CONN_LMP_TIMEOUT");
        aMap.put(0x3e, "GATT_CONN_FAIL_ESTABLISH");

        aMap.put(0x87, "GATT_ILLEGAL_PARAMETER");
        aMap.put(0x80, "GATT_NO_RESOURCES");
        aMap.put(0x81, "GATT_INTERNAL_ERROR");
        aMap.put(0x82, "GATT_WRONG_STATE");
        aMap.put(0x83, "GATT_DB_FULL");
        aMap.put(0x84, "GATT_BUSY");
        aMap.put(0x85, "GATT_ERROR");
        aMap.put(0x86, "GATT_CMD_STARTED");
        aMap.put(0x88, "GATT_PENDING");
        aMap.put(0x89, "GATT_AUTH_FAIL");
        aMap.put(0x8a, "GATT_MORE");
        aMap.put(0x8b, "GATT_INVALID_CFG");
        aMap.put(0x8c, "GATT_SERVICE_STARTED");
        aMap.put(0x8d, "GATT_ENCRYPED_NO_MITM");
        aMap.put(0x8e, "GATT_NOT_ENCRYPTED");
        aMap.put(0x8f, "GATT_CONGESTED");

        aMap.put(0xfd, "GATT_CCC_CFG_ERR");
        aMap.put(0xfe, "GATT_PRC_IN_PROGRESS");
        aMap.put(0xff, "GATT_OUT_OF_RANGE");

        aMap.put(0x100, "GATT_CONN_CANCEL");
        GATT_STATUS = Collections.unmodifiableMap(aMap);
    }

    private GattStatusParser() {
        // utility class
    }

    public static String getGattCallbackStatusDescription(int status) {
        final String description = GATT_STATUS.get(status);
        return description == null ? "UNKNOWN" : description;
    }
}
