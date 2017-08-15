package org.iofstorm.tengu.tengutravels.cache;

final class CacheEntry<T> {
    private final T value;
    private long lastAccessTs;

    CacheEntry(T value) {
        this.value = value;
        lastAccessTs = System.currentTimeMillis();
    }

    T getValue() {
        return value;
    }

    public long getLastAccessTs() {
        return lastAccessTs;
    }

    public void setLastAccessTs(long lastAccessTs) {
        this.lastAccessTs = lastAccessTs;
    }
}
