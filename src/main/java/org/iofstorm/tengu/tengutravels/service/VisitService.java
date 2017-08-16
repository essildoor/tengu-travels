package org.iofstorm.tengu.tengutravels.service;

import org.apache.commons.collections4.CollectionUtils;
import org.iofstorm.tengu.tengutravels.Utils;
import org.iofstorm.tengu.tengutravels.model.Entity;
import org.iofstorm.tengu.tengutravels.model.Location;
import org.iofstorm.tengu.tengutravels.model.User;
import org.iofstorm.tengu.tengutravels.model.Visit;
import org.iofstorm.tengu.tengutravels.model.Visits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.iofstorm.tengu.tengutravels.Constants.BAD_REQUEST;
import static org.iofstorm.tengu.tengutravels.Constants.NOT_FOUND;
import static org.iofstorm.tengu.tengutravels.Constants.OK;
import static org.iofstorm.tengu.tengutravels.Constants.VISITED_AT_MAX;
import static org.iofstorm.tengu.tengutravels.Constants.VISITED_AT_MIN;

@Service
public class VisitService {

    private final Map<Integer, Visit> visits;
    private final Map<Integer, List<Visit>> visitsByUser;
    private final Map<Integer, List<Visit>> visitsByLocation;
    private final ReadWriteLock lock;

    @Autowired
    private UserService userService;

    @Autowired
    private LocationService locationService;

    public VisitService() {
        visits = new HashMap<>(100_000, .65f);
        visitsByUser = new HashMap<>(1000);
        visitsByLocation = new HashMap<>(1000);
        lock = new ReentrantReadWriteLock(true);
    }

    public boolean exist(Integer id) {
        lock.readLock().lock();
        try {
            return visits.containsKey(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<Visit> getVisit(Integer id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(visits.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Loads chunk of visits to service storage
     *
     * Must be called after all users and locations has been loaded
     *
     * @param visitList list of visits
     */
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
            lock.writeLock().lock();
        }
    }

    public int createVisit(Visit visit) {
        if (exist(visit.getId()) || validate(visit, true)) return BAD_REQUEST;
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
                    } else {
                        return BAD_REQUEST;
                    }
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

    public int updateVisit(Integer visitId, Visit visit) {
        if (!exist(visitId)) return NOT_FOUND;
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
                        if (validate(visit, false)) {
                            visits.compute(visitId, (id, oldVisit) -> remapVisit(oldVisit, visit));
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

    public CompletableFuture<ResponseEntity<Entity>> getVisits(Integer userId, Long fromDate, Long toDate, String country, Integer toDistance) {
        return CompletableFuture.supplyAsync(() -> {
            if (!exist(userId)) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            lock.readLock().lock();
            List<Visit> userVisits = getVisitsByUserId(userId);
            List<Visit> result;
            if (CollectionUtils.isNotEmpty(userVisits)) {
                Stream<Visit> vs = userVisits.stream();
                if (fromDate != null) vs = vs.filter(visit -> visit.getVisitedAt() >= fromDate);
                if (toDate != null) vs = vs.filter(visit -> visit.getVisitedAt() <= toDate);
                if (country != null || toDistance != null) {
                    vs = vs.filter(visit -> {
                        boolean res = true;
                        Optional<Location> locationOptional = locationService.getLocation(visit.getLocationId());
                        if (locationOptional.isPresent()) {
                            Location loc = locationOptional.get();
                            if (country != null) res = country.equals(loc.getCountry());
                            if (toDistance != null) res = res && toDistance <= loc.getDistance();
                        }
                        return res;
                    });
                }
                vs = vs.sorted((o1, o2) -> (int)(o1.getVisitedAt() - o2.getVisitedAt()));
                result = vs.collect(Collectors.toList());
            } else {
                result = Collections.emptyList();
            }
            Visits visits = new Visits();
            visits.setVisits(result);
            return ResponseEntity.ok(visits);
        });
    }

    public List<Visit> getVisitsByUserId(Integer userId) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(visitsByUser.get(userId)).orElse(Collections.emptyList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Visit> getVisitsByLocationId(Integer locId) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(visitsByLocation.get(locId)).orElse(Collections.emptyList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Saves enriched visit to service's storage
     *
     * Must be executed with write lock
     *
     * @param visit enriched visit to save
     */
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
    }

    private Visit remapVisit(Visit oldVisit, Visit newVisit) {
        if (newVisit.getLocationId() != null) oldVisit.setLocationId(newVisit.getLocationId());
        if (newVisit.getUserId() != null) oldVisit.setUserId(newVisit.getUserId());
        if (newVisit.getVisitedAt() != null) oldVisit.setVisitedAt(newVisit.getVisitedAt());
        if (newVisit.getMark() != null) oldVisit.setMark(newVisit.getMark());
        return oldVisit;
    }

    private boolean validate(Visit visit, boolean isCreate) {
        if ((!isCreate && visit.getId() != null) || (isCreate && visit.getId() == null)) return false;
        if (isCreate && visit.getLocationId() == null) return false;
        if (isCreate && visit.getUserId() == null) return false;
        if ((isCreate && visit.getVisitedAt() == null) || (visit.getVisitedAt() < VISITED_AT_MIN || visit.getVisitedAt() > VISITED_AT_MAX)) return false;
        if (visit.getMark() == null || (visit.getMark() < 0 || visit.getMark() > 5)) return false;
        return true;
    }
}
