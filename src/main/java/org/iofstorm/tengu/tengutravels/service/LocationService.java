package org.iofstorm.tengu.tengutravels.service;

import org.iofstorm.tengu.tengutravels.Utils;
import org.iofstorm.tengu.tengutravels.model.Location;
import org.iofstorm.tengu.tengutravels.model.Mark;
import org.iofstorm.tengu.tengutravels.model.Visit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.iofstorm.tengu.tengutravels.controller.ControllerHelper.BAD_REQUEST;
import static org.iofstorm.tengu.tengutravels.controller.ControllerHelper.NOT_FOUND;
import static org.iofstorm.tengu.tengutravels.controller.ControllerHelper.OK;

@Service
public class LocationService {
    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private final Map<Integer, Location> locations;
    private final ReadWriteLock lock;
    private final VisitService visitService;
    private final Utils utils;

    public LocationService(VisitService visitService, Utils utils) {
        locations = new HashMap<>();
        lock = new ReentrantReadWriteLock(true);
        this.visitService = Objects.requireNonNull(visitService);
        this.utils = Objects.requireNonNull(utils);
    }

    public ReadWriteLock getLock() {
        return lock;
    }

    public Location getLocation(Integer id) {
        if (id == null) return null;
        lock.readLock().lock();
        try {
            return locations.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    Location getLocationWithoutLock(Integer id) {
        if (id == null) return null;
        return locations.get(id);
    }

    public Integer createLocation(Location location) {
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
                    return OK;
                }
            } finally {
                lock.readLock().lock();
                lock.writeLock().unlock();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public Integer updateLocation(Integer locationId, Location newLocation) {
        lock.readLock().lock();
        try {
            if (!locations.containsKey(locationId)) {
                return NOT_FOUND;
            } else if (newLocation == null) {
                return BAD_REQUEST;
            } else {
                lock.readLock().unlock();

                lock.writeLock().lock();
                visitService.getLock().writeLock().lock();
                try {
                    locations.compute(locationId, (id, oldLoc) -> remapLocation(oldLoc, newLocation));
                    visitService.updateVisitWithLocationWithoutLock(locationId, newLocation);
                    return OK;
                } finally {
                    lock.readLock().lock();
                    visitService.getLock().writeLock().unlock();
                    lock.writeLock().unlock();

                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public Mark getAverageMark(Integer locationId, Long fromDate, Long toDate, Integer fromAge, Integer toAge, String gender) {
        if (!exist(locationId)) return null;
        List<Visit> visitsByLocation = visitService.getVisitsByLocationId(locationId);
        BigDecimal acc = BigDecimal.ZERO;
        int i = 0;
        for (Visit v : visitsByLocation) {
            if (fromDate != null && v.getVisitedAt() <= fromDate) continue;
            if (toDate != null && v.getVisitedAt() >= toDate) continue;
            if (fromAge != null && v.getUserAge() < fromAge) continue;
            if (toAge != null && v.getUserAge() >= toAge) continue;
            if (gender != null && !gender.equals(v.getUserGender())) continue;
            acc = acc.add(BigDecimal.valueOf(v.getMark()));
            i++;
        }
        acc = acc.equals(BigDecimal.ZERO) ? BigDecimal.ZERO : acc.divide(BigDecimal.valueOf(i), 5, BigDecimal.ROUND_HALF_UP);
        Mark mark = new Mark();
        mark.setAvg(acc);
        return mark;
    }

    // used for data loading
    public Integer load(List<Location> locationList) {
        lock.writeLock().lock();
        try {
            locationList.forEach(loc -> {
                locations.put(loc.getId(), loc);
            });
        } finally {
            lock.writeLock().unlock();
        }
        return locationList.size();
    }

    // used for data loading
    Map<Integer, Location> getLocations(Set<Integer> ids) {
        Map<Integer, Location> res = new HashMap<>(ids.size());
        for (Integer id : ids) res.put(id, locations.get(id));
        return res;
    }

    private boolean exist(Integer id) {
        lock.readLock().lock();
        try {
            return locations.containsKey(id);
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
}
