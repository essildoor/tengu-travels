package org.iofstorm.tengu.tengutravels.service;

import org.apache.commons.collections4.CollectionUtils;
import org.iofstorm.tengu.tengutravels.Utils;
import org.iofstorm.tengu.tengutravels.model.Location;
import org.iofstorm.tengu.tengutravels.model.ShortVisit;
import org.iofstorm.tengu.tengutravels.model.ShortVisits;
import org.iofstorm.tengu.tengutravels.model.User;
import org.iofstorm.tengu.tengutravels.model.Visit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.iofstorm.tengu.tengutravels.Constants.BAD_REQUEST;
import static org.iofstorm.tengu.tengutravels.Constants.NOT_FOUND;
import static org.iofstorm.tengu.tengutravels.Constants.OK;
import static org.iofstorm.tengu.tengutravels.Constants.VISITED_AT_MAX;
import static org.iofstorm.tengu.tengutravels.Constants.VISITED_AT_MIN;
import static org.iofstorm.tengu.tengutravels.model.Visit.ID;
import static org.iofstorm.tengu.tengutravels.model.Visit.LOCATION_ID;
import static org.iofstorm.tengu.tengutravels.model.Visit.MARK;
import static org.iofstorm.tengu.tengutravels.model.Visit.USER_ID;
import static org.iofstorm.tengu.tengutravels.model.Visit.VISITED_AT;

@Service
public class VisitService {
    private static final Logger log = LoggerFactory.getLogger(VisitService.class);

    private final Map<Integer, Visit> visits;
    private final Map<Integer, List<Visit>> visitsByUser;
    private final Map<Integer, List<Visit>> visitsByLocation;
    private final ReadWriteLock lock;

    @Autowired
    private UserService userService;

    @Autowired
    private LocationService locationService;

    public VisitService() {
        visits = new HashMap<>(500_000);
        visitsByUser = new HashMap<>(50_000);
        visitsByLocation = new HashMap<>(50_000);
        lock = new ReentrantReadWriteLock(true);
    }

    public Optional<Visit> getVisit(Integer id) {
        lock.readLock().lock();
        try {
            Visit v = visits.get(id);
            if (v == null) log.warn("getting visit by id={}, got null, visits size={}", id, visits.size());
            return Optional.ofNullable(v);
        } finally {
            lock.readLock().unlock();
        }
    }

