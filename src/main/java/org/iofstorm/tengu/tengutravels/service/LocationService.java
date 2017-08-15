package org.iofstorm.tengu.tengutravels.service;

import org.iofstorm.tengu.tengutravels.model.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.iofstorm.tengu.tengutravels.Constants.BAD_REQUEST;
import static org.iofstorm.tengu.tengutravels.Constants.NOT_FOUND;
import static org.iofstorm.tengu.tengutravels.Constants.OK;

public class LocationService {

    private final Map<Integer, Location> locations;
    private final ReadWriteLock lock;

    public LocationService() {
        locations = new HashMap<>(10_000, .6f);
        lock = new ReentrantReadWriteLock(true);
    }

    public Optional<Location> getLocation(Integer id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(locations.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    public int createLocation(Location location) {
        if (validate(location, true)) return BAD_REQUEST;
        lock.readLock().lock();
        try {
            if (locations.containsKey(location.getId())) return BAD_REQUEST;
            lock.readLock().unlock();
            lock.writeLock().lock();
            try {
                if (locations.containsKey(location.getId())) {
                    return BAD_REQUEST;
                } else {
                    locations.put(location.getId(), location);
                }
                lock.readLock().lock();
                return OK;
            } finally {
                lock.writeLock().unlock();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public int updateLocation(Location location) {
        lock.readLock().lock();
        try {
            if (!locations.containsKey(location.getId())) {
                return NOT_FOUND;
            } else {
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    if (!locations.containsKey(location.getId())) {
                        return NOT_FOUND;
                    } else {
                        if (validate(location, false)) {
                            locations.compute(location.getId(), (id, oldLoc) -> remapLocation(oldLoc, location));
                        } else {
                            return BAD_REQUEST;
                        }
                    }
                    lock.readLock().lock();
                    return OK;
                } finally {
                    lock.writeLock().unlock();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    private Location remapLocation(Location oldLoc, Location newLoc) {
        if (newLoc.getPlace() != null) oldLoc.setPlace(newLoc.getPlace());
        if (newLoc.getCity() != null) oldLoc.setCity(newLoc.getCity());
        if (newLoc.getCountry() != null) oldLoc.setCountry(newLoc.getCountry());
        if (newLoc.getDistance() != null) oldLoc.setDistance(newLoc.getDistance());
        return oldLoc;
    }

    private boolean validate(Location location, boolean isCreate) {
        if ((isCreate && location.getId() == null) || (!isCreate && location.getId() != null)) return false;
        if (isCreate && location.getPlace() == null) return false;
        if ((isCreate && location.getCountry() == null) || location.getCountry().length() > 50) return false;
        if ((isCreate && location.getCity() == null) || location.getCity().length() > 50) return false;
        if (isCreate && location.getDistance() == null) return false;
        return true;
    }
}
