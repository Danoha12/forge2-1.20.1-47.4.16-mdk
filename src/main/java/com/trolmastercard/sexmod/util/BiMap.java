package com.trolmastercard.sexmod.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * BiMap — Mapa bidireccional (K↔V).
 * Portado a 1.20.1.
 * * Permite buscar un valor por su llave o una llave por su valor en tiempo constante O(1).
 * Útil para rastrear vínculos únicos entre entidades y jugadores.
 */
public class BiMap<K, V> {

    private final HashMap<K, V> forward = new HashMap<>();
    private final HashMap<V, K> inverse = new HashMap<>();

    /** * Asocia la llave con el valor.
     * Reemplaza cualquier mapeo previo en ambas direcciones para mantener la unicidad.
     */
    public void put(K key, V value) {
        // Eliminar rastros previos para evitar inconsistencias
        removeByKey(key);
        removeByValue(value);

        forward.put(key, value);
        inverse.put(value, key);
    }

    /** Retorna el valor mapeado a la llave, o null si no existe. */
    public V getByKey(K key) {
        return forward.get(key);
    }

    /** Retorna la llave mapeada al valor, o null si no existe. */
    public K getByValue(V value) {
        return inverse.get(value);
    }

    /** Retorna el número de entradas en el mapa. */
    public int size() {
        return forward.size();
    }

    /** Elimina la entrada asociada a la llave proporcionada. */
    public void removeByKey(K key) {
        V value = forward.remove(key);
        if (value != null) {
            inverse.remove(value);
        }
    }

    /** Elimina la entrada asociada al valor proporcionado. */
    public void removeByValue(V value) {
        K key = inverse.remove(value);
        if (key != null) {
            forward.remove(key);
        }
    }

    /** Retorna el set de entradas del mapa directo. */
    public Set<Map.Entry<K, V>> entrySet() {
        return forward.entrySet();
    }

    /** Retorna el set de llaves. */
    public Set<K> keySet() {
        return forward.keySet();
    }

    /** Retorna el set de valores. */
    public Set<V> valueSet() {
        return inverse.keySet();
    }

    /** Limpia ambos mapas por completo. */
    public void clear() {
        forward.clear();
        inverse.clear();
    }
}