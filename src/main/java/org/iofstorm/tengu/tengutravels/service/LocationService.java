package org.iofstorm.tengu.tengutravels.service;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.iofstorm.tengu.tengutravels.model.Entity;
import org.iofstorm.tengu.tengutravels.model.Location;
import org.iofstorm.tengu.tengutravels.model.Mark;
import org.iofstorm.tengu.tengutravels.model.Visit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import static org.iofstorm.tengu.tengutravels.Constants.BAD_REQUEST;
import static org.iofstorm.tengu.tengutravels.Constants.CITY_LENGTH;
import static org.iofstorm.tengu.tengutravels.Constants.COUNTRY_LENGTH;
import static org.iofstorm.tengu.tengutravels.Constants.NOT_FOUND;
import static org.iofstorm.tengu.tengutravels.Constants.OK;

@Service
public class LocationService {

    private final Map<Integer, Location> locations;
    private final ReadWriteLock lock;

    @Autowired
    private VisitService visitService;

    public LocationService() {
        locations = new HashMap<>(10_000, .6f);
        lock = new ReentrantReadWriteLock(true);
    }

    public boolean exist(Integer id) {
        lock.readLock().lock();
        try {
            return locations.containsKey(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<Location> getLocation(Integer id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(locations.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<Integer, Location> getLocations(Set<Integer> ids) {
        lock.readLock().lock();
        Map<Integer, Location> res = new HashMap<>(ids.size(), 1f);
        try {
            ids.stream().map(locations::get).forEach(loc -> res.put(loc.getId(), loc));
        } finally {
            lock.readLock().unlock();
        }
        return res;
    }

    /**
     * Loads chunk of locations to service's storage
     *
     * @param locationList list of locations
     */
    public void load(List<Location> locationList) {
        lock.writeLock().lock();
        try {
            locationList.forEach(loc -> locations.put(loc.getId(), loc));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int createLocation(Location location) {
        if (exist(location.getId()) || validate(location, true)) return BAD_REQUEST;
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

    public int updateLocation(Integer locationId, Location location) {
        if (!exist(locationId)) return NOT_FOUND;
        lock.readLock().lock();
        try {
            if (!locations.containsKey(locationId)) {
                return NOT_FOUND;
            } else {
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    if (!locations.containsKey(locationId)) {
                        return NOT_FOUND;
                    } else {
                        if (validate(location, false)) {
                            locations.compute(locationId, (id, oldLoc) -> remapLocation(oldLoc, location));
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

    public CompletableFuture<ResponseEntity<Entity>> getAverageMark(Integer locationId, Long fromDate, Long toDate, Integer fromAge, Integer toAge, String gender) {
        return CompletableFuture.supplyAsync(() -> {
            if (!exist(locationId)) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            List<Visit> visitsByLocation = visitService.getVisitsByLocationId(locationId);
            Double result;
            if (CollectionUtils.isNotEmpty(visitsByLocation)) {
                Mean mean = new Mean();
                Stream<Visit> vs = visitsByLocation.stream();
                if (fromDate != null) vs = vs.filter(visit -> visit.getVisitedAt() >= fromDate);
                if (toDate != null) vs = vs.filter(visit -> visit.getVisitedAt() <= toDate);
                if (fromAge != null) vs = vs.filter(visit -> visit.getUserAge() >= fromAge);
                if (toAge != null) vs = vs.filter(visit -> visit.getUserAge() <= toAge);
                if (gender != null) vs = vs.filter(visit -> gender.equals(visit.getUserGender()));
                vs.map(Visit::getMark)
                        .forEach(mean::increment);
                result = mean.getResult();
            } else {
                result = 0.0d;
            }
            Mark mark = new Mark();
            mark.setMark(result);
            return ResponseEntity.ok(mark);
        });
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
        if ((isCreate && location.getCountry() == null) || location.getCountry().length() > COUNTRY_LENGTH)
            return false;
        if ((isCreate && location.getCity() == null) || location.getCity().length() > CITY_LENGTH) return false;
        if (isCreate && location.getDistance() == null) return false;
        return true;
    }
}
