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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StandardUUIDsParser {

    // Service UUIDs
    private static final Map<String, String> SERVICE_UUIDS;

    static {
        Map<String, String> aMap = new HashMap<>();
        // Adopted
        aMap.put("1811", "Alert Notification Service");
        aMap.put("180F", "Battery Service");
        aMap.put("1810", "Blood Pressure");
        aMap.put("181B", "Body Composition");
        aMap.put("181E", "Bond Management");
        aMap.put("181F", "Continuous Glucose Monitoring");
        aMap.put("1805", "Current Time Service");
        aMap.put("1818", "Cycling Power");
        aMap.put("1816", "Cycling Speed and Cadence");
        aMap.put("180A", "Device Information");
        aMap.put("181A", "Environmental Sensing");
        aMap.put("1800", "Generic Access");
        aMap.put("1801", "Generic Attribute");
        aMap.put("1808", "Glucose");
        aMap.put("1809", "Health Thermometer");
        aMap.put("180D", "Heart Rate");
        aMap.put("1812", "Human Interface Device");
        aMap.put("1802", "Immediate Alert");
        aMap.put("1803", "Link Loss");
        aMap.put("1819", "Location and Navigation");
        aMap.put("1820", "Internet Protocol Support");
        aMap.put("1807", "Next DST Change Service");
        aMap.put("180E", "Phone Alert Status Service");
        aMap.put("1806", "Reference Time Update Service");
        aMap.put("1814", "Running Speed and Cadence");
        aMap.put("1813", "Scan Parameters");
        aMap.put("1804", "Tx Power");
        aMap.put("181C", "User Data");
        aMap.put("181D", "Weight Scale");
        // v0.9
        aMap.put("1815", "Automation IO");
        aMap.put("1802", "Immediate Alert Service 1.1");
        // aMap.put("1803", "Link Loss Service 1.1");
        // aMap.put("1804", "Tx Power Service 1.1");
        SERVICE_UUIDS = Collections.unmodifiableMap(aMap);
    }

    // Characteristic UUIDs
    private static final Map<String, String> CHARACTERISTIC_UUIDS;

    static {
        Map<String, String> aMap = new HashMap<>();
        // Adopted
        aMap.put("2A7E", "Aerobic Heart Rate Lower Limit");
        aMap.put("2A84", "Aerobic Heart Rate Upper Limit");
        aMap.put("2A7F", "Aerobic Threshold");
        aMap.put("2A80", "Age");
        aMap.put("2A43", "Alert Category ID");
        aMap.put("2A42", "Alert Category ID Bit Mask");
        aMap.put("2A06", "Alert Level");
        aMap.put("2A44", "Alert Notification Control Point");
        aMap.put("2A3F", "Alert Status");
        aMap.put("2A81", "Anaerobic Heart Rate Lower Limit");
        aMap.put("2A82", "Anaerobic Heart Rate Upper Limit");
        aMap.put("2A83", "Anaerobic Threshold");
        aMap.put("2A73", "Apparent Wind Direction");
        aMap.put("2A72", "Apparent Wind Speed");
        aMap.put("2A01", "Appearance");
        aMap.put("2AA3", "Barometric Pressure Trend");
        aMap.put("2A19", "Battery Level");
        aMap.put("2A49", "Blood Pressure Feature");
        aMap.put("2A35", "Blood Pressure Measurement");
        aMap.put("2A9B", "Body Composition Feature");
        aMap.put("2A9C", "Body Composition Measurement");
        aMap.put("2A38", "Body Sensor Location");
        aMap.put("2AA4", "Bond Management Control Point");
        aMap.put("2AA5", "Bond Management Feature");
        aMap.put("2A22", "Boot Keyboard Input Report");
        aMap.put("2A32", "Boot Keyboard Output Report");
        aMap.put("2A33", "Boot Mouse Input Report");
        aMap.put("2AA6", "Central Address Resolution");
        aMap.put("2AA8", "CGM Feature");
        aMap.put("2AA7", "CGM Measurement");
        aMap.put("2AAB", "CGM Session Run Time");
        aMap.put("2AAA", "CGM Session Start Time");
        aMap.put("2AAC", "CGM Specific Ops Control Point");
        aMap.put("2AA9", "CGM Status");
        aMap.put("2A5C", "CSC Feature");
        aMap.put("2A5B", "CSC Measurement");
        aMap.put("2A2B", "Current Time");
        aMap.put("2A66", "Cycling Power Control Point");
        aMap.put("2A65", "Cycling Power Feature");
        aMap.put("2A63", "Cycling Power Measurement");
        aMap.put("2A64", "Cycling Power Vector");
        aMap.put("2A99", "Database Change Increment");
        aMap.put("2A85", "Date of Birth");
        aMap.put("2A86", "Date of Threshold Assessment ");
        aMap.put("2A08", "Date Time");
        aMap.put("2A0A", "Day Date Time");
        aMap.put("2A09", "Day of Week");
        aMap.put("2A7D", "Descriptor Value Changed");
        aMap.put("2A00", "Device Name");
        aMap.put("2A7B", "Dew Point");
        aMap.put("2A0D", "DST Offset");
        aMap.put("2A6C", "Elevation");
        aMap.put("2A87", "Email Address");
        aMap.put("2A0C", "Exact Time 256");
        aMap.put("2A88", "Fat Burn Heart Rate Lower Limit");
        aMap.put("2A89", "Fat Burn Heart Rate Upper Limit");
        aMap.put("2A26", "Firmware Revision String");
        aMap.put("2A8A", "First Name");
        aMap.put("2A8B", "Five Zone Heart Rate Limits");
        aMap.put("2A8C", "Gender");
        aMap.put("2A51", "Glucose Feature");
        aMap.put("2A18", "Glucose Measurement");
        aMap.put("2A34", "Glucose Measurement Context");
        aMap.put("2A74", "Gust Factor");
        aMap.put("2A27", "Hardware Revision String");
        aMap.put("2A39", "Heart Rate Control Point");
        aMap.put("2A8D", "Heart Rate Max");
        aMap.put("2A37", "Heart Rate Measurement");
        aMap.put("2A7A", "Heat Index");
        aMap.put("2A8E", "Height");
        aMap.put("2A4C", "HID Control Point");
        aMap.put("2A4A", "HID Information");
        aMap.put("2A8F", "Hip Circumference");
        aMap.put("2A6F", "Humidity");
        aMap.put("2A2A", "IEEE 11073-20601 Regulatory Certification Data List");
        aMap.put("2A36", "Intermediate Cuff Pressure");
        aMap.put("2A1E", "Intermediate Temperature");
        aMap.put("2A77", "Irradiance");
        aMap.put("2AA2", "Language");
        aMap.put("2A90", "Last Name");
        aMap.put("2A6B", "LN Control Point");
        aMap.put("2A6A", "LN Feature");
        aMap.put("2A0F", "Local Time Information");
        aMap.put("2A67", "Location and Speed");
        aMap.put("2A2C", "Magnetic Declination");
        aMap.put("2AA0", "Magnetic Flux Density - 2D");
        aMap.put("2AA1", "Magnetic Flux Density - 3D");
        aMap.put("2A29", "Manufacturer Name String");
        aMap.put("2A91", "Maximum Recommended Heart Rate");
        aMap.put("2A21", "Measurement Interval");
        aMap.put("2A24", "Model Number String");
        aMap.put("2A68", "Navigation");
        aMap.put("2A46", "New Alert");
        aMap.put("2A04", "Peripheral Preferred Connection Parameters");
        aMap.put("2A02", "Peripheral Privacy Flag");
        aMap.put("2A50", "PnP ID");
        aMap.put("2A75", "Pollen Concentration");
        aMap.put("2A69", "Position Quality");
        aMap.put("2A6D", "Pressure");
        aMap.put("2A4E", "Protocol Mode");
        aMap.put("2A78", "Rainfall");
        aMap.put("2A03", "Reconnection Address");
        aMap.put("2A52", "Record Access Control Point");
        aMap.put("2A14", "Reference Time Information");
        aMap.put("2A4D", "Report");
        aMap.put("2A4B", "Report Map");
        aMap.put("2A92", "Resting Heart Rate");
        aMap.put("2A40", "Ringer Control Point");
        aMap.put("2A41", "Ringer Setting");
        aMap.put("2A54", "RSC Feature");
        aMap.put("2A53", "RSC Measurement");
        aMap.put("2A55", "SC Control Point");
        aMap.put("2A4F", "Scan Interval Window");
        aMap.put("2A31", "Scan Refresh");
        aMap.put("2A5D", "Sensor Location");
        aMap.put("2A25", "Serial Number String");
        aMap.put("2A05", "Service Changed");
        aMap.put("2A28", "Software Revision String");
        aMap.put("2A93", "Sport Type for Aerobic and Anaerobic Thresholds");
        aMap.put("2A47", "Supported New Alert Category");
        aMap.put("2A48", "Supported Unread Alert Category");
        aMap.put("2A23", "System ID");
        aMap.put("2A6E", "Temperature");
        aMap.put("2A1C", "Temperature Measurement");
        aMap.put("2A1D", "Temperature Type");
        aMap.put("2A94", "Three Zone Heart Rate Limits");
        aMap.put("2A12", "Time Accuracy");
        aMap.put("2A13", "Time Source");
        aMap.put("2A16", "Time Update Control Point");
        aMap.put("2A17", "Time Update State");
        aMap.put("2A11", "Time with DST");
        aMap.put("2A0E", "Time Zone");
        aMap.put("2A71", "True Wind Direction");
        aMap.put("2A70", "True Wind Speed");
        aMap.put("2A95", "Two Zone Heart Rate Limit");
        aMap.put("2A07", "Tx Power Level");
        aMap.put("2A45", "Unread Alert Status");
        aMap.put("2A9F", "User Control Point");
        aMap.put("2A9A", "User Index");
        aMap.put("2A76", "UV Index");
        aMap.put("2A96", "VO2 Max");
        aMap.put("2A97", "Waist Circumference");
        aMap.put("2A98", "Weight");
        aMap.put("2A9D", "Weight Measurement");
        aMap.put("2A9E", "Weight Scale Feature");
        aMap.put("2A79", "Wind Chill");
        // v0.9
        aMap.put("2A5A", "Aggregate");
        //   aMap.put("xxxx", "Altitude");
        aMap.put("2A58", "Analog");
        aMap.put("2A56", "Digital");
        CHARACTERISTIC_UUIDS = Collections.unmodifiableMap(aMap);
    }

    // Descriptors UUIDs
    private static final Map<String, String> DESCRIPTOR_UUIDS;

    static {
        Map<String, String> aMap = new HashMap<>();
        // Adopted
        aMap.put("2900", "Characteristic Extended Properties");
        aMap.put("2901", "Characteristic User Description");
        aMap.put("2902", "Client Characteristic Configuration");
        aMap.put("2903", "Server Characteristic Configuration");
        aMap.put("2904", "Characteristic Presentation Format");
        aMap.put("2905", "Characteristic Aggregate Format");
        aMap.put("2906", "Valid Range");
        aMap.put("2907", "External Report Reference");
        aMap.put("2908", "Report Reference");
        aMap.put("290B", "Environmental Sensing Configuration");
        aMap.put("290C", "Environmental Sensing Measurement");
        aMap.put("290D", "Environmental Sensing Trigger Setting");
        // v0.9
        aMap.put("2909", "Number of Digitals");
        aMap.put("290A", "Value Trigger Setting");
        aMap.put("290E", "Time Trigger Setting");
        DESCRIPTOR_UUIDS = Collections.unmodifiableMap(aMap);
    }

    private StandardUUIDsParser() {
        // utility class
    }

    // Public Getters
    public static String getServiceName(UUID uuid) {
        String result = null;
        String uuid16bit = getStandardizedUUIDComponent(uuid);
        if (uuid16bit != null) {
            result = SERVICE_UUIDS.get(uuid16bit);
        }
        return result;
    }

    public static String getCharacteristicName(UUID uuid) {
        String result = null;
        String uuid16bit = getStandardizedUUIDComponent(uuid);
        if (uuid16bit != null) {
            result = CHARACTERISTIC_UUIDS.get(uuid16bit);
        }
        return result;
    }

    public static String getDescriptorName(UUID uuid) {
        String result = null;
        String uuid16bit = getStandardizedUUIDComponent(uuid);
        if (uuid16bit != null) {
            result = DESCRIPTOR_UUIDS.get(uuid16bit);
        }
        return result;
    }

    private static String getStandardizedUUIDComponent(UUID uuid) {
        String result = null;
        // If is convertible to 16 bits
        String stringUUIDRepresentation = uuid.toString().toUpperCase();
        if (isStandardizedUUID(stringUUIDRepresentation)) {
            // Convert to 16 bit
            result = stringUUIDRepresentation.substring(4, 8);
        }
        return result;
    }

    private static boolean isStandardizedUUID(String stringUUIDRepresentation) {
        return stringUUIDRepresentation.startsWith("0000") && stringUUIDRepresentation.endsWith("-0000-1000-8000-00805F9B34FB");
    }
}
