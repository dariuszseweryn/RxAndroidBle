package utils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import org.mockito.Mockito;

public class MockUtils {

    public static BluetoothGatt bluetoothGatt(String deviceAddress) {
        BluetoothGatt bluetoothGatt = Mockito.mock(BluetoothGatt.class);
        BluetoothDevice bluetoothDevice = Mockito.mock(BluetoothDevice.class);
        Mockito.when(bluetoothGatt.getDevice()).thenReturn(bluetoothDevice);
        Mockito.when(bluetoothDevice.getAddress()).thenReturn(deviceAddress);
        return bluetoothGatt;
    }
}
