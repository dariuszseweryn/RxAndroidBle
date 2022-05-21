package android.bluetooth;
import android.annotation.SuppressLint;
import android.content.AttributionSource;
import android.content.Context;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Used for mocks and constants
 */
public class BluetoothDevice implements Parcelable {
    private static final String TAG = "BluetoothDevice";
    private static final boolean DBG = false;
    private static final int CONNECTION_STATE_DISCONNECTED = 0;
    private static final int CONNECTION_STATE_CONNECTED = 1;
    private static final int CONNECTION_STATE_ENCRYPTED_BREDR = 2;
    private static final int CONNECTION_STATE_ENCRYPTED_LE = 4;
    public static final int ERROR = Integer.MIN_VALUE;
    public static final String ACTION_FOUND =
            "android.bluetooth.device.action.FOUND";
    public static final String ACTION_CLASS_CHANGED =
            "android.bluetooth.device.action.CLASS_CHANGED";
    public static final String ACTION_ACL_CONNECTED =
            "android.bluetooth.device.action.ACL_CONNECTED";
    public static final String ACTION_ACL_DISCONNECT_REQUESTED =
            "android.bluetooth.device.action.ACL_DISCONNECT_REQUESTED";
    public static final String ACTION_ACL_DISCONNECTED =
            "android.bluetooth.device.action.ACL_DISCONNECTED";
    public static final String ACTION_NAME_CHANGED =
            "android.bluetooth.device.action.NAME_CHANGED";
    @SuppressLint("ActionValue")
    public static final String ACTION_ALIAS_CHANGED =
            "android.bluetooth.device.action.ALIAS_CHANGED";
    // Note: When EXTRA_BOND_STATE is BOND_NONE then this will also
    // contain a hidden extra field EXTRA_REASON with the result code.
    public static final String ACTION_BOND_STATE_CHANGED =
            "android.bluetooth.device.action.BOND_STATE_CHANGED";
    public static final String ACTION_BATTERY_LEVEL_CHANGED =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED";
    public static final String EXTRA_BATTERY_LEVEL =
            "android.bluetooth.device.extra.BATTERY_LEVEL";
    public static final int BATTERY_LEVEL_UNKNOWN = -1;
    public static final int BATTERY_LEVEL_BLUETOOTH_OFF = -100;
    public static final String EXTRA_DEVICE = "android.bluetooth.device.extra.DEVICE";
    public static final String EXTRA_NAME = "android.bluetooth.device.extra.NAME";
    public static final String EXTRA_RSSI = "android.bluetooth.device.extra.RSSI";
    public static final String EXTRA_CLASS = "android.bluetooth.device.extra.CLASS";
    public static final String EXTRA_BOND_STATE = "android.bluetooth.device.extra.BOND_STATE";
    public static final String EXTRA_PREVIOUS_BOND_STATE =
            "android.bluetooth.device.extra.PREVIOUS_BOND_STATE";
    public static final int BOND_NONE = 10;
    public static final int BOND_BONDING = 11;
    public static final int BOND_BONDED = 12;
    public static final String EXTRA_REASON = "android.bluetooth.device.extra.REASON";
    public static final String EXTRA_PAIRING_VARIANT =
            "android.bluetooth.device.extra.PAIRING_VARIANT";
    public static final String EXTRA_PAIRING_KEY = "android.bluetooth.device.extra.PAIRING_KEY";
    public static final String EXTRA_PAIRING_INITIATOR =
            "android.bluetooth.device.extra.PAIRING_INITIATOR";
    public static final int EXTRA_PAIRING_INITIATOR_FOREGROUND = 1;
    public static final int EXTRA_PAIRING_INITIATOR_BACKGROUND = 2;
    public static final int DEVICE_TYPE_UNKNOWN = 0;
    public static final int DEVICE_TYPE_CLASSIC = 1;
    public static final int DEVICE_TYPE_LE = 2;
    public static final int DEVICE_TYPE_DUAL = 3;
    public static final String ACTION_SDP_RECORD =
            "android.bluetooth.device.action.SDP_RECORD";
    @IntDef(value = {
            METADATA_MANUFACTURER_NAME,
            METADATA_MODEL_NAME,
            METADATA_SOFTWARE_VERSION,
            METADATA_HARDWARE_VERSION,
            METADATA_COMPANION_APP,
            METADATA_MAIN_ICON,
            METADATA_IS_UNTETHERED_HEADSET,
            METADATA_UNTETHERED_LEFT_ICON,
            METADATA_UNTETHERED_RIGHT_ICON,
            METADATA_UNTETHERED_CASE_ICON,
            METADATA_UNTETHERED_LEFT_BATTERY,
            METADATA_UNTETHERED_RIGHT_BATTERY,
            METADATA_UNTETHERED_CASE_BATTERY,
            METADATA_UNTETHERED_LEFT_CHARGING,
            METADATA_UNTETHERED_RIGHT_CHARGING,
            METADATA_UNTETHERED_CASE_CHARGING,
            METADATA_ENHANCED_SETTINGS_UI_URI,
            METADATA_DEVICE_TYPE,
            METADATA_MAIN_BATTERY,
            METADATA_MAIN_CHARGING,
            METADATA_MAIN_LOW_BATTERY_THRESHOLD,
            METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD,
            METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD,
            METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MetadataKey{}
    public static final int METADATA_MAX_LENGTH = 2048;
    public static final int METADATA_MANUFACTURER_NAME = 0;
    public static final int METADATA_MODEL_NAME = 1;
    public static final int METADATA_SOFTWARE_VERSION = 2;
    public static final int METADATA_HARDWARE_VERSION = 3;
    public static final int METADATA_COMPANION_APP = 4;
    public static final int METADATA_MAIN_ICON = 5;
    public static final int METADATA_IS_UNTETHERED_HEADSET = 6;
    public static final int METADATA_UNTETHERED_LEFT_ICON = 7;
    public static final int METADATA_UNTETHERED_RIGHT_ICON = 8;
    public static final int METADATA_UNTETHERED_CASE_ICON = 9;
    public static final int METADATA_UNTETHERED_LEFT_BATTERY = 10;
    public static final int METADATA_UNTETHERED_RIGHT_BATTERY = 11;
    public static final int METADATA_UNTETHERED_CASE_BATTERY = 12;
    public static final int METADATA_UNTETHERED_LEFT_CHARGING = 13;
    public static final int METADATA_UNTETHERED_RIGHT_CHARGING = 14;
    public static final int METADATA_UNTETHERED_CASE_CHARGING = 15;
    public static final int METADATA_ENHANCED_SETTINGS_UI_URI = 16;
    public static final int METADATA_DEVICE_TYPE = 17;
    public static final int METADATA_MAIN_BATTERY = 18;
    public static final int METADATA_MAIN_CHARGING = 19;
    public static final int METADATA_MAIN_LOW_BATTERY_THRESHOLD = 20;
    public static final int METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD = 21;
    public static final int METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD = 22;
    public static final int METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD = 23;
    public static final String DEVICE_TYPE_DEFAULT = "Default";
    public static final String DEVICE_TYPE_WATCH = "Watch";
    public static final String DEVICE_TYPE_UNTETHERED_HEADSET = "Untethered Headset";
    public static final String ACTION_UUID =
            "android.bluetooth.device.action.UUID";
    public static final String ACTION_MAS_INSTANCE =
            "android.bluetooth.device.action.MAS_INSTANCE";
    public static final String ACTION_NAME_FAILED =
            "android.bluetooth.device.action.NAME_FAILED";
    public static final String ACTION_PAIRING_REQUEST =
            "android.bluetooth.device.action.PAIRING_REQUEST";
    public static final String ACTION_PAIRING_CANCEL =
            "android.bluetooth.device.action.PAIRING_CANCEL";
    public static final String ACTION_CONNECTION_ACCESS_REQUEST =
            "android.bluetooth.device.action.CONNECTION_ACCESS_REQUEST";
    public static final String ACTION_CONNECTION_ACCESS_REPLY =
            "android.bluetooth.device.action.CONNECTION_ACCESS_REPLY";
    public static final String ACTION_CONNECTION_ACCESS_CANCEL =
            "android.bluetooth.device.action.CONNECTION_ACCESS_CANCEL";
    public static final String ACTION_SILENCE_MODE_CHANGED =
            "android.bluetooth.device.action.SILENCE_MODE_CHANGED";
    public static final String EXTRA_ACCESS_REQUEST_TYPE =
            "android.bluetooth.device.extra.ACCESS_REQUEST_TYPE";
    public static final int REQUEST_TYPE_PROFILE_CONNECTION = 1;
    public static final int REQUEST_TYPE_PHONEBOOK_ACCESS = 2;
    public static final int REQUEST_TYPE_MESSAGE_ACCESS = 3;
    public static final int REQUEST_TYPE_SIM_ACCESS = 4;
    public static final String EXTRA_PACKAGE_NAME = "android.bluetooth.device.extra.PACKAGE_NAME";
    public static final String EXTRA_CLASS_NAME = "android.bluetooth.device.extra.CLASS_NAME";
    public static final String EXTRA_CONNECTION_ACCESS_RESULT =
            "android.bluetooth.device.extra.CONNECTION_ACCESS_RESULT";
    public static final int CONNECTION_ACCESS_YES = 1;
    public static final int CONNECTION_ACCESS_NO = 2;
    public static final String EXTRA_ALWAYS_ALLOWED =
            "android.bluetooth.device.extra.ALWAYS_ALLOWED";
    public static final int BOND_SUCCESS = 0;
    public static final int UNBOND_REASON_AUTH_FAILED = 1;
    public static final int UNBOND_REASON_AUTH_REJECTED = 2;
    public static final int UNBOND_REASON_AUTH_CANCELED = 3;
    public static final int UNBOND_REASON_REMOTE_DEVICE_DOWN = 4;
    public static final int UNBOND_REASON_DISCOVERY_IN_PROGRESS = 5;
    public static final int UNBOND_REASON_AUTH_TIMEOUT = 6;
    public static final int UNBOND_REASON_REPEATED_ATTEMPTS = 7;
    public static final int UNBOND_REASON_REMOTE_AUTH_CANCELED = 8;
    public static final int UNBOND_REASON_REMOVED = 9;
    public static final int PAIRING_VARIANT_PIN = 0;
    public static final int PAIRING_VARIANT_PASSKEY = 1;
    public static final int PAIRING_VARIANT_PASSKEY_CONFIRMATION = 2;
    public static final int PAIRING_VARIANT_CONSENT = 3;
    public static final int PAIRING_VARIANT_DISPLAY_PASSKEY = 4;
    public static final int PAIRING_VARIANT_DISPLAY_PIN = 5;
    public static final int PAIRING_VARIANT_OOB_CONSENT = 6;
    public static final int PAIRING_VARIANT_PIN_16_DIGITS = 7;
    public static final String EXTRA_UUID = "android.bluetooth.device.extra.UUID";
    public static final String EXTRA_SDP_RECORD =
            "android.bluetooth.device.extra.SDP_RECORD";
    public static final String EXTRA_SDP_SEARCH_STATUS =
            "android.bluetooth.device.extra.SDP_SEARCH_STATUS";
    @IntDef( value = {ACCESS_UNKNOWN,
            ACCESS_ALLOWED, ACCESS_REJECTED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AccessPermission{}
    public static final int ACCESS_UNKNOWN = 0;
    public static final int ACCESS_ALLOWED = 1;
    public static final int ACCESS_REJECTED = 2;
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        value = {
            TRANSPORT_AUTO,
            TRANSPORT_BREDR,
            TRANSPORT_LE,
        }
    )
    public @interface Transport {}
    public static final int TRANSPORT_AUTO = 0;
    public static final int TRANSPORT_BREDR = 1;
    public static final int TRANSPORT_LE = 2;
    public static final int PHY_LE_1M = 1;
    public static final int PHY_LE_2M = 2;
    public static final int PHY_LE_CODED = 3;
    public static final int PHY_LE_1M_MASK = 1;
    public static final int PHY_LE_2M_MASK = 2;
    public static final int PHY_LE_CODED_MASK = 4;
    public static final int PHY_OPTION_NO_PREFERRED = 0;
    public static final int PHY_OPTION_S2 = 1;
    public static final int PHY_OPTION_S8 = 2;
    public static final String EXTRA_MAS_INSTANCE =
            "android.bluetooth.device.extra.MAS_INSTANCE";
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        value = {
            ADDRESS_TYPE_PUBLIC,
            ADDRESS_TYPE_RANDOM,
        }
    )
    public @interface AddressType {}
    public static final int ADDRESS_TYPE_PUBLIC = 0;
    public static final int ADDRESS_TYPE_RANDOM = 1;
    private AttributionSource mAttributionSource;
        BluetoothDevice(String address) {
    }
    public void setAttributionSource(AttributionSource attributionSource) {
        mAttributionSource = attributionSource;
    }
    public void prepareToEnterProcess(AttributionSource attributionSource) {
        setAttributionSource(attributionSource);
    }
    @Override
    public boolean equals(Object o) {
        return false;
    }
    @Override
    public int hashCode() {
        return 0;
    }
    @Override
    public String toString() {
        return "";
    }
    @Override
    public int describeContents() {
        return 0;
    }
    public static final Creator<BluetoothDevice> CREATOR =
            new Creator<BluetoothDevice>() {
                public BluetoothDevice createFromParcel(Parcel in) {
                    return new BluetoothDevice(in.readString());
                }
                public BluetoothDevice[] newArray(int size) {
                    return new BluetoothDevice[size];
                }
            };
    @Override
    public void writeToParcel(Parcel out, int flags) {
    }
    public String getAddress() {
        return null;
    }
    public String getAnonymizedAddress() {
        return "XX:XX:XX" + getAddress().substring(8);
    }
    public String getName() {
        return null;
    }
    public int getType() {
        return DEVICE_TYPE_UNKNOWN;
    }
    public String getAlias() {
        return null;
    }
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            BluetoothStatusCodes.SUCCESS,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED,
            BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ALLOWED,
            BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION,
            BluetoothStatusCodes.ERROR_DEVICE_NOT_BONDED
    })
    public @interface SetAliasReturnValues{}
    public @SetAliasReturnValues int setAlias(String alias) {
        return 0;
    }
    public int getBatteryLevel() {
        return BATTERY_LEVEL_UNKNOWN;
    }
    public boolean createBond() {
        return createBond(TRANSPORT_AUTO);
    }
    public boolean createBond(int transport) {
        return false;
    }
    public boolean isBondingInitiatedLocally() {
        return false;
    }
    public boolean cancelBondProcess() {
        return false;
    }
    public boolean removeBond() {
        return false;
    }
    private static final String BLUETOOTH_BONDING_CACHE_PROPERTY =
            "cache_key.bluetooth.get_bond_state";
    boolean isBluetoothEnabled() {
        boolean ret = false;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && adapter.isEnabled()) {
            ret = true;
        }
        return ret;
    }
    public BluetoothGatt connectGatt(Context context, boolean autoConnect,
            BluetoothGattCallback callback) {
        return (connectGatt(context, autoConnect, callback, TRANSPORT_AUTO));
    }
    public BluetoothGatt connectGatt(Context context, boolean autoConnect,
            BluetoothGattCallback callback, int transport) {
        return (connectGatt(context, autoConnect, callback, transport, PHY_LE_1M_MASK));
    }
    public BluetoothGatt connectGatt(Context context, boolean autoConnect,
            BluetoothGattCallback callback, int transport, int phy) {
        return connectGatt(context, autoConnect, callback, transport, phy, null);
    }
    public BluetoothGatt connectGatt(Context context, boolean autoConnect,
            BluetoothGattCallback callback, int transport, int phy,
            Handler handler) {
        return connectGatt(context, autoConnect, callback, transport, false, phy, handler);
    }
    public BluetoothGatt connectGatt(Context context, boolean autoConnect,
            BluetoothGattCallback callback, int transport,
            boolean opportunistic, int phy, Handler handler) {
        return null;
    }
}
