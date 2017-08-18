package org.iofstorm.tengu.tengutravels.service;

import org.iofstorm.tengu.tengutravels.model.Location;
import org.iofstorm.tengu.tengutravels.model.Mark;
import org.iofstorm.tengu.tengutravels.model.Visit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.iofstorm.tengu.tengutravels.Constants.BAD_REQUEST;
import static org.iofstorm.tengu.tengutravels.Constants.CITY_LENGTH;
import static org.iofstorm.tengu.tengutravels.Constants.COUNTRY_LENGTH;
import static org.iofstorm.tengu.tengutravels.Constants.NOT_FOUND;
import static org.iofstorm.tengu.tengutravels.Constants.OK;
import static org.iofstorm.tengu.tengutravels.Constants.ZERO_DOUBLE;
import static org.iofstorm.tengu.tengutravels.model.Location.CITY;
import static org.iofstorm.tengu.tengutravels.model.Location.COUNTRY;
import static org.iofstorm.tengu.tengutravels.model.Location.DISTANCE;
import static org.iofstorm.tengu.tengutravels.model.Location.ID;
import static org.iofstorm.tengu.tengutravels.model.Location.PLACE;

@Service
public class LocationService {
    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private final Map<Integer, Location> locations;
    private final ReadWriteLock lock;

    @Autowired
    private VisitService visitService;

    public LocationService() {
        locations = new HashMap<>(50_000);
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

    public CompletableFuture<Integer> createLocationAsync(Location location) {
        return CompletableFuture.supplyAsync(() -> {
            if (!validateOnCreate(location)) return BAD_REQUEST;
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
        });
    }

    public CompletableFuture<Integer> updateLocationAsync(Integer locationId, Map<String, Object> location) {
        return CompletableFuture.supplyAsync(() -> {
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
                            if (validateOnUpdate(location)) {
                                locations.compute(locationId, (id, oldLoc) -> remapLocation(oldLoc, location));
                                return OK;
                            } else {
                                return BAD_REQUEST;
                            }
                        }
                    } finally {
                        lock.readLock().lock();
                        lock.writeLock().unlock();
                    }
                }
            } finally {
                lock.readLock().unlock();
            }
        }).thenApply(code -> {
            if (Objects.equals(code, OK)) try {
                visitService.updateVisitWithLocationAsync(locationId, location).get();
            } catch (InterruptedException | ExecutionException e) {
                return BAD_REQUEST;
            }
            return code;
        });
    }

    public CompletableFuture<Optional<Mark>> getAverageMarkAsync(Integer locationId, Long fromDate, Long toDate, Integer fromAge, Integer toAge, String gender) {
        return CompletableFuture.supplyAsync(() -> {
            if (!exist(locationId)) return Optional.empty();
            List<Visit> visitsByLocation = visitService.getVisitsByLocationId(locationId);
            Integer acc = 0;
            int i = 0;
            for (Visit v : visitsByLocation) {
                if (fromDate != null && v.getVisitedAt() < fromDate) continue;
                if (toDate != null && v.getVisitedAt() > toDate) continue;
                if (fromAge != null && v.getUserAge() < fromAge) continue;
                if (toAge != null && v.getUserAge() > toAge) continue;
                if (gender != null && !gender.equals(v.getUserGender())) continue;
                acc += v.getMark();
                i++;
            }
            Double markk = ZERO_DOUBLE;
            if (i != 0) {
                markk = ((double) acc) / i;
            }
            Mark mark = new Mark();
            mark.setMark(markk);
            return Optional.of(mark);
        });
    }

    // used for data loading
    public void load(List<Location> locationList) {
        lock.writeLock().lock();
        try {
            locationList.forEach(loc -> locations.put(loc.getId(), loc));
        } finally {
            lock.writeLock().unlock();
        }
    }

    // used for data loading
    Map<Integer, Location> getLocations(Set<Integer> ids) {
        final Map<Integer, Location> res = new HashMap<>(ids.size(), 1f);
        lock.readLock().lock();
        try {
            for (Integer id : ids) res.put(id, locations.get(id));
        } finally {
            lock.readLock().unlock();
        }
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

    private Location remapLocation(Location oldLoc, Map<String, Object> newLoc) {
        if (newLoc.containsKey(PLACE)) oldLoc.setPlace((String) newLoc.get(PLACE));
        if (newLoc.containsKey(CITY)) oldLoc.setCity((String) newLoc.get(CITY));
        if (newLoc.containsKey(COUNTRY)) oldLoc.setCountry((String) newLoc.get(COUNTRY));
        if (newLoc.containsKey(DISTANCE)) oldLoc.setDistance((Integer) newLoc.get(DISTANCE));
        return oldLoc;
    }

    private boolean validateOnUpdate(Map<String, Object> loc) {
        if (loc.containsKey(ID)) return false;
        if (loc.containsKey(PLACE)) {
            Object o = loc.get(PLACE);
            if (o == null || !(o instanceof String)) return false;
        }
        if (loc.containsKey(COUNTRY)) {
            Object o = loc.get(COUNTRY);
            if (o == null || !(o instanceof String) || ((String) o).length() > COUNTRY_LENGTH) return false;
        }
        if (loc.containsKey(CITY)) {
            Object o = loc.get(CITY);
            if (o == null || !(o instanceof String) || ((String) o).length() > CITY_LENGTH) return false;
        }
        if (loc.containsKey(DISTANCE)) {
            Object o = loc.get(DISTANCE);
            if (o == null || !(o instanceof Integer)) return false;
        }
        return true;
    }

    private boolean validateOnCreate(Location location) {
        if (location == null) {
//            log.debug("location validation failed: location is null");
            return false;
        }
        if (location.getId() == null) {
//            log.debug("location validation failed: id={}, isCreate={}", location.getId(), isCreate);
            return false;
        }
        if (location.getPlace() == null) {
//            log.debug("location validation failed: place={}, isCreate={}", location.getPlace(), isCreate);
            return false;
        }
        if (location.getCountry() == null || (location.getCountry() != null && location.getCountry().length() > COUNTRY_LENGTH)) {
//            log.debug("location validation failed: country={}, isCreate={}", location.getCountry(), isCreate);
            return false;
        }
        if (location.getCity() == null || (location.getCity() != null && location.getCity().length() > CITY_LENGTH)) {
//            log.debug("location validation failed: city={}, isCreate={}", location.getCity(), isCreate);
            return false;
        }
        if (location.getDistance() == null) {
//            log.debug("location validation failed: distance={}, isCreate={}", location.getDistance(), isCreate);
            return false;
        }
        return true;
    }
}
