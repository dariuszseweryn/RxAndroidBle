package android.util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
public class SparseArray<E> {
    private final ArrayList<Integer> keys = new ArrayList<>();
    private final HashMap<Integer, E> mHashMap = new HashMap<>();
    public SparseArray() {
    }
    public void put(int key, E value) {
        mHashMap.put(key, value);
        keys.add(key);
        Collections.sort(keys);
    }
    public void append(int key, E value) {
        put(key, value);
    }
    public E get(int key) {
        return mHashMap.get(key);
    }
    public int size() {
        return mHashMap.size();
    }
    public int keyAt(int i) {
        if (i > keys.size()) {
            throw new ArrayIndexOutOfBoundsException(i);
        }
        return keys.get(i);
    }
}