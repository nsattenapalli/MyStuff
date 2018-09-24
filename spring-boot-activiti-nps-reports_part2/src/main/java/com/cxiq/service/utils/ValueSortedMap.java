package com.cxiq.service.utils;

import java.util.*;

/**
 * A map where {@link #keySet()} and {@link #entrySet()} return sets ordered
 * by associated values based on the the comparator provided at construction
 * time. The order of two or more keys with identical values is not defined.
 * <p>
 * Several contracts of the Map interface are not satisfied by this minimal
 * implementation.
 */
public class ValueSortedMap<K, V> extends HashMap<K, V> {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected Map<V, Collection<K>> valueToKeysMap;

    // uses natural order of value object, if any
    public ValueSortedMap() {
        this((Comparator<? super V>) null);
    }

    public ValueSortedMap(Comparator<? super V> valueComparator) {
        this.valueToKeysMap = new TreeMap<V, Collection<K>>(valueComparator);
    }

    public boolean containsValue(Object o) {
        return valueToKeysMap.containsKey(o);
    }

    public V put(K k, V v) {
        V oldV = null;
        if (containsKey(k)) {
            oldV = get(k);
            valueToKeysMap.get(oldV).remove(k);
        }
        super.put(k, v);
        if (!valueToKeysMap.containsKey(v)) {
            Collection<K> keys = new ArrayList<K>();
            keys.add(k);
            valueToKeysMap.put(v, keys);
        } else {
            valueToKeysMap.get(v).add(k);
        }
        return oldV;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            put(e.getKey(), e.getValue());
    }

    public V remove(Object k) {
        V oldV = null;
        if (containsKey(k)) {
            oldV = get(k);
            super.remove(k);
            valueToKeysMap.get(oldV).remove(k);
        }
        return oldV;
    }

    public void clear() {
        super.clear();
        valueToKeysMap.clear();
    }

    public Set<K> keySet() {
        LinkedHashSet<K> ret = new LinkedHashSet<K>(size());
        for (V v : valueToKeysMap.keySet()) {
            Collection<K> keys = valueToKeysMap.get(v);
            ret.addAll(keys);
        }
        return ret;
    }

    public Set<Map.Entry<K, V>> entrySet() {
        LinkedHashSet<Map.Entry<K, V>> ret = new LinkedHashSet<Map.Entry<K, V>>(size());
        for (Collection<K> keys : valueToKeysMap.values()) {
            for (final K k : keys) {
                final V v = get(k);
                ret.add(new Map.Entry<K,V>() {
                    public K getKey() {
                        return k;
                    }

                    public V getValue() {
                        return v;
                    }

                    public V setValue(V v) {
                        throw new UnsupportedOperationException();
                    }
                });
            }
        }
        return ret;
    }
}
