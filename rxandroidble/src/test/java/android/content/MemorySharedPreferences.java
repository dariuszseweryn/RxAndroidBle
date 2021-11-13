package android.content;
import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
/**
 * Must have implementation
 */
public class MemorySharedPreferences implements SharedPreferences{
    private final HashMap<String, Object> map = new HashMap<>();
    public MemorySharedPreferences() {
    }
    private class MemoryEditor implements Editor {
        private Editor put(String key, Object value) {
            map.put(key, value);
            notifyListeners(key);
            return this;
        }
        @Override
        public Editor putString(String key, String value) {
            return put(key, value);
        }
        @Override
        public Editor putStringSet(String key, Set<String> values) {
            return put(key, values);
        }
        @Override
        public Editor putInt(String key, int value) {
            return put(key, value);
        }
        @Override
        public Editor putLong(String key, long value) {
            return put(key, value);
        }
        @Override
        public Editor putFloat(String key, float value) {
            return put(key, value);
        }
        @Override
        public Editor putBoolean(String key, boolean value) {
            return put(key, value);
        }
        @Override
        public Editor remove(String key) {
            map.remove(key);
            notifyListeners(key);
            return this;
        }
        @Override
        public Editor clear() {
            Set<String> keys = map.keySet();
            map.clear();
            for (String key : keys) {
                notifyListeners(key);
            }
            return this;
        }
        @Override
        public boolean commit() {
            return false;
        }
        @Override
        public void apply() {
        }
    }
    private final Editor editor = new MemoryEditor();
    @Override
    public Map<String, ?> getAll() {
        return map;
    }
    @Override
    public String getString(String key, String defValue) {
        Object obj = map.get(key);
        if (obj instanceof String) {
            return (String) obj;
        }
        return defValue;
    }
    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        return defValues;
    }
    @Override
    public int getInt(String key, int defValue) {
        Object obj = map.get(key);
        if (obj instanceof Integer) {
            return (int) obj;
        }
        return defValue;
    }
    @Override
    public long getLong(String key, long defValue) {
        Object obj = map.get(key);
        if (obj instanceof Long) {
            return (long) obj;
        }
        return defValue;
    }
    @Override
    public float getFloat(String key, float defValue) {
        Object obj = map.get(key);
        if (obj instanceof Float) {
            return (float) obj;
        }
        return defValue;
    }
    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Object obj = map.get(key);
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        return defValue;
    }
    @Override
    public boolean contains(String key) {
        return map.containsKey(key);
    }
    public Editor edit() {
        return editor;
    }
    HashSet<OnSharedPreferenceChangeListener> listeners = new HashSet<>();
    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        listeners.add(listener);
    }
    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        listeners.remove(listener);
    }
    private void notifyListeners(String key) {
        for (OnSharedPreferenceChangeListener listener : listeners) {
            listener.onSharedPreferenceChanged(this, key);
        }
    }
}
