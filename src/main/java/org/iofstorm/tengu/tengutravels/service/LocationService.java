package org.iofstorm.tengu.tengutravels.service;

import org.iofstorm.tengu.tengutravels.model.Gender;
import org.iofstorm.tengu.tengutravels.model.Location;
import org.iofstorm.tengu.tengutravels.model.Mark;
import org.iofstorm.tengu.tengutravels.model.Visit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.iofstorm.tengu.tengutravels.controller.ControllerHelper.BAD_REQUEST;
import static org.iofstorm.tengu.tengutravels.controller.ControllerHelper.NOT_FOUND;
import static org.iofstorm.tengu.tengutravels.controller.ControllerHelper.OK;

@Service
public class LocationService {
    public static final Map<Integer, Location> locations = new HashMap<>(810_000, 1f);

    private final ReadWriteLock lock;

    @Autowired
    private VisitService visitService;

    public LocationService() {
        lock = new ReentrantReadWriteLock(true);
    }

    ReadWriteLock getLock() {
        return lock;
    }

    public Location getLocationWithoutLock(Integer id) {
        return locations.get(id);
    }

    public int createLocation(Location location) {
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

    public int updateLocation(Integer locationId, Location newLocation) {
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
                    Location updated = remapLocation(locations.get(locationId), newLocation);
                    locations.put(locationId, updated);
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

    public Mark getAverageMark(Integer locationId, Long fromDate, Long toDate, Integer fromAge, Integer toAge, Gender gender) {
        if (!locations.containsKey(locationId)) return null;
        List<Visit> visitsByLocation = visitService.getVisitsByLocationId(locationId);
        int i = 0;
        int acc = 0;
        boolean fromDateIsPresent = fromDate != null;
        boolean toDateIsPresent = toDate != null;
        boolean genderIsPresent = gender != Gender.UNKNOWN;
        boolean fromAgeIsPresent = fromAge != null;
        boolean toAgeIsPresent = toAge != null;
        for (Visit v : visitsByLocation) {
            if (fromDateIsPresent && v.getVisitedAt() <= fromDate) continue;
            if (toDateIsPresent && v.getVisitedAt() >= toDate) continue;
            if (genderIsPresent && gender != v.getUserGender()) continue;
            if (fromAgeIsPresent && v.getUserAge() < fromAge) continue;
            if (toAgeIsPresent && v.getUserAge() >= toAge) continue;

            acc += v.getMark();
            i++;
        }
        BigDecimal avg;
        if (acc == 0) avg = BigDecimal.ZERO;
        else avg = BigDecimal.valueOf(acc).divide(BigDecimal.valueOf(i), 5, BigDecimal.ROUND_HALF_UP);
        return new Mark(avg);
    }

    // used for data loading
    public void load(List<Location> locationList) {
        lock.writeLock().lock();
        try {
            for (Location location : locationList) {
                locations.put(location.getId(), location);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Location remapLocation(Location oldLoc, Location newLoc) {
        if (newLoc.getPlace() != null) oldLoc.setPlace(newLoc.getPlace());
        if (newLoc.getCity() != null) oldLoc.setCity(newLoc.getCity());
        if (newLoc.getCountry() != null) oldLoc.setCountry(newLoc.getCountry());
        if (newLoc.getDistance() != 0) oldLoc.setDistance(newLoc.getDistance());
        return oldLoc;
    }
}
