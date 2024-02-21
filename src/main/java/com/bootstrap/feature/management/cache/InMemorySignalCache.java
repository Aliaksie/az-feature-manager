package com.bootstrap.feature.management.cache;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import com.bootstrap.feature.management.configuration.properties.CacheProperties;

/**
 * Default memory-based {@linkplain Cache} implementation.
 *
 * @param <K> key type parameter
 * @param <V> value type parameter
 */
public class InMemorySignalCache<K, V> extends AbstractMap<K, V> implements InMemoryCache<K, V> {

    private final ReferenceQueue<V> queue;

    private final ItemCache<K, V> itemCache;

    public InMemorySignalCache(CacheProperties cacheProperties) {
        queue = new ReferenceQueue<>();
        itemCache = new ItemCache<>(cacheProperties.inMemory(), queue);
    }

    @Override
    public V get(Object source) {
        return Optional.ofNullable(source)
                .map(itemCache::get)
                .map(ItemReference::get)
                .orElse(null);
    }

    @Override
    public V put(K source, V signal) {
        Assert.notNull(source, "Source parameter should be represented as non null value.");

        return isItemCacheable(signal) ? putItem(source, signal) : null;
    }

    @Override
    public void clear() {
        itemCache.clear();
    }

    @Override
    public Set<K> keySet() {
        return itemCache.keySet();
    }

    @Override
    public Collection<V> values() {
        return itemCache.values()
                .stream()
                .filter(Objects::nonNull)
                .map(ItemReference::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return itemCache.entrySet()
                .stream()
                .filter(this::isReferenceAvailable)
                .map(entry -> new SimpleEntry<>(entry.getKey(), entry.getValue().get()))
                .collect(Collectors.toSet());
    }

    @Override
    public int size() {
        return itemCache.size();
    }

    private V putItem(K source, V signal) {
        ItemReference<K, V> itemReference = new ItemReference<>(signal, queue, source);

        return Optional.ofNullable(itemCache.put(source, itemReference))
                .map(ItemReference::get)
                .orElse(null);
    }

    private boolean isItemCacheable(V item) {
        return Optional.ofNullable(item).isPresent();
    }

    private boolean isReferenceAvailable(Entry<K, ItemReference<K, V>> entry) {
        return Optional.ofNullable(entry)
                .map(Entry::getValue)
                .map(ItemReference::get)
                .isPresent();
    }

    private static class ItemCache<K, V> extends LinkedHashMap<K, ItemReference<K, V>> {

        private static final long serialVersionUID = 1L;

        private final int limit;

        private final transient ReferenceQueue<V> queue;

        ItemCache(CacheProperties.InMemoryCacheProperties cacheProperties, ReferenceQueue<V> queue) {
            super(cacheProperties.min(), cacheProperties.getRatio(), true);
            this.limit = cacheProperties.max();
            this.queue = queue;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, ItemReference<K, V>> eldest) {
            return super.size() > limit;
        }

        @Override
        @SuppressWarnings("unchecked")
        public ItemReference<K, V> get(Object key) {
            for (ItemReference<K, V> ref; (ref = (ItemReference<K, V>) queue.poll()) != null; ) {
                K source = ref.source;
                remove(source);
            }

            return super.get(key);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ItemCache<?, ?> itemCache)) return false;
            if (!super.equals(o)) return false;
            return limit == itemCache.limit && Objects.equals(queue, itemCache.queue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), limit, queue);
        }
    }

    private static class ItemReference<K, V> extends SoftReference<V> {

        private final K source;

        ItemReference(V signal, ReferenceQueue<V> queue, K source) {
            super(signal, queue);
            this.source = source;
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InMemorySignalCache<?, ?> that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(queue, that.queue) && Objects.equals(itemCache, that.itemCache);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), queue, itemCache);
    }
}
