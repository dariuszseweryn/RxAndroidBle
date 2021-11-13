package android.os;
import android.util.ArrayMap;
import android.util.Size;
import android.util.SizeF;
import android.util.SparseArray;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
@SuppressWarnings({"unchecked"})
/**
 * Must have implementation
 */
public final class Bundle implements Cloneable, Parcelable {
    static final int FLAG_HAS_FDS = 1 << 8;
    static final int FLAG_HAS_FDS_KNOWN = 1 << 9;
    static final int FLAG_ALLOW_FDS = 1 << 10;
    public static final Bundle EMPTY;
    private Map<String, Object> mMap = new HashMap<>();
    public static final Bundle STRIPPED;
    static {
        EMPTY = new Bundle();
        EMPTY.mMap = new ArrayMap<>();
        STRIPPED = new Bundle();
        STRIPPED.putInt("STRIPPED", 1);
    }
    public Bundle() {
    }
    public Bundle(Bundle bundle) {
        mMap = bundle.mMap;
    }
    public Object clone() {
        return new Bundle(this);
    }
    public boolean containsKey(String key) {
        return mMap.containsKey(key);
    }
    public void clear() {
        mMap.clear();
    }
    public Set<String> keySet() {
        return mMap.keySet();
    }
    public int size() {
        return mMap.size();
    }
    public void remove(String key) {
        mMap.remove(key);
    }
    public void putAll(Bundle bundle) {
        mMap.putAll(bundle.mMap);
    }
    public int getSize() {
        return size();
    }
    public boolean hasFileDescriptors() {
        return false;
    }
    public void putByte(String key, byte value) {
        mMap.put(key, value);
    }
    public void putBoolean(String key, boolean value) {
        mMap.put(key, value);
    }
    public void putDouble(String key, double value) {
        mMap.put(key, value);
    }
    public void putInt(String key, int value) {
        mMap.put(key, value);
    }
    public void putLong(String key, long value) {
        mMap.put(key, value);
    }
    public void putChar(String key, char value) {
        mMap.put(key, value);
    }
    public void putShort(String key, short value) {
        mMap.put(key, value);
    }
    public void putFloat(String key, float value) {
        mMap.put(key, value);
    }
    public void putString(String key, String value) {
        mMap.put(key, value);
    }
    public void putCharSequence(String key, CharSequence value) {
        mMap.put(key, value);
    }
    public void putParcelable(String key, Parcelable value) {
        mMap.put(key, value);
    }
    public void putSize(String key, Size value) {
        mMap.put(key, value);
    }
    public void putSizeF(String key, SizeF value) {
        mMap.put(key, value);
    }
    public void putParcelableArray(String key, Parcelable[] value) {
        mMap.put(key, value);
    }
    public void putParcelableArrayList(String key,
            ArrayList<? extends Parcelable> value) {
        mMap.put(key, value);
    }
    public void putParcelableList(String key, List<? extends Parcelable> value) {
        mMap.put(key, value);
    }
    public void putSparseParcelableArray(String key,
            SparseArray<? extends Parcelable> value) {
        mMap.put(key, value);
    }
    public void putIntegerArrayList(String key, ArrayList<Integer> value) {
        mMap.put(key, value);
    }
    public void putStringArrayList(String key, ArrayList<String> value) {
        mMap.put(key, value);
    }
    public void putCharSequenceArrayList(String key,
            ArrayList<CharSequence> value) {
        mMap.put(key, value);
    }
    public void putSerializable(String key, Serializable value) {
        mMap.put(key, value);
    }
    public void putByteArray(String key, byte[] value) {
        mMap.put(key, value);
    }
    public void putShortArray(String key, short[] value) {
        mMap.put(key, value);
    }
    public void putCharArray(String key, char[] value) {
        mMap.put(key, value);
    }
    public void putFloatArray(String key, float[] value) {
        mMap.put(key, value);
    }
    public void putIntArray(String key, int[] value) {
        mMap.put(key, value);
    }
    public void putLongArray(String key, long[] value) {
        mMap.put(key, value);
    }
    public void putDoubleArray(String key, double[] value) {
        mMap.put(key, value);
    }
    public void putBooleanArray(String key, boolean[] value) {
        mMap.put(key, value);
    }
    public void putStringArray(String key, String[] value) {
        mMap.put(key, value);
    }
    public void putCharSequenceArray(String key, CharSequence[] value) {
        mMap.put(key, value);
    }
    public void putBundle(String key, Bundle value) {
        mMap.put(key, value);
    }
    public void putBinder(String key, IBinder value) {
        mMap.put(key, value);
    }
    @Deprecated
    public void putIBinder(String key, IBinder value) {
        mMap.put(key, value);
    }
    public byte getByte(String key) {
        return getByte(key, (byte) 0);
    }
    public Byte getByte(String key, byte defaultValue) {
        Object val = mMap.get(key);
        if (val instanceof Byte) {
            return (Byte) val;
        }
        return defaultValue;
    }
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }
    public boolean getBoolean(String key, boolean defaultValue) {
        Object val = mMap.get(key);
        if (val instanceof Boolean) {
            return (boolean) val;
        }
        return defaultValue;
    }
    public char getChar(String key) {
        return getChar(key, (char) 0);
    }
    public char getChar(String key, char defaultValue) {
        Object val = mMap.get(key);
        if (val instanceof Character) {
            return (Character) val;
        }
        return defaultValue;
    }
    public short getShort(String key) {
        return getShort(key, (short) 0);
    }
    public short getShort(String key, short defaultValue) {
        Object val = mMap.get(key);
        if (val instanceof Short) {
            return (Short) val;
        }
        return defaultValue;
    }
    public float getFloat(String key) {
        return getFloat(key, 0);
    }
    public float getFloat(String key, float defaultValue) {
        Object val = mMap.get(key);
        if (val instanceof Float) {
            return (Float) val;
        }
        return defaultValue;
    }
    public double getDouble(String key) {
        return getDouble(key, 0);
    }
    public double getDouble(String key, double defaultValue) {
        Object val = mMap.get(key);
        if (val instanceof Double) {
            return (Double) val;
        }
        return defaultValue;
    }
    public int getInt(String key) {
        return getInt(key, 0);
    }
    public int getInt(String key, int defaultValue) {
        Object val = mMap.get(key);
        if (val instanceof Integer) {
            return (Integer) val;
        }
        return defaultValue;
    }
    public long getLong(String key) {
        return getLong(key, 0);
    }
    public long getLong(String key, long defaultValue) {
        Object val = mMap.get(key);
        if (val instanceof Long) {
            return (Long) val;
        }
        return defaultValue;
    }
    public String getString(String key) {
        return getString(key, "");
    }
    public String getString(String key, String defaultValue) {
        Object val = mMap.get(key);
        if (val instanceof String) {
            return (String) val;
        }
        return defaultValue;
    }
    public CharSequence getCharSequence(String key) {
        return getCharSequence(key, null);
    }
    public CharSequence getCharSequence(String key, CharSequence defaultValue) {
        Object val = mMap.get(key);
        if (val instanceof CharSequence) {
            return (CharSequence) val;
        }
        return defaultValue;
    }
    public Size getSize(String key) {
        final Object o = mMap.get(key);
        try {
            return (Size) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public SizeF getSizeF(String key) {
        final Object o = mMap.get(key);
        try {
            return (SizeF) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public Bundle getBundle(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (Bundle) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public <T extends Parcelable> T getParcelable(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (T) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public Parcelable[] getParcelableArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (Parcelable[]) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public <T extends Parcelable> ArrayList<T> getParcelableArrayList(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (ArrayList<T>) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public <T extends Parcelable> SparseArray<T> getSparseParcelableArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (SparseArray<T>) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public Serializable getSerializable(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (Serializable) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public ArrayList<Integer> getIntegerArrayList(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (ArrayList<Integer>) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public ArrayList<String> getStringArrayList(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (ArrayList<String>) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public ArrayList<CharSequence> getCharSequenceArrayList(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (ArrayList<CharSequence>) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public byte[] getByteArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (byte[]) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public short[] getShortArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (short[]) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public char[] getCharArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (char[]) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public float[] getFloatArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (float[]) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public boolean[] getBooleanArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (boolean[]) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public int[] getIntArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (int[]) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public long[] getLongArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (long[]) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public double[] getDoubleArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (double[]) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public String[] getStringArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (String[]) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public CharSequence[] getCharSequenceArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (CharSequence[]) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public Object get(String key) {
        return mMap.get(key);
    }
    @Deprecated
    public IBinder getIBinder(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (IBinder) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
    public static final Creator<Bundle> CREATOR =
        new Creator<Bundle>() {
        @Override
        public Bundle createFromParcel(Parcel in) {
            return in.readBundle();
        }
        @Override
        public Bundle[] newArray(int size) {
            return new Bundle[size];
        }
    };
    public int describeContents() {
        int mask = 0;
        if (hasFileDescriptors()) {
            mask |= Parcelable.CONTENTS_FILE_DESCRIPTOR;
        }
        return mask;
    }
    public void writeToParcel(Parcel parcel, int flags) {
    }
    public void readFromParcel(Parcel parcel) {
    }
    public synchronized String toString() {
        return "Bundle[" + mMap.toString() + "]";
    }
    public synchronized String toShortString() {
        return mMap.toString();
    }
}
