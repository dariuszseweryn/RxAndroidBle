package android.bluetooth;
import android.bluetooth.le.BluetoothLeScanner;
import android.os.RemoteException;
import android.util.Log;
import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Used for mocks and constants
 */
public class BluetoothAdapter {
    private static final String TAG = "BluetoothAdapter";
    private static final boolean VDBG = false;
    public static final String
            ACTION_STATE_CHANGED = "android.bluetooth.adapter.action.STATE_CHANGED";
    public static final String EXTRA_STATE = "android.bluetooth.adapter.extra.STATE";
    @IntDef(value = {
            STATE_OFF,
            STATE_TURNING_ON,
            STATE_ON,
            STATE_TURNING_OFF,
            STATE_BLE_TURNING_ON,
            STATE_BLE_ON,
            STATE_BLE_TURNING_OFF
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AdapterState {}
    public static final int STATE_OFF = 10;
    public static final int STATE_TURNING_ON = 11;
    public static final int STATE_ON = 12;
    public static final int STATE_TURNING_OFF = 13;
    public static final int STATE_BLE_TURNING_ON = 14;
    public static final int STATE_BLE_ON = 15;
    public static final int STATE_BLE_TURNING_OFF = 16;
    public static final UUID LE_PSM_CHARACTERISTIC_UUID =
            UUID.fromString("2d410339-82b6-42aa-b34e-e2e01df8cc1a");
    public static String nameForState(@AdapterState int state) {
        return null;
    }
    @IntDef(value = {
            SCAN_MODE_NONE,
            SCAN_MODE_CONNECTABLE,
            SCAN_MODE_CONNECTABLE_DISCOVERABLE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScanMode {}
    public static final int SCAN_MODE_NONE = 20;
    public static final int SCAN_MODE_CONNECTABLE = 21;
    public static final int SCAN_MODE_CONNECTABLE_DISCOVERABLE = 23;
    public static final int IO_CAPABILITY_OUT = 0;
    public static final int IO_CAPABILITY_IO = 1;
    public static final int IO_CAPABILITY_IN = 2;
    public static final int IO_CAPABILITY_NONE = 3;
    public static final int IO_CAPABILITY_KBDISP = 4;
    public static final int IO_CAPABILITY_MAX = 5;
    public static final int IO_CAPABILITY_UNKNOWN = 255;
    @IntDef({IO_CAPABILITY_OUT, IO_CAPABILITY_IO, IO_CAPABILITY_IN, IO_CAPABILITY_NONE,
            IO_CAPABILITY_KBDISP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface IoCapability {}
    @IntDef(value = {ACTIVE_DEVICE_AUDIO,
            ACTIVE_DEVICE_PHONE_CALL, ACTIVE_DEVICE_ALL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActiveDeviceUse {}
    public static final int ACTIVE_DEVICE_AUDIO = 0;
    public static final int ACTIVE_DEVICE_PHONE_CALL = 1;
    public static final int ACTIVE_DEVICE_ALL = 2;
    public BluetoothDevice getRemoteDevice(String address) {
        return null;
    }
    public BluetoothLeScanner getBluetoothLeScanner() {
        return null;
    }
    public boolean isEnabled() {
        return false;
    }
    @AdapterState
    private int getStateInternal() {
        return 0;
    }
    public int getState() {
        return 0;
    }
    public int getLeState() {
        return 0;
    }
    public String getName() {
        return null;
    }
    public interface LeScanCallback {
        void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord);
    }

    public static synchronized BluetoothAdapter getDefaultAdapter() {
        return null;
    }

    public boolean startLeScan(LeScanCallback callback) {
        return false;
    }

    public void stopLeScan(LeScanCallback callback) {
    }
}
