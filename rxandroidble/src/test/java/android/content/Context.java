package android.content;

import android.content.res.Resources;

public abstract class Context {

        public abstract String getPackageName();

        public abstract Resources getResources();
        public abstract ContentResolver getContentResolver();
        public abstract Intent registerReceiver(BroadcastReceiver receiver, IntentFilter intentFilter);
        public abstract void unregisterReceiver(BroadcastReceiver receiver);

}
