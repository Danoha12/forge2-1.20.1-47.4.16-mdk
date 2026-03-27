package com.trolmastercard.sexmod.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * BiMap - bidirectional HashMap (K-V).
 * Ported from gl.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 * No Minecraft API dependencies - no migration needed.
 */
public class BiMap<K, V> {

    private final HashMap<K, V> forward = new HashMap<>();
    private final HashMap<V, K> inverse = new HashMap<>();

    /** Associates key with value (replaces any previous mapping in both directions). */
    public void put(K key, V value) {
        V old = forward.put(key, value);
        inverse.remove(old);
        inverse.put(value, key);
    }

    /** Returns the value mapped to the given key, or null. */
    public V getByKey(K key) {
        return forward.get(key);
    }

    /** Returns the key mapped to the given value, or null. */
    public K getByValue(V value) {
        return inverse.get(value);
    }

    /** Returns the number of entries. */
    public int size() {
        return forward.size();
    }

    /** Removes the entry with the given key. */
    public void removeByKey(K key) {
        V value = forward.get(key);
        if (value != null) {
            forward.remove(key);
            inverse.remove(value);
        }
    }

    /** Returns the entry set of the forward map. */
    public Set<Map.Entry<K, V>> entrySet() {
        return forward.entrySet();
    }

    /** Returns the key set. */
    public Set<K> keySet() {
        return forward.keySet();
    }

    /** Returns the value set. */
    public Set<V> valueSet() {
        return inverse.keySet();
    }

    /** Clears both maps. */
    public void clear() {
        forward.clear();
        inverse.clear();
    }
}
