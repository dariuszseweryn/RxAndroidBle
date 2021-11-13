package android.content;
import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.verify.domain.DomainVerificationManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.ResultReceiver;
import android.os.StrictMode;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.provider.ContactsContract.QuickContact;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.telecom.PhoneAccount;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.proto.ProtoOutputStream;
import androidx.annotation.IntDef;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
/**
 * Used for mocks and constants, but put and get extras must be implemented
 */
public class Intent implements Parcelable, Cloneable {
    private static final String TAG = "Intent";
    private static final String ATTR_ACTION = "action";
    private static final String TAG_CATEGORIES = "categories";
    private static final String ATTR_CATEGORY = "category";
    private static final String TAG_EXTRA = "extra";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_IDENTIFIER = "ident";
    private static final String ATTR_COMPONENT = "component";
    private static final String ATTR_DATA = "data";
    private static final String ATTR_FLAGS = "flags";
    public static final String ACTION_MAIN = "android.intent.action.MAIN";
    public static final String ACTION_VIEW = "android.intent.action.VIEW";
    public static final String EXTRA_FROM_STORAGE = "android.intent.extra.FROM_STORAGE";
    public static final String ACTION_DEFAULT = ACTION_VIEW;
    public static final String ACTION_QUICK_VIEW = "android.intent.action.QUICK_VIEW";
    public static final String ACTION_ATTACH_DATA = "android.intent.action.ATTACH_DATA";
    public static final String ACTION_EDIT = "android.intent.action.EDIT";
    public static final String ACTION_INSERT_OR_EDIT = "android.intent.action.INSERT_OR_EDIT";
    public static final String ACTION_PICK = "android.intent.action.PICK";
    public static final String ACTION_CREATE_REMINDER = "android.intent.action.CREATE_REMINDER";
    public static final String ACTION_CREATE_SHORTCUT = "android.intent.action.CREATE_SHORTCUT";
    @Deprecated
    public static final String EXTRA_SHORTCUT_INTENT = "android.intent.extra.shortcut.INTENT";
    @Deprecated
    public static final String EXTRA_SHORTCUT_NAME = "android.intent.extra.shortcut.NAME";
    @Deprecated
    public static final String EXTRA_SHORTCUT_ICON = "android.intent.extra.shortcut.ICON";
    @Deprecated
    public static final String EXTRA_SHORTCUT_ICON_RESOURCE =
            "android.intent.extra.shortcut.ICON_RESOURCE";
    public static final String ACTION_APPLICATION_PREFERENCES
            = "android.intent.action.APPLICATION_PREFERENCES";
    public static final String ACTION_SHOW_APP_INFO
            = "android.intent.action.SHOW_APP_INFO";
    public static final String ACTION_ACTIVITY_RECOGNIZER =
            "android.intent.action.ACTIVITY_RECOGNIZER";
    public static class ShortcutIconResource implements Parcelable {
        public String packageName;
        public String resourceName;
        public static ShortcutIconResource fromContext(Context context, int resourceId) {
            return null;
        }
        public static final Creator<ShortcutIconResource> CREATOR =
            new Creator<ShortcutIconResource>() {
                public ShortcutIconResource createFromParcel(Parcel source) {
                    ShortcutIconResource icon = new ShortcutIconResource();
                    return null;
                }
                public ShortcutIconResource[] newArray(int size) {
                    return null;
                }
            };
        public int describeContents() {
            return 0;
        }
        public void writeToParcel(Parcel dest, int flags) {
        }
        @Override
        public String toString() {
            return null;
        }
    }
    public static final String ACTION_CHOOSER = "android.intent.action.CHOOSER";
    public static final String ACTION_GET_CONTENT = "android.intent.action.GET_CONTENT";
    public static final String ACTION_DIAL = "android.intent.action.DIAL";
    public static final String ACTION_CALL = "android.intent.action.CALL";
    public static final String ACTION_CALL_EMERGENCY = "android.intent.action.CALL_EMERGENCY";
    public static final String ACTION_DIAL_EMERGENCY = "android.intent.action.DIAL_EMERGENCY";
    public static final String ACTION_CALL_PRIVILEGED = "android.intent.action.CALL_PRIVILEGED";
    public static final String ACTION_CARRIER_SETUP = "android.intent.action.CARRIER_SETUP";
    public static final String ACTION_SENDTO = "android.intent.action.SENDTO";
    public static final String ACTION_SEND = "android.intent.action.SEND";
    public static final String ACTION_SEND_MULTIPLE = "android.intent.action.SEND_MULTIPLE";
    public static final String ACTION_ANSWER = "android.intent.action.ANSWER";
    public static final String ACTION_INSERT = "android.intent.action.INSERT";
    public static final String ACTION_PASTE = "android.intent.action.PASTE";
    public static final String ACTION_DELETE = "android.intent.action.DELETE";
    public static final String ACTION_RUN = "android.intent.action.RUN";
    public static final String ACTION_SYNC = "android.intent.action.SYNC";
    public static final String ACTION_PICK_ACTIVITY = "android.intent.action.PICK_ACTIVITY";
    public static final String ACTION_SEARCH = "android.intent.action.SEARCH";
    public static final String ACTION_SYSTEM_TUTORIAL = "android.intent.action.SYSTEM_TUTORIAL";
    public static final String ACTION_WEB_SEARCH = "android.intent.action.WEB_SEARCH";
    public static final String ACTION_ASSIST = "android.intent.action.ASSIST";
    public static final String ACTION_VOICE_ASSIST = "android.intent.action.VOICE_ASSIST";
    public static final String EXTRA_ASSIST_PACKAGE
            = "android.intent.extra.ASSIST_PACKAGE";
    public static final String EXTRA_ASSIST_UID
            = "android.intent.extra.ASSIST_UID";
    public static final String EXTRA_ASSIST_CONTEXT
            = "android.intent.extra.ASSIST_CONTEXT";
    public static final String EXTRA_ASSIST_INPUT_HINT_KEYBOARD =
            "android.intent.extra.ASSIST_INPUT_HINT_KEYBOARD";
    public static final String EXTRA_ASSIST_INPUT_DEVICE_ID =
            "android.intent.extra.ASSIST_INPUT_DEVICE_ID";
    public static final String ACTION_ALL_APPS = "android.intent.action.ALL_APPS";
    public static final String ACTION_SET_WALLPAPER = "android.intent.action.SET_WALLPAPER";
    public static final String ACTION_BUG_REPORT = "android.intent.action.BUG_REPORT";
    public static final String ACTION_FACTORY_TEST = "android.intent.action.FACTORY_TEST";
    public static final String ACTION_CALL_BUTTON = "android.intent.action.CALL_BUTTON";
    public static final String ACTION_VOICE_COMMAND = "android.intent.action.VOICE_COMMAND";
    public static final String ACTION_SEARCH_LONG_PRESS = "android.intent.action.SEARCH_LONG_PRESS";
    public static final String ACTION_APP_ERROR = "android.intent.action.APP_ERROR";
    public static final String ACTION_PENDING_INCIDENT_REPORTS_CHANGED =
            "android.intent.action.PENDING_INCIDENT_REPORTS_CHANGED";
    public static final String ACTION_INCIDENT_REPORT_READY =
            "android.intent.action.INCIDENT_REPORT_READY";
    public static final String ACTION_POWER_USAGE_SUMMARY = "android.intent.action.POWER_USAGE_SUMMARY";
    @Deprecated
    public static final String ACTION_DEVICE_INITIALIZATION_WIZARD =
            "android.intent.action.DEVICE_INITIALIZATION_WIZARD";
    public static final String ACTION_UPGRADE_SETUP = "android.intent.action.UPGRADE_SETUP";
    public static final String ACTION_SHOW_KEYBOARD_SHORTCUTS =
            "com.android.intent.action.SHOW_KEYBOARD_SHORTCUTS";
    public static final String ACTION_DISMISS_KEYBOARD_SHORTCUTS =
            "com.android.intent.action.DISMISS_KEYBOARD_SHORTCUTS";
    public static final String ACTION_MANAGE_NETWORK_USAGE =
            "android.intent.action.MANAGE_NETWORK_USAGE";
    @Deprecated
    public static final String ACTION_INSTALL_PACKAGE = "android.intent.action.INSTALL_PACKAGE";
    public static final String ACTION_INSTALL_FAILURE = "android.intent.action.INSTALL_FAILURE";
    public static final String ACTION_INSTALL_INSTANT_APP_PACKAGE
            = "android.intent.action.INSTALL_INSTANT_APP_PACKAGE";
    public static final String ACTION_RESOLVE_INSTANT_APP_PACKAGE
            = "android.intent.action.RESOLVE_INSTANT_APP_PACKAGE";
    public static final String ACTION_INSTANT_APP_RESOLVER_SETTINGS
            = "android.intent.action.INSTANT_APP_RESOLVER_SETTINGS";
    public static final String EXTRA_INSTALLER_PACKAGE_NAME
            = "android.intent.extra.INSTALLER_PACKAGE_NAME";
    public static final String EXTRA_NOT_UNKNOWN_SOURCE
            = "android.intent.extra.NOT_UNKNOWN_SOURCE";
    public static final String EXTRA_ORIGINATING_URI
            = "android.intent.extra.ORIGINATING_URI";
    public static final String EXTRA_REFERRER
            = "android.intent.extra.REFERRER";
    public static final String EXTRA_REFERRER_NAME
            = "android.intent.extra.REFERRER_NAME";
    public static final String EXTRA_ORIGINATING_UID
            = "android.intent.extra.ORIGINATING_UID";
    @Deprecated
    public static final String EXTRA_ALLOW_REPLACE
            = "android.intent.extra.ALLOW_REPLACE";
    public static final String EXTRA_RETURN_RESULT
            = "android.intent.extra.RETURN_RESULT";
    public static final String EXTRA_INSTALL_RESULT
            = "android.intent.extra.INSTALL_RESULT";
    @Deprecated
    public static final String ACTION_UNINSTALL_PACKAGE = "android.intent.action.UNINSTALL_PACKAGE";
    public static final String EXTRA_UNINSTALL_ALL_USERS
            = "android.intent.extra.UNINSTALL_ALL_USERS";
    public static final String METADATA_SETUP_VERSION = "android.SETUP_VERSION";
    public static final String ACTION_MANAGE_APP_PERMISSIONS =
            "android.intent.action.MANAGE_APP_PERMISSIONS";
    public static final String ACTION_MANAGE_APP_PERMISSION =
            "android.intent.action.MANAGE_APP_PERMISSION";
    public static final String ACTION_MANAGE_PERMISSIONS =
            "android.intent.action.MANAGE_PERMISSIONS";
    public static final String ACTION_AUTO_REVOKE_PERMISSIONS =
            "android.intent.action.AUTO_REVOKE_PERMISSIONS";
    public static final String ACTION_MANAGE_UNUSED_APPS =
            "android.intent.action.MANAGE_UNUSED_APPS";
    public static final String ACTION_REVIEW_PERMISSIONS =
            "android.intent.action.REVIEW_PERMISSIONS";
    public static final String ACTION_VIEW_PERMISSION_USAGE =
            "android.intent.action.VIEW_PERMISSION_USAGE";
    public static final String ACTION_VIEW_PERMISSION_USAGE_FOR_PERIOD =
            "android.intent.action.VIEW_PERMISSION_USAGE_FOR_PERIOD";
    public static final String ACTION_MANAGE_DEFAULT_APP =
            "android.intent.action.MANAGE_DEFAULT_APP";
    public static final String EXTRA_ROLE_NAME = "android.intent.extra.ROLE_NAME";
    public static final String ACTION_MANAGE_SPECIAL_APP_ACCESSES =
            "android.intent.action.MANAGE_SPECIAL_APP_ACCESSES";
    public static final String EXTRA_REMOTE_CALLBACK = "android.intent.extra.REMOTE_CALLBACK";
    public static final String EXTRA_PACKAGE_NAME = "android.intent.extra.PACKAGE_NAME";
    public static final String EXTRA_SUSPENDED_PACKAGE_EXTRAS = "android.intent.extra.SUSPENDED_PACKAGE_EXTRAS";
    public static final String EXTRA_SPLIT_NAME = "android.intent.extra.SPLIT_NAME";
    public static final String EXTRA_COMPONENT_NAME = "android.intent.extra.COMPONENT_NAME";
    public static final String EXTRA_RESULT_NEEDED = "android.intent.extra.RESULT_NEEDED";
    public static final String EXTRA_SHORTCUT_ID = "android.intent.extra.shortcut.ID";
    public static final String ACTION_MANAGE_PERMISSION_APPS =
            "android.intent.action.MANAGE_PERMISSION_APPS";
    public static final String EXTRA_PERMISSION_NAME = "android.intent.extra.PERMISSION_NAME";
    public static final String EXTRA_PERMISSION_GROUP_NAME =
            "android.intent.extra.PERMISSION_GROUP_NAME";
    public static final String EXTRA_DURATION_MILLIS =
            "android.intent.extra.DURATION_MILLIS";
    public static final String ACTION_REVIEW_PERMISSION_USAGE =
            "android.intent.action.REVIEW_PERMISSION_USAGE";
    public static final String ACTION_REVIEW_PERMISSION_HISTORY =
            "android.intent.action.REVIEW_PERMISSION_HISTORY";
    public static final String ACTION_REVIEW_ONGOING_PERMISSION_USAGE =
            "android.intent.action.REVIEW_ONGOING_PERMISSION_USAGE";
    public static final String ACTION_REVIEW_ACCESSIBILITY_SERVICES =
            "android.intent.action.REVIEW_ACCESSIBILITY_SERVICES";
    public static final String ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF";
    public static final String ACTION_SCREEN_ON = "android.intent.action.SCREEN_ON";
    public static final String ACTION_DREAMING_STOPPED = "android.intent.action.DREAMING_STOPPED";
    public static final String ACTION_DREAMING_STARTED = "android.intent.action.DREAMING_STARTED";
    public static final String ACTION_USER_PRESENT = "android.intent.action.USER_PRESENT";
    public static final String ACTION_TIME_TICK = "android.intent.action.TIME_TICK";
    public static final String ACTION_TIME_CHANGED = "android.intent.action.TIME_SET";
    public static final String ACTION_DATE_CHANGED = "android.intent.action.DATE_CHANGED";
    public static final String ACTION_TIMEZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED";
    public static final String ACTION_ALARM_CHANGED = "android.intent.action.ALARM_CHANGED";
    public static final String ACTION_LOCKED_BOOT_COMPLETED = "android.intent.action.LOCKED_BOOT_COMPLETED";
    public static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    @Deprecated
    public static final String ACTION_CLOSE_SYSTEM_DIALOGS = "android.intent.action.CLOSE_SYSTEM_DIALOGS";
    @Deprecated
    public static final String ACTION_PACKAGE_INSTALL = "android.intent.action.PACKAGE_INSTALL";
    public static final String ACTION_PACKAGE_ADDED = "android.intent.action.PACKAGE_ADDED";
    public static final String ACTION_PACKAGE_REPLACED = "android.intent.action.PACKAGE_REPLACED";
    public static final String ACTION_MY_PACKAGE_REPLACED = "android.intent.action.MY_PACKAGE_REPLACED";
    public static final String ACTION_PACKAGE_REMOVED = "android.intent.action.PACKAGE_REMOVED";
    public static final String ACTION_PACKAGE_REMOVED_INTERNAL =
            "android.intent.action.PACKAGE_REMOVED_INTERNAL";
    public static final String ACTION_PACKAGE_FULLY_REMOVED
            = "android.intent.action.PACKAGE_FULLY_REMOVED";
    public static final String ACTION_PACKAGE_CHANGED = "android.intent.action.PACKAGE_CHANGED";
    public static final String ACTION_PACKAGE_ENABLE_ROLLBACK =
            "android.intent.action.PACKAGE_ENABLE_ROLLBACK";
    public static final String ACTION_CANCEL_ENABLE_ROLLBACK =
            "android.intent.action.CANCEL_ENABLE_ROLLBACK";
    public static final String ACTION_ROLLBACK_COMMITTED =
            "android.intent.action.ROLLBACK_COMMITTED";
    public static final String ACTION_QUERY_PACKAGE_RESTART = "android.intent.action.QUERY_PACKAGE_RESTART";
    public static final String ACTION_PACKAGE_RESTARTED = "android.intent.action.PACKAGE_RESTARTED";
    public static final String ACTION_PACKAGE_DATA_CLEARED = "android.intent.action.PACKAGE_DATA_CLEARED";
    public static final String ACTION_PACKAGES_SUSPENDED = "android.intent.action.PACKAGES_SUSPENDED";
    public static final String ACTION_PACKAGES_UNSUSPENDED = "android.intent.action.PACKAGES_UNSUSPENDED";
    public static final String ACTION_DISTRACTING_PACKAGES_CHANGED =
            "android.intent.action.DISTRACTING_PACKAGES_CHANGED";
    public static final String ACTION_MY_PACKAGE_SUSPENDED = "android.intent.action.MY_PACKAGE_SUSPENDED";
    public static final String ACTION_SHOW_SUSPENDED_APP_DETAILS =
            "android.intent.action.SHOW_SUSPENDED_APP_DETAILS";
    public static final String ACTION_PACKAGE_UNSUSPENDED_MANUALLY =
            "android.intent.action.PACKAGE_UNSUSPENDED_MANUALLY";
    public static final String ACTION_MY_PACKAGE_UNSUSPENDED = "android.intent.action.MY_PACKAGE_UNSUSPENDED";
    public static final String ACTION_UID_REMOVED = "android.intent.action.UID_REMOVED";
    public static final String ACTION_PACKAGE_FIRST_LAUNCH = "android.intent.action.PACKAGE_FIRST_LAUNCH";
    public static final String ACTION_PACKAGE_NEEDS_VERIFICATION = "android.intent.action.PACKAGE_NEEDS_VERIFICATION";
    public static final String ACTION_PACKAGE_VERIFIED = "android.intent.action.PACKAGE_VERIFIED";
    @Deprecated
    public static final String ACTION_INTENT_FILTER_NEEDS_VERIFICATION =
            "android.intent.action.INTENT_FILTER_NEEDS_VERIFICATION";
    public static final String ACTION_DOMAINS_NEED_VERIFICATION =
            "android.intent.action.DOMAINS_NEED_VERIFICATION";
    public static final String ACTION_EXTERNAL_APPLICATIONS_AVAILABLE =
        "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE";
    public static final String ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE =
        "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE";
    public static final String ACTION_PREFERRED_ACTIVITY_CHANGED =
            "android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED";
    public static final String ACTION_WALLPAPER_CHANGED = "android.intent.action.WALLPAPER_CHANGED";
    public static final String ACTION_CONFIGURATION_CHANGED = "android.intent.action.CONFIGURATION_CHANGED";
    public static final String ACTION_SPLIT_CONFIGURATION_CHANGED =
            "android.intent.action.SPLIT_CONFIGURATION_CHANGED";
    public static final String ACTION_LOCALE_CHANGED = "android.intent.action.LOCALE_CHANGED";
    public static final String ACTION_BATTERY_CHANGED = "android.intent.action.BATTERY_CHANGED";
    public static final String ACTION_BATTERY_LEVEL_CHANGED =
            "android.intent.action.BATTERY_LEVEL_CHANGED";
    public static final String ACTION_BATTERY_LOW = "android.intent.action.BATTERY_LOW";
    public static final String ACTION_BATTERY_OKAY = "android.intent.action.BATTERY_OKAY";
    public static final String ACTION_POWER_CONNECTED = "android.intent.action.ACTION_POWER_CONNECTED";
    public static final String ACTION_POWER_DISCONNECTED =
            "android.intent.action.ACTION_POWER_DISCONNECTED";
    public static final String ACTION_SHUTDOWN = "android.intent.action.ACTION_SHUTDOWN";
    public static final String ACTION_REQUEST_SHUTDOWN
            = "com.android.internal.intent.action.REQUEST_SHUTDOWN";
    @Deprecated
    public static final String ACTION_DEVICE_STORAGE_LOW = "android.intent.action.DEVICE_STORAGE_LOW";
    @Deprecated
    public static final String ACTION_DEVICE_STORAGE_OK = "android.intent.action.DEVICE_STORAGE_OK";
    @Deprecated
    public static final String ACTION_DEVICE_STORAGE_FULL = "android.intent.action.DEVICE_STORAGE_FULL";
    @Deprecated
    public static final String ACTION_DEVICE_STORAGE_NOT_FULL = "android.intent.action.DEVICE_STORAGE_NOT_FULL";
    public static final String ACTION_MANAGE_PACKAGE_STORAGE = "android.intent.action.MANAGE_PACKAGE_STORAGE";
    @Deprecated
    public static final String ACTION_UMS_CONNECTED = "android.intent.action.UMS_CONNECTED";
    @Deprecated
    public static final String ACTION_UMS_DISCONNECTED = "android.intent.action.UMS_DISCONNECTED";
    public static final String ACTION_MEDIA_REMOVED = "android.intent.action.MEDIA_REMOVED";
    public static final String ACTION_MEDIA_UNMOUNTED = "android.intent.action.MEDIA_UNMOUNTED";
    public static final String ACTION_MEDIA_CHECKING = "android.intent.action.MEDIA_CHECKING";
    public static final String ACTION_MEDIA_NOFS = "android.intent.action.MEDIA_NOFS";
    public static final String ACTION_MEDIA_MOUNTED = "android.intent.action.MEDIA_MOUNTED";
    public static final String ACTION_MEDIA_SHARED = "android.intent.action.MEDIA_SHARED";
    public static final String ACTION_MEDIA_UNSHARED = "android.intent.action.MEDIA_UNSHARED";
    public static final String ACTION_MEDIA_BAD_REMOVAL = "android.intent.action.MEDIA_BAD_REMOVAL";
    public static final String ACTION_MEDIA_UNMOUNTABLE = "android.intent.action.MEDIA_UNMOUNTABLE";
    public static final String ACTION_MEDIA_EJECT = "android.intent.action.MEDIA_EJECT";
    public static final String ACTION_MEDIA_SCANNER_STARTED = "android.intent.action.MEDIA_SCANNER_STARTED";
    public static final String ACTION_MEDIA_SCANNER_FINISHED = "android.intent.action.MEDIA_SCANNER_FINISHED";
    @Deprecated
    public static final String ACTION_MEDIA_SCANNER_SCAN_FILE = "android.intent.action.MEDIA_SCANNER_SCAN_FILE";
    public static final String ACTION_MEDIA_BUTTON = "android.intent.action.MEDIA_BUTTON";
    public static final String ACTION_CAMERA_BUTTON = "android.intent.action.CAMERA_BUTTON";
    public static final String ACTION_GTALK_SERVICE_CONNECTED =
            "android.intent.action.GTALK_CONNECTED";
    public static final String ACTION_GTALK_SERVICE_DISCONNECTED =
            "android.intent.action.GTALK_DISCONNECTED";
    public static final String ACTION_INPUT_METHOD_CHANGED =
            "android.intent.action.INPUT_METHOD_CHANGED";
    public static final String ACTION_AIRPLANE_MODE_CHANGED = "android.intent.action.AIRPLANE_MODE";
    public static final String ACTION_PROVIDER_CHANGED =
            "android.intent.action.PROVIDER_CHANGED";
    public static final String ACTION_HEADSET_PLUG = android.media.AudioManager.ACTION_HEADSET_PLUG;
    //@SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_ADVANCED_SETTINGS_CHANGED
            = "android.intent.action.ADVANCED_SETTINGS";
    public static final String ACTION_APPLICATION_RESTRICTIONS_CHANGED =
            "android.intent.action.APPLICATION_RESTRICTIONS_CHANGED";
    @Deprecated
    public static final String ACTION_NEW_OUTGOING_CALL =
            "android.intent.action.NEW_OUTGOING_CALL";
    public static final String ACTION_REBOOT =
            "android.intent.action.REBOOT";
    public static final String ACTION_DOCK_EVENT =
            "android.intent.action.DOCK_EVENT";
    public static final String ACTION_IDLE_MAINTENANCE_START =
            "android.intent.action.ACTION_IDLE_MAINTENANCE_START";
    public static final String ACTION_IDLE_MAINTENANCE_END =
            "android.intent.action.ACTION_IDLE_MAINTENANCE_END";
    public static final String ACTION_REMOTE_INTENT =
            "com.google.android.c2dm.intent.RECEIVE";
    public static final String ACTION_PRE_BOOT_COMPLETED =
            "android.intent.action.PRE_BOOT_COMPLETED";
    public static final String ACTION_GET_RESTRICTION_ENTRIES =
            "android.intent.action.GET_RESTRICTION_ENTRIES";
    public static final String ACTION_USER_INITIALIZE =
            "android.intent.action.USER_INITIALIZE";
    public static final String ACTION_USER_FOREGROUND =
            "android.intent.action.USER_FOREGROUND";
    public static final String ACTION_USER_BACKGROUND =
            "android.intent.action.USER_BACKGROUND";
    public static final String ACTION_USER_ADDED =
            "android.intent.action.USER_ADDED";
    public static final String ACTION_USER_STARTED =
            "android.intent.action.USER_STARTED";
    public static final String ACTION_USER_STARTING =
            "android.intent.action.USER_STARTING";
    public static final String ACTION_USER_STOPPING =
            "android.intent.action.USER_STOPPING";
    public static final String ACTION_USER_STOPPED =
            "android.intent.action.USER_STOPPED";
    public static final String ACTION_USER_REMOVED =
            "android.intent.action.USER_REMOVED";
    public static final String ACTION_USER_SWITCHED =
            "android.intent.action.USER_SWITCHED";
    public static final String ACTION_USER_UNLOCKED = "android.intent.action.USER_UNLOCKED";
    public static final String ACTION_USER_INFO_CHANGED =
            "android.intent.action.USER_INFO_CHANGED";
    public static final String ACTION_MANAGED_PROFILE_ADDED =
            "android.intent.action.MANAGED_PROFILE_ADDED";
    public static final String ACTION_MANAGED_PROFILE_REMOVED =
            "android.intent.action.MANAGED_PROFILE_REMOVED";
    public static final String ACTION_MANAGED_PROFILE_UNLOCKED =
            "android.intent.action.MANAGED_PROFILE_UNLOCKED";
    public static final String ACTION_MANAGED_PROFILE_AVAILABLE =
            "android.intent.action.MANAGED_PROFILE_AVAILABLE";
    public static final String ACTION_MANAGED_PROFILE_UNAVAILABLE =
            "android.intent.action.MANAGED_PROFILE_UNAVAILABLE";
    public static final String ACTION_PROFILE_ACCESSIBLE =
            "android.intent.action.PROFILE_ACCESSIBLE";
    public static final String ACTION_PROFILE_INACCESSIBLE =
            "android.intent.action.PROFILE_INACCESSIBLE";
    public static final String ACTION_DEVICE_LOCKED_CHANGED =
            "android.intent.action.DEVICE_LOCKED_CHANGED";
    public static final String ACTION_QUICK_CLOCK =
            "android.intent.action.QUICK_CLOCK";
    public static final String ACTION_SHOW_BRIGHTNESS_DIALOG =
            "com.android.intent.action.SHOW_BRIGHTNESS_DIALOG";
    public static final String ACTION_GLOBAL_BUTTON = "android.intent.action.GLOBAL_BUTTON";
    public static final String ACTION_MEDIA_RESOURCE_GRANTED =
            "android.intent.action.MEDIA_RESOURCE_GRANTED";
    public static final String ACTION_OVERLAY_CHANGED = "android.intent.action.OVERLAY_CHANGED";
    public static final String ACTION_OPEN_DOCUMENT = "android.intent.action.OPEN_DOCUMENT";
    public static final String ACTION_CREATE_DOCUMENT = "android.intent.action.CREATE_DOCUMENT";
    public static final String
            ACTION_OPEN_DOCUMENT_TREE = "android.intent.action.OPEN_DOCUMENT_TREE";
    public static final String ACTION_TRANSLATE = "android.intent.action.TRANSLATE";
    public static final String ACTION_DEFINE = "android.intent.action.DEFINE";
    public static final String
            ACTION_DYNAMIC_SENSOR_CHANGED = "android.intent.action.DYNAMIC_SENSOR_CHANGED";
    @Deprecated
    public static final String ACTION_MASTER_CLEAR = "android.intent.action.MASTER_CLEAR";
    public static final String ACTION_MASTER_CLEAR_NOTIFICATION
            = "android.intent.action.MASTER_CLEAR_NOTIFICATION";
    @Deprecated
    public static final String EXTRA_FORCE_MASTER_CLEAR =
            "android.intent.extra.FORCE_MASTER_CLEAR";
    public static final String ACTION_FACTORY_RESET = "android.intent.action.FACTORY_RESET";
    public static final String EXTRA_FORCE_FACTORY_RESET =
            "android.intent.extra.FORCE_FACTORY_RESET";
    public static final String ACTION_SETTING_RESTORED = "android.os.action.SETTING_RESTORED";
    public static final String EXTRA_SETTING_NAME = "setting_name";
    public static final String EXTRA_SETTING_PREVIOUS_VALUE = "previous_value";
    public static final String EXTRA_SETTING_NEW_VALUE = "new_value";
    public static final String EXTRA_SETTING_RESTORED_FROM_SDK_INT = "restored_from_sdk_int";
    public static final String ACTION_PROCESS_TEXT = "android.intent.action.PROCESS_TEXT";
    @Deprecated
    public static final String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    public static final String EXTRA_SIM_STATE = "ss";
    public static final String SIM_STATE_UNKNOWN = "UNKNOWN";
    public static final String SIM_STATE_NOT_READY = "NOT_READY";
    public static final String SIM_STATE_ABSENT = "ABSENT";
    public static final String SIM_STATE_PRESENT = "PRESENT";
    static public final String SIM_STATE_CARD_IO_ERROR = "CARD_IO_ERROR";
    static public final String SIM_STATE_CARD_RESTRICTED = "CARD_RESTRICTED";
    public static final String SIM_STATE_LOCKED = "LOCKED";
    public static final String SIM_STATE_READY = "READY";
    public static final String SIM_STATE_IMSI = "IMSI";
    public static final String SIM_STATE_LOADED = "LOADED";
    public static final String EXTRA_SIM_LOCKED_REASON = "reason";
    public static final String SIM_LOCKED_ON_PIN = "PIN";
        public static final String SIM_LOCKED_ON_PUK = "PUK";
    public static final String SIM_LOCKED_NETWORK = "NETWORK";
    public static final String SIM_ABSENT_ON_PERM_DISABLED = "PERM_DISABLED";
    public static final String EXTRA_REBROADCAST_ON_UNLOCK = "rebroadcastOnUnlock";
    @Deprecated
    public static final String ACTION_SERVICE_STATE = "android.intent.action.SERVICE_STATE";
    public static final String ACTION_LOAD_DATA = "android.intent.action.LOAD_DATA";
    @Deprecated
    public static final String EXTRA_VOICE_REG_STATE = "voiceRegState";
    @Deprecated
    public static final String EXTRA_DATA_REG_STATE = "dataRegState";
    @Deprecated
    public static final String EXTRA_VOICE_ROAMING_TYPE = "voiceRoamingType";
    @Deprecated
    public static final String EXTRA_DATA_ROAMING_TYPE = "dataRoamingType";
    @Deprecated
    public static final String EXTRA_OPERATOR_ALPHA_LONG = "operator-alpha-long";
    @Deprecated
    public static final String EXTRA_OPERATOR_ALPHA_SHORT = "operator-alpha-short";
    @Deprecated
    public static final String EXTRA_OPERATOR_NUMERIC = "operator-numeric";
    @Deprecated
    public static final String EXTRA_DATA_OPERATOR_ALPHA_LONG = "data-operator-alpha-long";
    @Deprecated
    public static final String EXTRA_DATA_OPERATOR_ALPHA_SHORT = "data-operator-alpha-short";
    @Deprecated
    public static final String EXTRA_DATA_OPERATOR_NUMERIC = "data-operator-numeric";
    @Deprecated
    public static final String EXTRA_MANUAL = "manual";
    @Deprecated
    public static final String EXTRA_VOICE_RADIO_TECH = "radioTechnology";
    @Deprecated
    public static final String EXTRA_DATA_RADIO_TECH = "dataRadioTechnology";
    @Deprecated
    public static final String EXTRA_CSS_INDICATOR = "cssIndicator";
    @Deprecated
    public static final String EXTRA_NETWORK_ID = "networkId";
    @Deprecated
    public static final String EXTRA_SYSTEM_ID = "systemId";
    @Deprecated
    public static final String EXTRA_CDMA_ROAMING_INDICATOR = "cdmaRoamingIndicator";
    @Deprecated
    public static final String EXTRA_CDMA_DEFAULT_ROAMING_INDICATOR = "cdmaDefaultRoamingIndicator";
    @Deprecated
    public static final String EXTRA_EMERGENCY_ONLY = "emergencyOnly";
    @Deprecated
    public static final String EXTRA_IS_DATA_ROAMING_FROM_REGISTRATION =
            "isDataRoamingFromRegistration";
    @Deprecated
    public static final String EXTRA_IS_USING_CARRIER_AGGREGATION = "isUsingCarrierAggregation";
    @Deprecated
    public static final String EXTRA_LTE_EARFCN_RSRP_BOOST = "LteEarfcnRsrpBoost";
    public static final String EXTRA_PROCESS_TEXT = "android.intent.extra.PROCESS_TEXT";
    public static final String EXTRA_PROCESS_TEXT_READONLY =
            "android.intent.extra.PROCESS_TEXT_READONLY";
    public static final String ACTION_THERMAL_EVENT = "android.intent.action.THERMAL_EVENT";
    public static final String EXTRA_THERMAL_STATE = "android.intent.extra.THERMAL_STATE";
    public static final int EXTRA_THERMAL_STATE_NORMAL = 0;
    public static final int EXTRA_THERMAL_STATE_WARNING = 1;
    public static final int EXTRA_THERMAL_STATE_EXCEEDED = 2;
    public static final String ACTION_DOCK_IDLE = "android.intent.action.DOCK_IDLE";
    public static final String ACTION_DOCK_ACTIVE = "android.intent.action.DOCK_ACTIVE";
    public static final String ACTION_DEVICE_CUSTOMIZATION_READY =
            "android.intent.action.DEVICE_CUSTOMIZATION_READY";
    public static final String ACTION_VIEW_LOCUS = "android.intent.action.VIEW_LOCUS";
    public static final String ACTION_PACKAGE_NEEDS_INTEGRITY_VERIFICATION =
            "android.intent.action.PACKAGE_NEEDS_INTEGRITY_VERIFICATION";
    public static final String CATEGORY_DEFAULT = "android.intent.category.DEFAULT";
    public static final String CATEGORY_BROWSABLE = "android.intent.category.BROWSABLE";
    public static final String CATEGORY_VOICE = "android.intent.category.VOICE";
    public static final String CATEGORY_ALTERNATIVE = "android.intent.category.ALTERNATIVE";
    public static final String CATEGORY_SELECTED_ALTERNATIVE = "android.intent.category.SELECTED_ALTERNATIVE";
    public static final String CATEGORY_TAB = "android.intent.category.TAB";
    public static final String CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";
    public static final String CATEGORY_LEANBACK_LAUNCHER = "android.intent.category.LEANBACK_LAUNCHER";
    public static final String CATEGORY_CAR_LAUNCHER = "android.intent.category.CAR_LAUNCHER";
    public static final String CATEGORY_LEANBACK_SETTINGS = "android.intent.category.LEANBACK_SETTINGS";
    public static final String CATEGORY_INFO = "android.intent.category.INFO";
    public static final String CATEGORY_HOME = "android.intent.category.HOME";
    public static final String CATEGORY_HOME_MAIN = "android.intent.category.HOME_MAIN";
    public static final String CATEGORY_SECONDARY_HOME = "android.intent.category.SECONDARY_HOME";
    public static final String CATEGORY_SETUP_WIZARD = "android.intent.category.SETUP_WIZARD";
    public static final String CATEGORY_LAUNCHER_APP = "android.intent.category.LAUNCHER_APP";
    public static final String CATEGORY_PREFERENCE = "android.intent.category.PREFERENCE";
    public static final String CATEGORY_DEVELOPMENT_PREFERENCE = "android.intent.category.DEVELOPMENT_PREFERENCE";
    public static final String CATEGORY_EMBED = "android.intent.category.EMBED";
    public static final String CATEGORY_APP_MARKET = "android.intent.category.APP_MARKET";
    public static final String CATEGORY_MONKEY = "android.intent.category.MONKEY";
    public static final String CATEGORY_TEST = "android.intent.category.TEST";
    public static final String CATEGORY_UNIT_TEST = "android.intent.category.UNIT_TEST";
    public static final String CATEGORY_SAMPLE_CODE = "android.intent.category.SAMPLE_CODE";
    public static final String CATEGORY_OPENABLE = "android.intent.category.OPENABLE";
    public static final String CATEGORY_TYPED_OPENABLE  =
            "android.intent.category.TYPED_OPENABLE";
    public static final String CATEGORY_FRAMEWORK_INSTRUMENTATION_TEST =
            "android.intent.category.FRAMEWORK_INSTRUMENTATION_TEST";
    public static final String CATEGORY_CAR_DOCK = "android.intent.category.CAR_DOCK";
    public static final String CATEGORY_DESK_DOCK = "android.intent.category.DESK_DOCK";
    public static final String CATEGORY_LE_DESK_DOCK = "android.intent.category.LE_DESK_DOCK";
    public static final String CATEGORY_HE_DESK_DOCK = "android.intent.category.HE_DESK_DOCK";
    public static final String CATEGORY_CAR_MODE = "android.intent.category.CAR_MODE";
    public static final String CATEGORY_VR_HOME = "android.intent.category.VR_HOME";
    public static final String CATEGORY_ACCESSIBILITY_SHORTCUT_TARGET =
            "android.intent.category.ACCESSIBILITY_SHORTCUT_TARGET";
    public static final String CATEGORY_APP_BROWSER = "android.intent.category.APP_BROWSER";
    public static final String CATEGORY_APP_CALCULATOR = "android.intent.category.APP_CALCULATOR";
    public static final String CATEGORY_APP_CALENDAR = "android.intent.category.APP_CALENDAR";
    public static final String CATEGORY_APP_CONTACTS = "android.intent.category.APP_CONTACTS";
    public static final String CATEGORY_APP_EMAIL = "android.intent.category.APP_EMAIL";
    public static final String CATEGORY_APP_GALLERY = "android.intent.category.APP_GALLERY";
    public static final String CATEGORY_APP_MAPS = "android.intent.category.APP_MAPS";
    public static final String CATEGORY_APP_MESSAGING = "android.intent.category.APP_MESSAGING";
    public static final String CATEGORY_APP_MUSIC = "android.intent.category.APP_MUSIC";
    public static final String CATEGORY_APP_FILES = "android.intent.category.APP_FILES";
    public static final String EXTRA_TEMPLATE = "android.intent.extra.TEMPLATE";
    public static final String EXTRA_TEXT = "android.intent.extra.TEXT";
    public static final String EXTRA_HTML_TEXT = "android.intent.extra.HTML_TEXT";
    public static final String EXTRA_STREAM = "android.intent.extra.STREAM";
    public static final String EXTRA_EMAIL       = "android.intent.extra.EMAIL";
    public static final String EXTRA_CC       = "android.intent.extra.CC";
    public static final String EXTRA_BCC      = "android.intent.extra.BCC";
    public static final String EXTRA_SUBJECT  = "android.intent.extra.SUBJECT";
    public static final String EXTRA_INTENT = "android.intent.extra.INTENT";
    public static final String EXTRA_USER_ID = "android.intent.extra.USER_ID";
    public static final String EXTRA_TASK_ID = "android.intent.extra.TASK_ID";
    public static final String EXTRA_ATTRIBUTION_TAGS = "android.intent.extra.ATTRIBUTION_TAGS";
    public static final String EXTRA_START_TIME = "android.intent.extra.START_TIME";
    public static final String EXTRA_END_TIME = "android.intent.extra.END_TIME";
    public static final String EXTRA_ALTERNATE_INTENTS = "android.intent.extra.ALTERNATE_INTENTS";
    public static final String EXTRA_EXCLUDE_COMPONENTS
            = "android.intent.extra.EXCLUDE_COMPONENTS";
    public static final String EXTRA_CHOOSER_TARGETS = "android.intent.extra.CHOOSER_TARGETS";
    public static final String EXTRA_CHOOSER_REFINEMENT_INTENT_SENDER
            = "android.intent.extra.CHOOSER_REFINEMENT_INTENT_SENDER";
    public static final String EXTRA_CONTENT_ANNOTATIONS
            = "android.intent.extra.CONTENT_ANNOTATIONS";
    public static final String EXTRA_RESULT_RECEIVER
            = "android.intent.extra.RESULT_RECEIVER";
    public static final String EXTRA_TITLE = "android.intent.extra.TITLE";
    public static final String EXTRA_INITIAL_INTENTS = "android.intent.extra.INITIAL_INTENTS";
    public static final String EXTRA_INSTANT_APP_SUCCESS =
            "android.intent.extra.INSTANT_APP_SUCCESS";
    public static final String EXTRA_INSTANT_APP_FAILURE =
            "android.intent.extra.INSTANT_APP_FAILURE";
    public static final String EXTRA_INSTANT_APP_HOSTNAME =
            "android.intent.extra.INSTANT_APP_HOSTNAME";
    public static final String EXTRA_INSTANT_APP_TOKEN =
            "android.intent.extra.INSTANT_APP_TOKEN";
    public static final String EXTRA_INSTANT_APP_ACTION = "android.intent.extra.INSTANT_APP_ACTION";
    public static final String EXTRA_INSTANT_APP_BUNDLES =
            "android.intent.extra.INSTANT_APP_BUNDLES";
    public static final String EXTRA_INSTANT_APP_EXTRAS =
            "android.intent.extra.INSTANT_APP_EXTRAS";
    public static final String EXTRA_UNKNOWN_INSTANT_APP =
            "android.intent.extra.UNKNOWN_INSTANT_APP";
    @Deprecated
    public static final String EXTRA_VERSION_CODE = "android.intent.extra.VERSION_CODE";
    public static final String EXTRA_LONG_VERSION_CODE = "android.intent.extra.LONG_VERSION_CODE";
    public static final String EXTRA_CALLING_PACKAGE
            = "android.intent.extra.CALLING_PACKAGE";
    public static final String EXTRA_VERIFICATION_BUNDLE
            = "android.intent.extra.VERIFICATION_BUNDLE";
    public static final String EXTRA_REPLACEMENT_EXTRAS =
            "android.intent.extra.REPLACEMENT_EXTRAS";
    public static final String EXTRA_CHOSEN_COMPONENT_INTENT_SENDER =
            "android.intent.extra.CHOSEN_COMPONENT_INTENT_SENDER";
    public static final String EXTRA_CHOSEN_COMPONENT = "android.intent.extra.CHOSEN_COMPONENT";
    public static final String EXTRA_KEY_EVENT = "android.intent.extra.KEY_EVENT";
    public static final String EXTRA_KEY_CONFIRM = "android.intent.extra.KEY_CONFIRM";
    public static final String EXTRA_USER_REQUESTED_SHUTDOWN =
            "android.intent.extra.USER_REQUESTED_SHUTDOWN";
    public static final String EXTRA_DONT_KILL_APP = "android.intent.extra.DONT_KILL_APP";
    public static final String EXTRA_USER_INITIATED = "android.intent.extra.USER_INITIATED";
    public static final String EXTRA_PHONE_NUMBER = "android.intent.extra.PHONE_NUMBER";
    public static final String EXTRA_UID = "android.intent.extra.UID";
    public static final String EXTRA_PACKAGES = "android.intent.extra.PACKAGES";
    public static final String EXTRA_DATA_REMOVED = "android.intent.extra.DATA_REMOVED";
    public static final String EXTRA_REMOVED_FOR_ALL_USERS
            = "android.intent.extra.REMOVED_FOR_ALL_USERS";
    public static final String EXTRA_REPLACING = "android.intent.extra.REPLACING";
    public static final String EXTRA_ALARM_COUNT = "android.intent.extra.ALARM_COUNT";
    public static final String EXTRA_DOCK_STATE = "android.intent.extra.DOCK_STATE";
    public static final int EXTRA_DOCK_STATE_UNDOCKED = 0;
    public static final int EXTRA_DOCK_STATE_DESK = 1;
    public static final int EXTRA_DOCK_STATE_CAR = 2;
    public static final int EXTRA_DOCK_STATE_LE_DESK = 3;
    public static final int EXTRA_DOCK_STATE_HE_DESK = 4;
    public static final String METADATA_DOCK_HOME = "android.dock_home";
    public static final String EXTRA_BUG_REPORT = "android.intent.extra.BUG_REPORT";
    public static final String EXTRA_REMOTE_INTENT_TOKEN =
            "android.intent.extra.remote_intent_token";
    @Deprecated public static final String EXTRA_CHANGED_COMPONENT_NAME =
            "android.intent.extra.changed_component_name";
    public static final String EXTRA_CHANGED_COMPONENT_NAME_LIST =
            "android.intent.extra.changed_component_name_list";
    public static final String EXTRA_CHANGED_PACKAGE_LIST =
            "android.intent.extra.changed_package_list";
    public static final String EXTRA_CHANGED_UID_LIST =
            "android.intent.extra.changed_uid_list";
    public static final String EXTRA_DISTRACTION_RESTRICTIONS =
            "android.intent.extra.distraction_restrictions";
    public static final String EXTRA_CLIENT_LABEL =
            "android.intent.extra.client_label";
    public static final String EXTRA_CLIENT_INTENT =
            "android.intent.extra.client_intent";
    public static final String EXTRA_LOCAL_ONLY =
            "android.intent.extra.LOCAL_ONLY";
    public static final String EXTRA_ALLOW_MULTIPLE =
            "android.intent.extra.ALLOW_MULTIPLE";
    public static final String EXTRA_USER_HANDLE =
            "android.intent.extra.user_handle";
    public static final String EXTRA_USER =
            "android.intent.extra.USER";
    public static final String EXTRA_RESTRICTIONS_LIST = "android.intent.extra.restrictions_list";
    public static final String EXTRA_RESTRICTIONS_BUNDLE =
            "android.intent.extra.restrictions_bundle";
    public static final String EXTRA_RESTRICTIONS_INTENT =
            "android.intent.extra.restrictions_intent";
    public static final String EXTRA_MIME_TYPES = "android.intent.extra.MIME_TYPES";
    public static final String EXTRA_SHUTDOWN_USERSPACE_ONLY
            = "android.intent.extra.SHUTDOWN_USERSPACE_ONLY";
    public static final String EXTRA_TIME = "android.intent.extra.TIME";
    @SuppressLint("ActionValue")
    public static final String EXTRA_TIMEZONE = "time-zone";
    public static final String EXTRA_TIME_PREF_24_HOUR_FORMAT =
            "android.intent.extra.TIME_PREF_24_HOUR_FORMAT";
    public static final int EXTRA_TIME_PREF_VALUE_USE_12_HOUR = 0;
    public static final int EXTRA_TIME_PREF_VALUE_USE_24_HOUR = 1;
    public static final int EXTRA_TIME_PREF_VALUE_USE_LOCALE_DEFAULT = 2;
    public static final String EXTRA_REASON = "android.intent.extra.REASON";
    public static final String EXTRA_WIPE_EXTERNAL_STORAGE = "android.intent.extra.WIPE_EXTERNAL_STORAGE";
    public static final String EXTRA_WIPE_ESIMS = "com.android.internal.intent.extra.WIPE_ESIMS";
    public static final String EXTRA_SIM_ACTIVATION_RESPONSE =
            "android.intent.extra.SIM_ACTIVATION_RESPONSE";
    public static final String EXTRA_INDEX = "android.intent.extra.INDEX";
    @Deprecated
    public static final String EXTRA_QUICK_VIEW_ADVANCED =
            "android.intent.extra.QUICK_VIEW_ADVANCED";
    public static final String EXTRA_QUICK_VIEW_FEATURES =
            "android.intent.extra.QUICK_VIEW_FEATURES";
    public static final String EXTRA_QUIET_MODE = "android.intent.extra.QUIET_MODE";
    public static final String EXTRA_CONTENT_QUERY = "android.intent.extra.CONTENT_QUERY";
    public static final String EXTRA_MEDIA_RESOURCE_TYPE =
            "android.intent.extra.MEDIA_RESOURCE_TYPE";
    public static final String EXTRA_AUTO_LAUNCH_SINGLE_CHOICE =
            "android.intent.extra.AUTO_LAUNCH_SINGLE_CHOICE";
    public static final int EXTRA_MEDIA_RESOURCE_TYPE_VIDEO_CODEC = 0;
    public static final int EXTRA_MEDIA_RESOURCE_TYPE_AUDIO_CODEC = 1;
    public static final String EXTRA_LOCUS_ID = "android.intent.extra.LOCUS_ID";
    public static final String EXTRA_VISIBILITY_ALLOW_LIST =
            "android.intent.extra.VISIBILITY_ALLOW_LIST";
    @IntDef(flag = true, value = {
            FLAG_GRANT_READ_URI_PERMISSION, FLAG_GRANT_WRITE_URI_PERMISSION,
            FLAG_GRANT_PERSISTABLE_URI_PERMISSION, FLAG_GRANT_PREFIX_URI_PERMISSION })
    @Retention(RetentionPolicy.SOURCE)
    public @interface GrantUriMode {}
    @IntDef(flag = true, value = {
            FLAG_GRANT_READ_URI_PERMISSION, FLAG_GRANT_WRITE_URI_PERMISSION })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AccessUriMode {}
    public static boolean isAccessUriMode(int modeFlags) {
        return false;
    }
    @IntDef(flag = true, value = {
            FLAG_GRANT_READ_URI_PERMISSION,
            FLAG_GRANT_WRITE_URI_PERMISSION,
            FLAG_FROM_BACKGROUND,
            FLAG_DEBUG_LOG_RESOLUTION,
            FLAG_EXCLUDE_STOPPED_PACKAGES,
            FLAG_INCLUDE_STOPPED_PACKAGES,
            FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
            FLAG_GRANT_PREFIX_URI_PERMISSION,
            FLAG_DEBUG_TRIAGED_MISSING,
            FLAG_IGNORE_EPHEMERAL,
            FLAG_ACTIVITY_MATCH_EXTERNAL,
            FLAG_ACTIVITY_NO_HISTORY,
            FLAG_ACTIVITY_SINGLE_TOP,
            FLAG_ACTIVITY_NEW_TASK,
            FLAG_ACTIVITY_MULTIPLE_TASK,
            FLAG_ACTIVITY_CLEAR_TOP,
            FLAG_ACTIVITY_FORWARD_RESULT,
            FLAG_ACTIVITY_PREVIOUS_IS_TOP,
            FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
            FLAG_ACTIVITY_BROUGHT_TO_FRONT,
            FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
            FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY,
            FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET,
            FLAG_ACTIVITY_NEW_DOCUMENT,
            FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET,
            FLAG_ACTIVITY_NO_USER_ACTION,
            FLAG_ACTIVITY_REORDER_TO_FRONT,
            FLAG_ACTIVITY_NO_ANIMATION,
            FLAG_ACTIVITY_CLEAR_TASK,
            FLAG_ACTIVITY_TASK_ON_HOME,
            FLAG_ACTIVITY_RETAIN_IN_RECENTS,
            FLAG_ACTIVITY_LAUNCH_ADJACENT,
            FLAG_ACTIVITY_REQUIRE_NON_BROWSER,
            FLAG_ACTIVITY_REQUIRE_DEFAULT,
            FLAG_RECEIVER_REGISTERED_ONLY,
            FLAG_RECEIVER_REPLACE_PENDING,
            FLAG_RECEIVER_FOREGROUND,
            FLAG_RECEIVER_NO_ABORT,
            FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT,
            FLAG_RECEIVER_BOOT_UPGRADE,
            FLAG_RECEIVER_INCLUDE_BACKGROUND,
            FLAG_RECEIVER_EXCLUDE_BACKGROUND,
            FLAG_RECEIVER_FROM_SHELL,
            FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS,
            FLAG_RECEIVER_OFFLOAD,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Flags {}
    @IntDef(flag = true, value = {
            FLAG_FROM_BACKGROUND,
            FLAG_DEBUG_LOG_RESOLUTION,
            FLAG_EXCLUDE_STOPPED_PACKAGES,
            FLAG_INCLUDE_STOPPED_PACKAGES,
            FLAG_DEBUG_TRIAGED_MISSING,
            FLAG_IGNORE_EPHEMERAL,
            FLAG_ACTIVITY_MATCH_EXTERNAL,
            FLAG_ACTIVITY_NO_HISTORY,
            FLAG_ACTIVITY_SINGLE_TOP,
            FLAG_ACTIVITY_NEW_TASK,
            FLAG_ACTIVITY_MULTIPLE_TASK,
            FLAG_ACTIVITY_CLEAR_TOP,
            FLAG_ACTIVITY_FORWARD_RESULT,
            FLAG_ACTIVITY_PREVIOUS_IS_TOP,
            FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
            FLAG_ACTIVITY_BROUGHT_TO_FRONT,
            FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
            FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY,
            FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET,
            FLAG_ACTIVITY_NEW_DOCUMENT,
            FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET,
            FLAG_ACTIVITY_NO_USER_ACTION,
            FLAG_ACTIVITY_REORDER_TO_FRONT,
            FLAG_ACTIVITY_NO_ANIMATION,
            FLAG_ACTIVITY_CLEAR_TASK,
            FLAG_ACTIVITY_TASK_ON_HOME,
            FLAG_ACTIVITY_RETAIN_IN_RECENTS,
            FLAG_ACTIVITY_LAUNCH_ADJACENT,
            FLAG_RECEIVER_REGISTERED_ONLY,
            FLAG_RECEIVER_REPLACE_PENDING,
            FLAG_RECEIVER_FOREGROUND,
            FLAG_RECEIVER_NO_ABORT,
            FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT,
            FLAG_RECEIVER_BOOT_UPGRADE,
            FLAG_RECEIVER_INCLUDE_BACKGROUND,
            FLAG_RECEIVER_EXCLUDE_BACKGROUND,
            FLAG_RECEIVER_FROM_SHELL,
            FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS,
            FLAG_RECEIVER_OFFLOAD,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MutableFlags {}
    public static final int FLAG_GRANT_READ_URI_PERMISSION = 0x00000001;
    public static final int FLAG_GRANT_WRITE_URI_PERMISSION = 0x00000002;
    public static final int FLAG_FROM_BACKGROUND = 0x00000004;
    public static final int FLAG_DEBUG_LOG_RESOLUTION = 0x00000008;
    public static final int FLAG_EXCLUDE_STOPPED_PACKAGES = 0x00000010;
    public static final int FLAG_INCLUDE_STOPPED_PACKAGES = 0x00000020;
    public static final int FLAG_GRANT_PERSISTABLE_URI_PERMISSION = 0x00000040;
    public static final int FLAG_GRANT_PREFIX_URI_PERMISSION = 0x00000080;
    public static final int FLAG_DIRECT_BOOT_AUTO = 0x00000100;
    @Deprecated
    public static final int FLAG_DEBUG_TRIAGED_MISSING = FLAG_DIRECT_BOOT_AUTO;
    public static final int FLAG_IGNORE_EPHEMERAL = 0x00000200;
    public static final int FLAG_ACTIVITY_NO_HISTORY = 0x40000000;
    public static final int FLAG_ACTIVITY_SINGLE_TOP = 0x20000000;
    public static final int FLAG_ACTIVITY_NEW_TASK = 0x10000000;
    public static final int FLAG_ACTIVITY_MULTIPLE_TASK = 0x08000000;
    public static final int FLAG_ACTIVITY_CLEAR_TOP = 0x04000000;
    public static final int FLAG_ACTIVITY_FORWARD_RESULT = 0x02000000;
    public static final int FLAG_ACTIVITY_PREVIOUS_IS_TOP = 0x01000000;
    public static final int FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS = 0x00800000;
    public static final int FLAG_ACTIVITY_BROUGHT_TO_FRONT = 0x00400000;
    public static final int FLAG_ACTIVITY_RESET_TASK_IF_NEEDED = 0x00200000;
    public static final int FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY = 0x00100000;
    @Deprecated
    public static final int FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET = 0x00080000;
    public static final int FLAG_ACTIVITY_NEW_DOCUMENT = FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET;
    public static final int FLAG_ACTIVITY_NO_USER_ACTION = 0x00040000;
    public static final int FLAG_ACTIVITY_REORDER_TO_FRONT = 0X00020000;
    public static final int FLAG_ACTIVITY_NO_ANIMATION = 0X00010000;
    public static final int FLAG_ACTIVITY_CLEAR_TASK = 0X00008000;
    public static final int FLAG_ACTIVITY_TASK_ON_HOME = 0X00004000;
    public static final int FLAG_ACTIVITY_RETAIN_IN_RECENTS = 0x00002000;
    public static final int FLAG_ACTIVITY_LAUNCH_ADJACENT = 0x00001000;
    public static final int FLAG_ACTIVITY_MATCH_EXTERNAL = 0x00000800;
    public static final int FLAG_ACTIVITY_REQUIRE_NON_BROWSER = 0x00000400;
    public static final int FLAG_ACTIVITY_REQUIRE_DEFAULT = 0x00000200;
    public static final int FLAG_RECEIVER_REGISTERED_ONLY = 0x40000000;
    public static final int FLAG_RECEIVER_REPLACE_PENDING = 0x20000000;
    public static final int FLAG_RECEIVER_FOREGROUND = 0x10000000;
    public static final int FLAG_RECEIVER_OFFLOAD = 0x80000000;
    public static final int FLAG_RECEIVER_NO_ABORT = 0x08000000;
    public static final int FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT = 0x04000000;
    public static final int FLAG_RECEIVER_BOOT_UPGRADE = 0x02000000;
    public static final int FLAG_RECEIVER_INCLUDE_BACKGROUND = 0x01000000;
    public static final int FLAG_RECEIVER_EXCLUDE_BACKGROUND = 0x00800000;
    public static final int FLAG_RECEIVER_FROM_SHELL = 0x00400000;
    public static final int FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS = 0x00200000;
    public static final int IMMUTABLE_FLAGS = FLAG_GRANT_READ_URI_PERMISSION
            | FLAG_GRANT_WRITE_URI_PERMISSION | FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            | FLAG_GRANT_PREFIX_URI_PERMISSION;
    private static final int LOCAL_FLAG_FROM_COPY = 1 << 0;
    private static final int LOCAL_FLAG_FROM_PARCEL = 1 << 1;
    private static final int LOCAL_FLAG_FROM_PROTECTED_COMPONENT = 1 << 2;
    private static final int LOCAL_FLAG_UNFILTERED_EXTRAS = 1 << 3;
    private static final int LOCAL_FLAG_FROM_URI = 1 << 4;
    @IntDef(flag = true, value = {
            URI_ALLOW_UNSAFE,
            URI_ANDROID_APP_SCHEME,
            URI_INTENT_SCHEME,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface UriFlags {}
    public static final int URI_INTENT_SCHEME = 1<<0;
    public static final int URI_ANDROID_APP_SCHEME = 1<<1;
    public static final int URI_ALLOW_UNSAFE = 1<<2;

    private static final int COPY_MODE_ALL = 0;
    private static final int COPY_MODE_FILTER = 1;
    private static final int COPY_MODE_HISTORY = 2;
    @IntDef(value = {
            COPY_MODE_ALL,
            COPY_MODE_FILTER,
            COPY_MODE_HISTORY
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface CopyMode {}
    public Intent() {
    }
    public Intent(Intent o) {
        this(o, COPY_MODE_ALL);
    }
    private Intent(Intent o, @CopyMode int copyMode) {
    }
    @Override
    public Object clone() {
        return null;
    }
    public Intent cloneFilter() {
        return null;
    }
    public Intent(String action) {
        setAction(action);
    }
    public Intent(String action, Uri uri) {
    }
    public Intent(Context packageContext, Class<?> cls) {
        
    }
    public Intent(String action, Uri uri,
            Context packageContext, Class<?> cls) {
    }
    public static Intent makeMainActivity(ComponentName mainActivity) {
        return null;
    }
    public static Intent makeMainSelectorActivity(String selectorAction,
            String selectorCategory) {
        return null;
    }
    public static Intent makeRestartActivityTask(ComponentName mainActivity) {
        return null;
    }
    @Deprecated
    public static Intent getIntent(String uri) throws URISyntaxException {
        return null;
    }
    public static Intent parseUri(String uri, @UriFlags int flags) throws URISyntaxException {
        return null;
    }
    private static Intent parseUriInternal(String uri, @UriFlags int flags)
            throws URISyntaxException {
        return null;
    }
    public static Intent getIntentOld(String uri) throws URISyntaxException {
        return null;
    }
    private static Intent getIntentOld(String uri, int flags) throws URISyntaxException {
        return null;
    }
    public interface CommandOptionHandler {
    }
    public static void printIntentArgsHelp(PrintWriter pw, String prefix) {
        
    }
    public String getAction() {
        return null;
    }
    public Uri getData() {
        return null;
    }
    public String getDataString() {
        return null;
    }
    public String getScheme() {
        return null;
    }
    public String getType() {
        return null;
    }
    public String resolveType(Context context) {
        return null;
    }
    public String resolveType(ContentResolver resolver) {
        return null;
    }
    public Set<String> getCategories() {
        return null;
    }
    @Deprecated
    public Object getExtra(String name) {
        return null;
    }
    public boolean getBooleanExtra(String name, boolean defaultValue) {
        return false;
    }
    public byte getByteExtra(String name, byte defaultValue) {
        return 0;
    }
    public short getShortExtra(String name, short defaultValue) {
        return 0;
    }
    public char getCharExtra(String name, char defaultValue) {
        return 0;
    }
    public int getIntExtra(String name, int defaultValue) {
        return 0;
    }
    public long getLongExtra(String name, long defaultValue) {
        return 0;
    }
    public float getFloatExtra(String name, float defaultValue) {
        return 0;
    }
    public double getDoubleExtra(String name, double defaultValue) {
        return 0;
    }
    public String getStringExtra(String name) {
        return null;
    }
    public CharSequence getCharSequenceExtra(String name) {
        return null;
    }
    public <T extends Parcelable> T getParcelableExtra(String name) {
        return null;
    }
    public Parcelable[] getParcelableArrayExtra(String name) {
        return null;
    }
    public <T extends Parcelable> ArrayList<T> getParcelableArrayListExtra(String name) {
        return null;
    }
    public Serializable getSerializableExtra(String name) {
        return null;
    }
    public ArrayList<Integer> getIntegerArrayListExtra(String name) {
        return null;
    }
    public ArrayList<String> getStringArrayListExtra(String name) {
        return null;
    }
    public ArrayList<CharSequence> getCharSequenceArrayListExtra(String name) {
        return null;
    }
    public boolean[] getBooleanArrayExtra(String name) {
        return null;
    }
    public byte[] getByteArrayExtra(String name) {
        return null;
    }
    public short[] getShortArrayExtra(String name) {
        return null;
    }
    public char[] getCharArrayExtra(String name) {
        return null;
    }
    public int[] getIntArrayExtra(String name) {
        return null;
    }
    public long[] getLongArrayExtra(String name) {
        return null;
    }
    public float[] getFloatArrayExtra(String name) {
        return null;
    }
    public double[] getDoubleArrayExtra(String name) {
        return null;
    }
    public String[] getStringArrayExtra(String name) {
        return null;
    }
    public CharSequence[] getCharSequenceArrayExtra(String name) {
        return null;
    }
    public Bundle getBundleExtra(String name) {
        return null;
    }
    @Deprecated
    public IBinder getIBinderExtra(String name) {
        return null;
    }
    @Deprecated
    public Object getExtra(String name, Object defaultValue) {
        Object result = defaultValue;
        if (mExtras != null) {
            Object result2 = mExtras.get(name);
            if (result2 != null) {
                result = result2;
            }
        }
        return null;
    }
    public Bundle getExtras() {
        return null;
    }
    public void removeUnsafeExtras() {
    }
    public boolean canStripForHistory() {
        return false;
    }
    public Intent maybeStripForHistory() {
        // TODO Scan and remove possibly heavy instances like Bitmaps from unparcelled extras?
        if (!canStripForHistory()) {
            return null;
        }
        return null;
    }
    public @Flags int getFlags() {
        return 0;
    }
    public boolean isExcludingStopped() {
        return false;
    }
    public String getPackage() {
        return null;
    }
    public ComponentName getComponent() {
        return null;
    }
    public Rect getSourceBounds() {
        return null;
    }
    public ComponentName resolveActivity(PackageManager pm) {
        return null;
    }
    public ActivityInfo resolveActivityInfo(PackageManager pm,
            int flags) {
        return null;
    }
    public ComponentName resolveSystemService(PackageManager pm,
            int flags) {
        return null;
    }
    public Intent setAction(String action) {
        return null;
    }
    public Intent setData(Uri data) {
        return null;
    }
    public Intent setDataAndNormalize(Uri data) {
        return null;
    }
    public Intent setType(String type) {
        return null;
    }
    public Intent setTypeAndNormalize(String type) {
        return null;
    }
    public Intent setDataAndType(Uri data, String type) {
        return null;
    }
    public Intent setDataAndTypeAndNormalize(Uri data, String type) {
        return null;
    }
    public Intent setIdentifier(String identifier) {
        return null;
    }
    public Intent addCategory(String category) {
        return null;
    }
    public void removeCategory(String category) {

    }
    public void setSelector(Intent selector) {

    }
    public void setClipData(ClipData clip) {

    }
    public void prepareToLeaveUser(int userId) {
    }
    private Bundle mExtras = null;
    public Intent putExtra(String name, boolean value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putBoolean(name, value);
        return null;
    }
    public Intent putExtra(String name, byte value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putByte(name, value);
        return null;
    }
    public Intent putExtra(String name, char value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putChar(name, value);
        return null;
    }
    public Intent putExtra(String name, short value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putShort(name, value);
        return null;
    }
    public Intent putExtraInt(String name, int value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putInt(name, value);
        return null;
    }
    public Intent putExtra(String name, int value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putInt(name, value);
        return null;
    }
    public Intent putExtra(String name, long value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putLong(name, value);
        return null;
    }
    public Intent putExtra(String name, float value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putFloat(name, value);
        return null;
    }
    public Intent putExtra(String name, double value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putDouble(name, value);
        return null;
    }
    public Intent putExtra(String name, String value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putString(name, value);
        return null;
    }
    public Intent putExtra(String name, CharSequence value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putCharSequence(name, value);
        return null;
    }
    public Intent putExtra(String name, Parcelable value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putParcelable(name, value);
        return null;
    }
    public Intent putExtra(String name, Parcelable[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putParcelableArray(name, value);
        return null;
    }
    public Intent putParcelableArrayListExtra(String name,
            ArrayList<? extends Parcelable> value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putParcelableArrayList(name, value);
        return null;
    }
    public Intent putIntegerArrayListExtra(String name,
            ArrayList<Integer> value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putIntegerArrayList(name, value);
        return null;
    }
    public Intent putStringArrayListExtra(String name, ArrayList<String> value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putStringArrayList(name, value);
        return null;
    }
    public Intent putCharSequenceArrayListExtra(String name,
            ArrayList<CharSequence> value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putCharSequenceArrayList(name, value);
        return null;
    }
    public Intent putExtra(String name, Serializable value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putSerializable(name, value);
        return null;
    }
    public Intent putExtra(String name, boolean[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putBooleanArray(name, value);
        return null;
    }
    public Intent putExtra(String name, byte[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putByteArray(name, value);
        return null;
    }
    public Intent putExtra(String name, short[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putShortArray(name, value);
        return null;
    }
    public Intent putExtra(String name, char[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putCharArray(name, value);
        return null;
    }
    public Intent putExtra(String name, int[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putIntArray(name, value);
        return null;
    }
    public Intent putExtra(String name, long[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putLongArray(name, value);
        return null;
    }
    public Intent putExtra(String name, float[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putFloatArray(name, value);
        return null;
    }
    public Intent putExtra(String name, double[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putDoubleArray(name, value);
        return null;
    }
    public Intent putExtra(String name, String[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putStringArray(name, value);
        return null;
    }
    public Intent putExtra(String name, CharSequence[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putCharSequenceArray(name, value);
        return null;
    }
    public Intent putExtra(String name, Bundle value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putBundle(name, value);
        return null;
    }
    @Deprecated
    public Intent putExtra(String name, IBinder value) {
        return null;
    }
    public Intent putExtras(Intent src) {
        return null;
    }
    public Intent putExtras(Bundle extras) {
        return null;
    }
    public Intent replaceExtras(Intent src) {
        return null;
    }
    public Intent replaceExtras(Bundle extras) {
        mExtras = extras != null ? new Bundle(extras) : null;
        return null;
    }
    public void removeExtra(String name) {
        if (mExtras != null) {
            mExtras.remove(name);
            if (mExtras.size() == 0) {
                mExtras = null;
            }
        }
    }
    public Intent setFlags(@Flags int flags) {
        return null;
    }
    public Intent addFlags(@Flags int flags) {
        return null;
    }
    public void removeFlags(@Flags int flags) {

    }
    public Intent setPackage(String packageName) {
        return null;
    }
    public Intent setComponent(ComponentName component) {
        return null;
    }
    public Intent setClassName(Context packageContext,
            String className) {
        return null;
    }
    public Intent setClassName(String packageName, String className) {
        return null;
    }
    public Intent setClass(Context packageContext, Class<?> cls) {
        return null;
    }
    public void setSourceBounds(Rect r) {

    }
    @IntDef(flag = true, value = {
            FILL_IN_ACTION,
            FILL_IN_DATA,
            FILL_IN_CATEGORIES,
            FILL_IN_COMPONENT,
            FILL_IN_PACKAGE,
            FILL_IN_SOURCE_BOUNDS,
            FILL_IN_SELECTOR,
            FILL_IN_CLIP_DATA
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FillInFlags {}
    public static final int FILL_IN_ACTION = 1<<0;
    public static final int FILL_IN_DATA = 1<<1;
    public static final int FILL_IN_CATEGORIES = 1<<2;
    public static final int FILL_IN_COMPONENT = 1<<3;
    public static final int FILL_IN_PACKAGE = 1<<4;
    public static final int FILL_IN_SOURCE_BOUNDS = 1<<5;
    public static final int FILL_IN_SELECTOR = 1<<6;
    public static final int FILL_IN_CLIP_DATA = 1<<7;
    public static final int FILL_IN_IDENTIFIER = 1<<8;
    @FillInFlags
    public int fillIn(Intent other, @FillInFlags int flags) {
        return 0;
    }
    public static final class FilterComparison {
        public FilterComparison(Intent intent) {
        }
        public Intent getIntent() {
            return null;
        }
        @Override
        public boolean equals(Object obj) {
            return false;
        }
        @Override
        public int hashCode() {
            return 0;
        }
    }
    public boolean filterEquals(Intent other) {
        return false;
    }
    private boolean hasPackageEquivalentComponent() {
        return false;
    }
    public int filterHashCode() {
        return 0;
    }
    @Override
    public String toString() {
        return null;
    }
    public String toInsecureString() {
        return null;
    }
    public String toShortString(boolean secure, boolean comp, boolean extras, boolean clip) {
        return null;
    }
    public void toShortString(StringBuilder b, boolean secure, boolean comp, boolean extras,
            boolean clip) {
    }
    public void dumpDebug(ProtoOutputStream proto, long fieldId, boolean secure, boolean comp,
            boolean extras, boolean clip) {

    }
    private void dumpDebugWithoutFieldId(ProtoOutputStream proto, boolean secure, boolean comp,
            boolean extras, boolean clip) {}
    @Deprecated
    public String toURI() {
        return null;
    }
    public String toUri(@UriFlags int flags) {
        return null;
    }
    private void toUriFragment(StringBuilder uri, String scheme, String defAction,
            String defPackage, int flags) {

    }
    private void toUriInner(StringBuilder uri, String scheme, String defAction,
            String defPackage, int flags) {

    }
    public int describeContents() {
        return 0;
    }
    public void writeToParcel(Parcel out, int flags) {}
    public static final Creator<Intent> CREATOR
            = new Creator<Intent>() {
        public Intent createFromParcel(Parcel in) {
            return null;
        }
        public Intent[] newArray(int size) {
            return null;
        }
    };
    protected Intent(Parcel in) {
    }
    public void readFromParcel(Parcel in) {}
    public static Intent parseIntent(Resources resources,
            XmlPullParser parser, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        return null;
    }
    public void saveToXml(XmlSerializer out) throws IOException {

    }
    public static Intent restoreFromXml(XmlPullParser in) throws IOException,
            XmlPullParserException {
        return null;
    }
    public static String normalizeMimeType(String type) {
        return null;
    }
    public void prepareToLeaveProcess(Context context) {

    }
    public void prepareToLeaveProcess(boolean leavingPackage) {}
    public void prepareToEnterProcess(boolean fromProtectedComponent, AttributionSource source) {}
    public boolean hasWebURI() {
        return false;
    }
    public boolean isWebIntent() {
        return false;
    }
    private boolean isImageCaptureIntent() {
        return false;
    }
    public boolean isImplicitImageCaptureIntent() {
        return false;
    }
     public void fixUris(int contentUserHint) {
     }
    public boolean migrateExtraStreamToClipData() {
        return false;
    }
    public boolean migrateExtraStreamToClipData(Context context) {
        return false;
    }
    private Uri maybeConvertFileToContentUri(Context context, Uri uri) {
        return null;
    }
    public static String dockStateToString(int dock) {
        return null;
    }
    private static ClipData.Item makeClipItem(ArrayList<Uri> streams, ArrayList<CharSequence> texts,
            ArrayList<String> htmlTexts, int which) {
        return null;
    }
    public boolean isDocument() {
        return false;
    }
}
