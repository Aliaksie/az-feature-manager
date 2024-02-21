package com.bootstrap.feature.management.cache;

import java.util.Map;

/**
 * Default memory-based {@linkplain Cache} marker interface.
 *
 * @param <K> key type parameter
 * @param <V> value type parameter
 */
public interface InMemoryCache<K, V> extends Cache<K, V>, Map<K, V> {
}
