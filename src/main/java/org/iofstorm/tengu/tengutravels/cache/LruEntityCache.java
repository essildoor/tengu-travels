package org.iofstorm.tengu.tengutravels.cache;

import org.iofstorm.tengu.tengutravels.model.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LruEntityCache<T extends Entity> {
    private static final Logger log = LoggerFactory.getLogger(LruEntityCache.class);
    private static final int CACHE_SIZE = 1000;

    private final Class<T> type;
    private final ConcurrentMap<Integer, CacheEntry<T>> storage;
    private final NavigableMap<Long, Integer> accessMap;
    private final ExecutorService janitor;

    public LruEntityCache(Class<T> type) {
        this.type = Objects.requireNonNull(type, "type");
        storage = new ConcurrentHashMap<>();
        accessMap = new TreeMap<>();
        janitor = Executors.newSingleThreadExecutor();
    }

    public Optional<T> get(Integer id) {
        if (log.isDebugEnabled()) {
            log.debug("getting from cache [class={}, id={}]", type.getSimpleName(), id);
        }
        CacheEntry<T> cacheEntry = storage.get(id);
        T result = null;
        if (cacheEntry != null) {
            result = cacheEntry.getValue();
            janitor.submit(MaintenanceTask.newAccessTask(accessMap, id, cacheEntry));
            if (log.isDebugEnabled()) {
                log.debug("cache hit [class={}, entity={}]", type.getSimpleName(), result);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("cache miss [class={}, id={}]", type.getSimpleName(), id);
            }
        }
        return Optional.ofNullable(result);
    }

    public void put(Integer key, T entity) {
        if (log.isDebugEnabled()) {
            log.debug("putting to cache [class={}, key={}, entity={}]", type.getSimpleName(), key, entity);
        }

        checkAndInvalidate();

        CacheEntry<T> cacheEntry = new CacheEntry<>(Objects.requireNonNull(entity, "entity"));
        storage.put(key, cacheEntry);
        janitor.submit(MaintenanceTask.newAddTask(accessMap, key, cacheEntry));
    }

    public void remove(Integer key) {
        if (log.isDebugEnabled()) {
            log.debug("removing from cache [class={}, key={}]", type.getSimpleName(), key);
        }
        CacheEntry<T> cacheEntry = storage.remove(key);
        janitor.submit(MaintenanceTask.newRemoveTask(accessMap, cacheEntry));
    }

    private void checkAndInvalidate() {
        if (storage.size() > CACHE_SIZE) {
            if (log.isDebugEnabled()) {
                log.debug("invalidating cache");
            }
            janitor.submit(MaintenanceTask.newInvalidateTask(storage, accessMap));
        }
    }
}
