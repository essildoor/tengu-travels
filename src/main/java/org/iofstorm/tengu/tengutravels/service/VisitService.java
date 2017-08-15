package org.iofstorm.tengu.tengutravels.service;

import org.iofstorm.tengu.tengutravels.model.Location;
import org.iofstorm.tengu.tengutravels.model.Visit;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.iofstorm.tengu.tengutravels.Constants.BAD_REQUEST;
import static org.iofstorm.tengu.tengutravels.Constants.NOT_FOUND;
import static org.iofstorm.tengu.tengutravels.Constants.OK;

public class VisitService {

    private final Map<Integer, Visit> visits;
    private final Map<Integer, SortedSet<Visit>> visitsByUser;
    private final ReadWriteLock lock;
    private final LocationService locationService;

    @Autowired
    public VisitService(LocationService locationService) {
        visits = new HashMap<>(100_000, .65f);
        visitsByUser = new HashMap<>(1000);
        lock = new ReentrantReadWriteLock(true);
        this.locationService = Objects.requireNonNull(locationService);
    }

    public Optional<Visit> getVisit(Integer id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(visits.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    public int createVisit(Visit visit) {
        if (validate(visit, true)) return BAD_REQUEST;
        lock.readLock().lock();
        try {
            if (visits.containsKey(visit.getId())) return BAD_REQUEST;
            lock.readLock().unlock();
            lock.writeLock().lock();
            try {
                if (visits.containsKey(visit.getId())) {
                    return BAD_REQUEST;
                } else {
                    visits.put(visit.getId(), visit);
                    visitsByUser.compute(visit.getUserId(), (userId, set) -> {
                        if (set == null) set = new TreeSet<>((o1, o2) -> (int) (o1.getVisitedAt() - o2.getVisitedAt()));
                        else set.add(visit);
                        return set;
                    });
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

    public int updateVisit(Visit visit) {
        lock.readLock().lock();
        try {
            if (!visits.containsKey(visit.getId())) {
                return NOT_FOUND;
            } else {
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    if (!visits.containsKey(visit.getId())) {
                        return NOT_FOUND;
                    } else {
                        if (validate(visit, false)) {
                            visits.compute(visit.getId(), (id, oldVisit) -> remapVisit(oldVisit, visit));
                            visitsByUser.compute(visit.getUserId(), (userId, set) -> {
                                set.add(visit);
                                return set;
                            });
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

    public CompletableFuture<List<Visit>> getVisits(Integer userId, Long fromDate, Long toDate, String country, Integer toDistance) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            Set<Visit> userVisits;
            try {
                userVisits = visitsByUser.get(userId);
            } finally {
                lock.readLock().unlock();
            }
            if (userVisits != null) {
                Stream<Visit> vs = userVisits.stream();
                if (fromDate != null) vs = vs.filter(visit -> visit.getVisitedAt() >= fromDate);
                if (toDate != null) vs = vs.filter(visit -> visit.getVisitedAt() <= toDate);
                if (country != null || toDistance != null) {
                    vs = vs.filter(visit -> {
                        boolean result = true;
                        Optional<Location> locationOptional = locationService.getLocation(visit.getLocationId());
                        if (locationOptional.isPresent()) {
                            Location loc = locationOptional.get();
                            if (country != null) result = country.equals(loc.getCountry());
                            if (toDistance != null) result = result && toDistance <= loc.getDistance();
                        }
                        return result;
                    });
                }
                return vs.collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        });
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
        if (isCreate) {
            if (visit.getLocationId() == null) return false;
            if (visit.getUserId() == null) return false;
            if (visit.getVisitedAt() == null) return false;
            if (visit.getMark() == null) return false;
        }
        return true;
    }
}
