package org.iofstorm.tengu.tengutravels.cache;

import java.util.NavigableMap;
import java.util.concurrent.ConcurrentMap;

class MaintenanceTask implements Runnable {
    private final ConcurrentMap storage;
    private final NavigableMap<Long, Integer> accessTsToKeyMap;
    private final TaskType type;
    private final Integer entryKey;
    private final CacheEntry cacheEntry;

    private MaintenanceTask(ConcurrentMap storage, NavigableMap<Long, Integer> accessTsToKeyMap,
                            TaskType type, Integer entryKey, CacheEntry<?> cacheEntry) {
        this.storage = storage;
        this.accessTsToKeyMap = accessTsToKeyMap;
        this.type = type;
        this.entryKey = entryKey;
        this.cacheEntry = cacheEntry;
    }

    static MaintenanceTask newAddTask(NavigableMap<Long, Integer> accessTsToKeyMap, Integer entryKey, CacheEntry<?> cacheEntry) {
        return new MaintenanceTask(null, accessTsToKeyMap, TaskType.ADD, entryKey, cacheEntry);
    }

    static MaintenanceTask newAccessTask(NavigableMap<Long, Integer> accessTsToKeyMap, Integer entryKey, CacheEntry<?> cacheEntry) {
        return new MaintenanceTask(null, accessTsToKeyMap, TaskType.ACCESS, entryKey, cacheEntry);
    }

    static MaintenanceTask newRemoveTask(NavigableMap<Long, Integer> accessTsToKeyMap, CacheEntry<?> cacheEntry) {
        return new MaintenanceTask(null, accessTsToKeyMap, TaskType.REMOVE, null, cacheEntry);
    }

    static MaintenanceTask newInvalidateTask(ConcurrentMap storage, NavigableMap<Long, Integer> accessTsToKeyMap) {
        return new MaintenanceTask(storage, accessTsToKeyMap, TaskType.INVALIDATE, null, null);
    }

    @Override
    public void run() {
        switch (type) {
            case ADD: {
                accessTsToKeyMap.put(cacheEntry.getLastAccessTs(), entryKey);
                break;
            }
            case ACCESS: {
                Long oldAccessTs = cacheEntry.getLastAccessTs();
                Long newAccessTs = System.currentTimeMillis();
                cacheEntry.setLastAccessTs(newAccessTs);
                accessTsToKeyMap.remove(oldAccessTs);
                accessTsToKeyMap.put(newAccessTs, entryKey);
                break;
            }
            case REMOVE: {
                accessTsToKeyMap.remove(cacheEntry.getLastAccessTs());
                break;
            }
            case INVALIDATE: {
                storage.remove(accessTsToKeyMap.remove(accessTsToKeyMap.firstKey()));
                break;
            }
            default:
                throw new IllegalStateException("unknown type: " + type);
        }
    }

    private enum TaskType {
        ADD, ACCESS, REMOVE, INVALIDATE
    }
}
