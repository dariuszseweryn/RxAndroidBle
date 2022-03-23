package android.bluetooth;
import android.os.Handler;
import java.util.List;
import java.util.UUID;
public class BluetoothGatt implements BluetoothProfile {
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
    static final int AUTHENTICATION_NONE = 0;
    static final int AUTHENTICATION_NO_MITM = 1;
    static final int AUTHENTICATION_MITM = 2;
    public void close() {
    }
    BluetoothGattService getService(BluetoothDevice device, UUID uuid,
            int instanceId) {
        return null;
    }
    BluetoothGattCharacteristic getCharacteristicById(BluetoothDevice device,
            int instanceId) {
        return null;
    }
    BluetoothGattDescriptor getDescriptorById(BluetoothDevice device, int instanceId) {
        return null;
    }

    boolean connect(Boolean autoConnect, BluetoothGattCallback callback,
            Handler handler) {
        return true;
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
    public boolean discoverServiceByUuid(UUID uuid) {
        return true;
    }
    public List<BluetoothGattService> getServices() {
        return null;
    }
    public BluetoothGattService getService(UUID uuid) {
        return null;
    }
    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        return true;
    }
    public boolean readUsingCharacteristicUuid(UUID uuid, int startHandle, int endHandle) {
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
    @Override
    public int getConnectionState(BluetoothDevice device) {
        throw new UnsupportedOperationException("Use BluetoothManager#getConnectionState instead.");
    }
    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        throw new UnsupportedOperationException(
                "Use BluetoothManager#getConnectedDevices instead.");
    }
    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        throw new UnsupportedOperationException(
                "Use BluetoothManager#getDevicesMatchingConnectionStates instead.");
    }
}
