package org.iofstorm.tengu.tengutravels.service;

import org.iofstorm.tengu.tengutravels.model.Location;
import org.iofstorm.tengu.tengutravels.model.ShortVisit;
import org.iofstorm.tengu.tengutravels.model.ShortVisits;
import org.iofstorm.tengu.tengutravels.model.User;
import org.iofstorm.tengu.tengutravels.model.Visit;
import org.iofstorm.tengu.tengutravels.model.Visits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.iofstorm.tengu.tengutravels.controller.ControllerHelper.BAD_REQUEST;
import static org.iofstorm.tengu.tengutravels.controller.ControllerHelper.OK;

@Service
public class VisitService {
    private static final ShortVisits EMPTY_SHORT_VISITS = new ShortVisits(Collections.emptyList());
    private static final List<Visit> EMPTY_VISITS = Collections.emptyList();
    private static final Comparator<ShortVisit> VISITED_AT_COMPARATOR = (o1, o2) -> (int)(o1.getVisitedAt() - o2.getVisitedAt());

    private final Map<Integer, Visit> visits;
    private final Map<Integer, List<Visit>> visitsByUser;
    private final Map<Integer, List<Visit>> visitsByLocation;
    private final ReadWriteLock lock;

    private UserService userService;
    private LocationService locationService;

    public ReadWriteLock getLock() {
        return lock;
    }

    public VisitService() {
        visits = new HashMap<>(10_041_000, 1f);
        visitsByUser = new HashMap<>(1_041_000, 1f);
        visitsByLocation = new HashMap<>(810_000, 1f);
        lock = new ReentrantReadWriteLock(true);
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Autowired
    public void setLocationService(LocationService locationService) {
        this.locationService = locationService;
    }

    public boolean visitExist(Integer id) {
        lock.readLock().lock();
        try {
            return visits.containsKey(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Visit getVisitWithoutLock(Integer id) {
        return visits.get(id);
    }

    public int createVisit(Visit visit) {
        lock.readLock().lock();
        try {
            if (visits.containsKey(visit.getId())) return BAD_REQUEST; // visit already exist
            lock.readLock().unlock();
            lock.writeLock().lock();
            try {
                if (visits.containsKey(visit.getId())) return BAD_REQUEST; // visit already exist

                User user = userService.getUserWithoutLock(visit.getUserId());
                Location location = locationService.getLocationWithoutLock(visit.getLocationId());
                if (user != null && location != null) {
                    enrichVisit(visit, user, location);
                    saveVisit(visit);
                    return OK;
                } else {
                    return BAD_REQUEST; // either user or location linked for this visit doesn't exist
                }
            } finally {
                lock.readLock().lock();
                lock.writeLock().unlock();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public void updateVisit(Integer visitId, Visit newVisit) {
        lock.writeLock().lock();
        try {
            Visit oldVisit = visits.get(visitId);

            if (newVisit.user != null) visitsByUser.get(oldVisit.getUserId()).remove(oldVisit);
            if (newVisit.location != null) visitsByLocation.get(oldVisit.getLocationId()).remove(oldVisit);

            Visit updatedVisit = remapVisit(oldVisit, newVisit, newVisit.user, newVisit.location);
            saveVisit(updatedVisit);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ShortVisits getUserVisits(Integer userId, Long fromDate, Long toDate, String country, Integer toDistance) {
        if (UserService.users.get(userId) == null) return null; // user not found
        List<Visit> userVisits = visitsByUser.get(userId);
        if (userVisits == null) return EMPTY_SHORT_VISITS; // user has no visits, return empty visits list, ok response

        List<ShortVisit> result = new ArrayList<>();

        for (Visit v : userVisits) {
            if (fromDate != null && v.getVisitedAt() <= fromDate) continue;
            if (toDate != null && v.getVisitedAt() >= toDate) continue;
            if (country != null && !country.equals(v.getLocationCountry())) continue;
            if (toDistance != null && v.getLocationDistance() >= toDistance) continue;
            result.add(new ShortVisit(v.getMark(), v.getVisitedAt(), v.getLocationPlace()));
        }
        result.sort(VISITED_AT_COMPARATOR);
        return new ShortVisits(result);
    }

    // used for data loading
    public void load(Visits visits) {
        for (Visit visit : visits.getVisits()) {
            saveVisit(visit);
        }
    }

    List<Visit> getVisitsByLocationId(Integer locationId) {
        List<Visit> locationVisits = visitsByLocation.get(locationId);
        if (locationVisits == null) return EMPTY_VISITS; // no visits for this location
        return locationVisits;
    }

    @SuppressWarnings("Java8MapApi")
    private void saveVisit(Visit visit) {
        visits.put(visit.getId(), visit);

        List<Visit> userVisits = visitsByUser.get(visit.getUserId());
        if (userVisits == null) {
            userVisits = new ArrayList<>();
            visitsByUser.put(visit.getUserId(), userVisits);
            userVisits.add(visit);
        } else if (!userVisits.contains(visit)) {
            userVisits.add(visit);
        }

        List<Visit> locVisits = visitsByLocation.get(visit.getLocationId());
        if (locVisits == null) {
            locVisits = new ArrayList<>();
            visitsByLocation.put(visit.getLocationId(), locVisits);
            locVisits.add(visit);
        } else if (!locVisits.contains(visit)) {
            locVisits.add(visit);
        }
    }

    private void enrichVisit(Visit vst, User usr, Location loc) {
        vst.setUser(usr);
        vst.setLocation(loc);
    }

    private Visit remapVisit(Visit oldVisit, Visit newVisit, User newUser, Location newLocation) {
        if (newVisit.getLocationId() != null) {
            // assuming newLocation is not null here
            oldVisit.setLocation(newLocation);
        }
        if (newVisit.getUserId() != null) {
            // assuming newUser is not null here
            oldVisit.setUser(newUser);
        }
        if (newVisit.getVisitedAt() != Long.MIN_VALUE) oldVisit.setVisitedAt(newVisit.getVisitedAt());
        if (newVisit.getMark() != Integer.MIN_VALUE) oldVisit.setMark(newVisit.getMark());

        return oldVisit;
    }
}
