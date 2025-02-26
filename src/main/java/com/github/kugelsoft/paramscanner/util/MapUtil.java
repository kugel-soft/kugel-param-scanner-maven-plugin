package com.github.kugelsoft.paramscanner.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapUtil {

    private MapUtil() {
    }

    public static <K, V> Map<K, V> of(K k1, V v1) {
        Map<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        return map;
    }

    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
        Map<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        Map<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return map;
    }

    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        Map<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> of(Object... keysValues) {
        Map<K, V> map = new LinkedHashMap<>();
        if (keysValues.length % 2 == 1) {
            throw new IllegalArgumentException("MapUtil.of não pode ter keysValues com tamanho impar");
        }
        for (int i = 0; i < keysValues.length; i += 2) {
            map.put((K) keysValues[i], (V) keysValues[i + 1]);
        }
        return map;
    }
}
