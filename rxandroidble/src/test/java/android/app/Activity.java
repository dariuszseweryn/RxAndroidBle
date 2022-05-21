package android.app;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.MemorySharedPreferences;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.view.Display;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Stubs, only used for mocks
 */
@SuppressWarnings("deprecation")
public class Activity {
    public static final int RESULT_CANCELED    = 0;
    public static final int RESULT_OK           = -1;
    public static final int RESULT_FIRST_USER   = 1;
    public Activity() {
    }
    public Activity(Resources resources) {
    }
    public void requestPermissions(String[] permissions, int requestCode) {
    }
    public void startActivityForResult(Intent intent, int requestCode) {
    }
    public void startActivityForResult(Intent intent, int requestCode,
                                       Bundle options) {
    }
    public AssetManager getAssets() {
        return null;
    }
    public Resources getResources() {
        return null;
    }
    public PackageManager getPackageManager() {
        return null;
    }
    public ContentResolver getContentResolver() {
        return null;
    }
    public Looper getMainLooper() {
        return null;
    }
    public Context getApplicationContext() {
        return null;
    }
    public void setTheme(int resid) {
    }
    public Resources.Theme getTheme() {
        return null;
    }
    public ClassLoader getClassLoader() {
        return null;
    }
    public String getPackageName() {
        return null;
    }
    public ApplicationInfo getApplicationInfo() {
        return null;
    }
    public String getPackageResourcePath() {
        return null;
    }
    public String getPackageCodePath() {
        return null;
    }
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return new MemorySharedPreferences();
    }
    public boolean moveSharedPreferencesFrom(Context sourceContext, String name) {
        return false;
    }
    public boolean deleteSharedPreferences(String name) {
        return false;
    }
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        return null;
    }
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        return null;
    }
    public boolean deleteFile(String name) {
        return false;
    }
    public File getFileStreamPath(String name) {
        return null;
    }
    public File getDataDir() {
        return null;
    }
    public File getFilesDir() {
        return null;
    }
    public File getNoBackupFilesDir() {
        return null;
    }
    public File getExternalFilesDir(String type) {
        return null;
    }
    public File[] getExternalFilesDirs(String type) {
        return new File[0];
    }
    public File getObbDir() {
        return null;
    }
    public File[] getObbDirs() {
        return new File[0];
    }
    public File getCacheDir() {
        return null;
    }
    public File getCodeCacheDir() {
        return null;
    }
    public File getExternalCacheDir() {
        return null;
    }
    public File[] getExternalCacheDirs() {
        return new File[0];
    }
    public File[] getExternalMediaDirs() {
        return new File[0];
    }
    public String[] fileList() {
        return new String[0];
    }
    public File getDir(String name, int mode) {
        return null;
    }
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        return null;
    }
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
        return null;
    }
    public boolean moveDatabaseFrom(Context sourceContext, String name) {
        return false;
    }
    public boolean deleteDatabase(String name) {
        return false;
    }
    public File getDatabasePath(String name) {
        return null;
    }
    public String[] databaseList() {
        return new String[0];
    }
    public Drawable getWallpaper() {
        return null;
    }
    public Drawable peekWallpaper() {
        return null;
    }
    public int getWallpaperDesiredMinimumWidth() {
        return 0;
    }
    public int getWallpaperDesiredMinimumHeight() {
        return 0;
    }
    public void setWallpaper(Bitmap bitmap) throws IOException {
    }
    public void setWallpaper(InputStream data) throws IOException {
    }
    public void clearWallpaper() throws IOException {
    }
    public void startActivity(Intent intent) {
    }
    public void startActivity(Intent intent, Bundle options) {
    }
    public void startActivities(Intent[] intents) {
    }
    public void startActivities(Intent[] intents, Bundle options) {
    }
    public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws IntentSender.SendIntentException {
    }
    public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) throws IntentSender.SendIntentException {
    }
    public void sendBroadcast(Intent intent) {
    }
    public void sendBroadcast(Intent intent, String receiverPermission) {
    }
    public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
    }
    public void sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
    }
    public void sendBroadcastAsUser(Intent intent, UserHandle user) {
    }
    public void sendBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission) {
    }
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission,
                                           BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
    }
    public void sendStickyBroadcast(Intent intent) {
    }
    public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode,
                                           String initialData, Bundle initialExtras) {
    }
    public void removeStickyBroadcast(Intent intent) {
    }
    public void sendStickyBroadcastAsUser(Intent intent, UserHandle user) {
    }
    public void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle user, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
    }
    public void removeStickyBroadcastAsUser(Intent intent, UserHandle user) {
    }
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return null;
    }
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, int flags) {
        return null;
    }
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission,
                                   Handler scheduler) {
        return null;
    }
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission,
                                   Handler scheduler, int flags) {
        return null;
    }
    public void unregisterReceiver(BroadcastReceiver receiver) {
    }
    public ComponentName startService(Intent service) {
        return null;
    }
    public ComponentName startForegroundService(Intent service) {
        return null;
    }
    public boolean stopService(Intent service) {
        return false;
    }
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        return false;
    }
    public void unbindService(ServiceConnection conn) {
    }
    public boolean startInstrumentation(ComponentName className, String profileFile, Bundle arguments) {
        return false;
    }
    public Object getSystemService(String name) {
        return null;
    }
    public String getSystemServiceName(Class<?> serviceClass) {
        return null;
    }
    public int checkPermission(String permission, int pid, int uid) {
        return 0;
    }
    public int checkCallingPermission(String permission) {
        return 0;
    }
    public int checkCallingOrSelfPermission(String permission) {
        return 0;
    }
    public int checkSelfPermission(String permission) {
        return 0;
    }
    public void enforcePermission(String permission, int pid, int uid, String message) {
    }
    public void enforceCallingPermission(String permission, String message) {
    }
    public void enforceCallingOrSelfPermission(String permission, String message) {
    }
    public void grantUriPermission(String toPackage, Uri uri, int modeFlags) {
    }
    public void revokeUriPermission(Uri uri, int modeFlags) {
    }
    public void revokeUriPermission(String toPackage, Uri uri, int modeFlags) {
    }
    public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
        return 0;
    }
    public int checkCallingUriPermission(Uri uri, int modeFlags) {
        return 0;
    }
    public int checkCallingOrSelfUriPermission(Uri uri, int modeFlags) {
        return 0;
    }
    public int checkUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid,
                                  int modeFlags) {
        return 0;
    }
    public void enforceUriPermission(Uri uri, int pid, int uid, int modeFlags, String message) {
    }
    public void enforceCallingUriPermission(Uri uri, int modeFlags, String message) {
    }
    public void enforceCallingOrSelfUriPermission(Uri uri, int modeFlags, String message) {
    }
    public void enforceUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid, int modeFlags, String message) {
    }
    public Context createPackageContext(String packageName, int flags) throws PackageManager.NameNotFoundException {
        return null;
    }
    public Context createContextForSplit(String splitName) throws PackageManager.NameNotFoundException {
        return null;
    }
    public Context createConfigurationContext(Configuration overrideConfiguration) {
        return null;
    }
    public Context createDisplayContext(Display display) {
        return null;
    }
    public Context createDeviceProtectedStorageContext() {
        return null;
    }
    public boolean isDeviceProtectedStorage() {
        return false;
    }
}
