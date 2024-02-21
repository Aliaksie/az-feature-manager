package com.bootstrap.feature.management.cache;

import java.util.Optional;

import org.springframework.beans.factory.FactoryBean;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import com.bootstrap.feature.management.configuration.properties.CacheProperties;

/**
 * Cache abstraction intended only for non-concurrent access patterns.
 *
 * @param <K> key type parameter
 * @param <V> value type parameter
 */
public interface Cache<K, V> {

    /**
     * Default scheduler used to handle cache read/write operations.
     */
    Scheduler CACHE_HANDLE_SCHEDULER = Schedulers.newSingle("Cache Handler", true);

    /**
     * Get cache entry by specified key value.
     *
     * @param key cache entry key
     * @return cache entry associated with specified key
     */
    V get(Object key);

    /**
     * Place/cache provided value under specified key.
     *
     * @param key cache entry key
     * @param value cache entry value
     * @return former value associated with current key, or null if there was no mapping for key
     */
    V put(K key, V value);

    /**
     * Remove/release entry by specified key.
     *
     * @param key entry key
     * @return removed value if it was presented
     */
    V remove(Object key);

    /**
     * Purge current instance.
     */
    void clear();

    // todo --->>>:
    /**
     * Factory type responsible for consumer identities cache creation.
     *
     * @param <K> key type parameter
     * @param <V> value type parameter
     */
    class CacheFactory<K, V> implements FactoryBean<Cache<K, V>> {

        private final CacheProperties cacheProperties;

        public CacheFactory(CacheProperties cacheProperties) {
            this.cacheProperties = cacheProperties;
        }

        @Override
        public Cache<K, V> getObject() {
            return Optional.ofNullable(cacheProperties)
                    .filter(CacheProperties::enabled)
                    .<Cache<K, V>>map(InMemorySignalCache::new)
                    .orElse(null);

        }

        // todo <<<---:
        @Override
        public Class<?> getObjectType() {
            return Cache.class;
        }

        @Override
        public boolean isSingleton() {
            return true;
        }

    }

}