    public CompletableFuture<Integer> createVisitAsync(Visit visit) {
        return CompletableFuture.supplyAsync(() -> {
            if (!validateOnCreate(visit)) return BAD_REQUEST;
            lock.readLock().lock();
            try {
                if (visits.containsKey(visit.getId())) return BAD_REQUEST;
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    if (visits.containsKey(visit.getId())) {
                        return BAD_REQUEST;
                    } else {
                        Optional<User> user = userService.getUser(visit.getUserId());
                        Optional<Location> location = locationService.getLocation(visit.getLocationId());
                        if (user.isPresent() && location.isPresent()) {
                            enrichVisit(visit, user.get(), location.get());
                            saveNewVisit(visit);
                            return OK;
                        } else {
                            return BAD_REQUEST;
                        }
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

    public CompletableFuture<Integer> updateVisitAsync(Integer visitId, Map<String, Object> visit) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                if (!visits.containsKey(visitId)) {
                    return NOT_FOUND;
                } else {
                    lock.readLock().unlock();
                    lock.writeLock().lock();
                    try {
                        if (!visits.containsKey(visitId)) {
                            return NOT_FOUND;
                        } else {
                            if (validateOnUpdate(visit)) {
                                Optional<User> user = userService.getUser((Integer) visit.get(USER_ID));
                                Optional<Location> location = locationService.getLocation((Integer) visit.get(LOCATION_ID));
                                if (user.isPresent() && location.isPresent()) {
                                    visits.compute(visitId, (id, oldVisit) -> remapVisit(oldVisit, visit));
                                    return OK;
                                } else {
                                    return BAD_REQUEST;
                                }
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
        });
    }

    public CompletableFuture<Optional<ShortVisits>> getUserVisitsAsync(Integer userId, Long fromDate, Long toDate, String country, Integer toDistance) {
        return CompletableFuture.supplyAsync(() -> {
            List<Visit> userVisits = getVisitsByUser(userId); // r/w lock
            List<ShortVisit> result = new ArrayList<>();
            for (Visit v : userVisits) {
                if (fromDate != null && v.getVisitedAt() < fromDate) continue;
                if (toDate != null && v.getVisitedAt() > toDate) continue;
                if (country != null && !country.equals(v.getLocationCountry())) continue;
                if (toDistance != null && v.getLocationDistance() > toDistance) continue;
                result.add(ShortVisit.fromVisit(v));
            }
            result.sort((o1, o2) -> (int) (o1.getVisitedAt() - o2.getVisitedAt()));
            ShortVisits shortVisits = new ShortVisits();
            shortVisits.setVisits(result);
            return Optional.of(shortVisits);
        });
    }

    // used for data loading
    public void load(List<Visit> visitList) {
        Set<Integer> userIds = new HashSet<>(visitList.size(), 1f);
        Set<Integer> locationIds = new HashSet<>(visitList.size(), 1f);
        visitList.forEach(vst -> {
            userIds.add(vst.getUserId());
            locationIds.add(vst.getLocationId());
        });
        Map<Integer, User> users = userService.getUsers(userIds);
        Map<Integer, Location> locations = locationService.getLocations(locationIds);
        visitList.forEach(vst -> enrichVisit(vst, users.get(vst.getUserId()), locations.get(vst.getLocationId())));

        lock.writeLock().lock();
        try {
            visitList.forEach(this::saveNewVisit);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // updates visits on location update
    CompletableFuture<Void> updateVisitWithLocationAsync(Integer locId, Map<String, Object> loc) {
        return CompletableFuture.runAsync(() -> {
            // todo check if exist with read lock first
            lock.writeLock().lock();
            try {
                for (Visit vst : visitsByLocation.get(locId)) {
                    if (loc.containsKey(Location.COUNTRY)) vst.setLocationCountry((String) loc.get(Location.COUNTRY));
                    if (loc.containsKey(Location.PLACE)) vst.setLocationPlace((String) loc.get(Location.PLACE));
                    if (loc.containsKey(Location.DISTANCE))
                        vst.setLocationDistance((Integer) loc.get(Location.DISTANCE));
                }
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    // updates visits on user update
    CompletableFuture<Void> updateVisitWithUserAsync(Integer userId, Map<String, Object> user) {
        return CompletableFuture.runAsync(() -> {
            // todo check if exist with read lock first
            lock.writeLock().lock();
            try {
                for (Visit vst : visitsByUser.get(userId)) {
                    if (user.containsKey(User.GENDER)) vst.setUserGender((String) user.get(User.GENDER));
                    if (user.containsKey(User.BIRTH_DATE))
                        vst.setUserId(Utils.calcAge((Long) user.get(User.BIRTH_DATE)));
                }
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    List<Visit> getVisitsByLocationId(Integer locId) {
        lock.readLock().lock();
        try {
            if (!visitsByLocation.containsKey(locId)) return Collections.emptyList();
            lock.readLock().unlock();
            lock.writeLock().lock();
            try {
                List<Visit> locationVisits = visitsByLocation.get(locId);
                return CollectionUtils.isNotEmpty(locationVisits) ? new ArrayList<>(locationVisits) : Collections.emptyList();
            } finally {
                lock.readLock().lock();
                lock.writeLock().unlock();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    private List<Visit> getVisitsByUser(Integer userId) {
        lock.readLock().lock();
        try {
            if (!visits.containsKey(userId)) return Collections.emptyList();
            lock.readLock().unlock();
            lock.writeLock().lock();
            try {
                if (!visits.containsKey(userId)) return Collections.emptyList();
                List<Visit> userVisits = visitsByUser.get(userId);
                return CollectionUtils.isNotEmpty(userVisits) ? new ArrayList<>(userVisits) : Collections.emptyList();
            } finally {
                lock.readLock().lock();
                lock.writeLock().unlock();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    private void saveNewVisit(Visit visit) {
        visits.put(visit.getId(), visit);
        visitsByUser.compute(visit.getUserId(), (userId, list) -> {
            if (list == null) list = new ArrayList<>();
            else list.add(visit);
            return list;
        });
        visitsByLocation.compute(visit.getLocationId(), (locId, list) -> {
            if (list == null) list = new ArrayList<>();
            else list.add(visit);
            return list;
        });
    }

    private void enrichVisit(Visit vst, User usr, Location loc) {
        vst.setUserAge(Utils.calcAge(usr.getBirthDate()));
        vst.setUserGender(usr.getGender());
        vst.setLocationCountry(loc.getCountry());
        vst.setLocationDistance(loc.getDistance());
        vst.setLocationPlace(loc.getPlace());
    }

    private Visit remapVisit(Visit oldVisit, Map<String, Object> newVisit) {
        if (newVisit.containsKey(LOCATION_ID)) oldVisit.setLocationId((Integer) newVisit.get(LOCATION_ID));
        if (newVisit.containsKey(USER_ID)) oldVisit.setUserId((Integer) newVisit.get(USER_ID));
        if (newVisit.containsKey(VISITED_AT)) oldVisit.setVisitedAt((Long) newVisit.get(VISITED_AT));
        if (newVisit.containsKey(MARK)) oldVisit.setMark((Integer) newVisit.get(MARK));
        return oldVisit;
    }

    private boolean validateOnUpdate(Map<String, Object> visit) {
        if (visit.containsKey(ID)) return false;
        if (visit.containsKey(LOCATION_ID)) {
            Object o = visit.get(LOCATION_ID);
            if (o == null || !(o instanceof Integer)) return false;
        }
        if (visit.containsKey(USER_ID)) {
            Object o = visit.get(USER_ID);
            if (o == null || !(o instanceof Integer)) return false;
        }
        if (visit.containsKey(VISITED_AT)) {
            Object o = visit.get(VISITED_AT);
            if (o == null || !(o instanceof Long) || ((Long) o < VISITED_AT_MIN || (Long) o > VISITED_AT_MAX))
                return false;
        }
        if (visit.containsKey(MARK)) {
            Object o = visit.get(MARK);
            if (o == null || !(o instanceof Integer) || ((Integer) o < 0 || (Integer) o > 5)) return false;
        }
        return true;
    }

    private boolean validateOnCreate(Visit visit) {
        if (visit == null) {
//            log.debug("visit validation failed: visit is null");
            return false;
        }
        if (visit.getId() == null) {
//            log.debug("visit validation failed: id={}, isCreate={}", visit.getId(), true);
            return false;
        }
        if (visit.getLocationId() == null) {
//            log.debug("visit validation failed: locationId={}, isCreate={}", visit.getLocationId(), true);
            return false;
        }
        if (visit.getUserId() == null) {
//            log.debug("visit validation failed: userId={}, isCreate={}", visit.getUserId(), true);
            return false;
        }
        if (visit.getVisitedAt() == null || (visit.getVisitedAt() != null && (visit.getVisitedAt() < VISITED_AT_MIN || visit.getVisitedAt() > VISITED_AT_MAX))) {
//            log.debug("visit validation failed: visitedAt={}, isCreate={}", visit.getVisitedAt(), true);
            return false;
        }
        if (visit.getMark() == null || (visit.getMark() != null && (visit.getMark() < 0 || visit.getMark() > 5))) {
//            log.debug("visit validation failed: visitedAt={}, isCreate={}", visit.getVisitedAt(), true);
            return false;
        }
        return true;
    }
}
