/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.bluetooth;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothGatt {
    
    
    

    

    
    

    
    

    

    
    
    
    

    

    
    
    
    
    
    
    
    
    
    
    
    

    public static final int GATT_SUCCESS = 0;

    public static final int GATT_READ_NOT_PERMITTED = 0x2;

    public static final int GATT_WRITE_NOT_PERMITTED = 0x3;

    public static final int GATT_INSUFFICIENT_AUTHENTICATION = 0x5;

    public static final int GATT_REQUEST_NOT_SUPPORTED = 0x6;

    public static final int GATT_INSUFFICIENT_ENCRYPTION = 0xf;

    public static final int GATT_INVALID_OFFSET = 0x7;

    public static final int GATT_INVALID_ATTRIBUTE_LENGTH = 0xd;

    public static final int GATT_CONNECTION_CONGESTED = 0x8f;

    public static final int GATT_FAILURE = 0x101;

    public static final int CONNECTION_PRIORITY_BALANCED = 0;

    public static final int CONNECTION_PRIORITY_HIGH = 1;

    public static final int CONNECTION_PRIORITY_LOW_POWER = 2;
    
    
    

    public void close() {
    }
    
    
    

    public void disconnect() {
    }

    public boolean connect() {
        return false;

    }

    public void setPreferredPhy(int txPhy, int rxPhy, int phyOptions) {
    }

    public void readPhy() {
    }

    public BluetoothDevice getDevice() {
        return null;
    }

    public boolean discoverServices() {
        return true;
    }
    

    public List<BluetoothGattService> getServices() {
        List<BluetoothGattService> result =
                new ArrayList<BluetoothGattService>();
        return result;
    }

    public BluetoothGattService getService(UUID uuid) {
        return null;
    }

    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        return true;
    }
    

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        return true;
    }

    public boolean readDescriptor(BluetoothGattDescriptor descriptor) {
        return true;
    }

    public boolean writeDescriptor(BluetoothGattDescriptor descriptor) {
        return true;
    }

    public boolean beginReliableWrite() {
        return true;
    }

    public boolean executeReliableWrite() {
        return true;
    }

    public void abortReliableWrite() {
    }

    @Deprecated
    public void abortReliableWrite(BluetoothDevice mDevice) {
        abortReliableWrite();
    }

    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                                 boolean enable) {
        return true;
    }
    

    public boolean readRemoteRssi() {
        return true;
    }

    public boolean requestMtu(int mtu) {
        return true;
    }

    public boolean requestConnectionPriority(int connectionPriority) {
        return true;
    }
}
