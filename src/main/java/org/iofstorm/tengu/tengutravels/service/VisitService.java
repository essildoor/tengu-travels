package org.iofstorm.tengu.tengutravels.service;

import org.apache.commons.collections4.CollectionUtils;
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
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.iofstorm.tengu.tengutravels.controller.ControllerHelper.BAD_REQUEST;
import static org.iofstorm.tengu.tengutravels.controller.ControllerHelper.OK;

@Service
public class VisitService {
    private static final Logger log = LoggerFactory.getLogger(VisitService.class);

    private final Map<Integer, Visit> visits;
    private final Map<Integer, Set<Visit>> visitsByUser;
    private final Map<Integer, Set<Visit>> visitsByLocation;
    private final ReadWriteLock lock;

    @Autowired
    private UserService userService;

    @Autowired
    private LocationService locationService;

    public ReadWriteLock getLock() {
        return lock;
    }

    public VisitService() {
        visits = new HashMap<>();
        visitsByUser = new HashMap<>();
        visitsByLocation = new HashMap<>();
        lock = new ReentrantReadWriteLock(true);
    }

    public boolean visitExist(Integer id) {
        lock.readLock().lock();
        try {
            return visits.containsKey(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Visit getVisit(Integer id) {
        lock.readLock().lock();
        try {
            return visits.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Integer createVisit(Visit visit) {
        lock.readLock().lock();
        try {
            if (visits.containsKey(visit.getId())) return BAD_REQUEST; // visit already exist
            lock.readLock().unlock();

            userService.getLock().writeLock().lock();
            locationService.getLock().writeLock().lock();
            lock.writeLock().lock();
            try {
                if (visits.containsKey(visit.getId())) return BAD_REQUEST; // visit already exist

                User user = userService.getUserWithouLock(visit.getUserId());
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
                locationService.getLock().writeLock().unlock();
                userService.getLock().writeLock().unlock();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public void updateVisit(Integer visitId, Visit newVisit) {
        userService.getLock().writeLock().lock();
        locationService.getLock().writeLock().lock();
        lock.writeLock().lock();
        try {
            Visit oldVisit = visits.get(visitId);

            User newUser = userService.getUser(newVisit.getUserId());
            Location newLocation = locationService.getLocation(newVisit.getLocationId());

            if (newUser != null) visitsByUser.get(oldVisit.getUserId()).remove(oldVisit);
            if (newLocation != null) visitsByLocation.get(oldVisit.getLocationId()).remove(oldVisit);

            Visit updatedVisit = remapVisit(oldVisit, newVisit, newUser, newLocation);

            saveVisit(updatedVisit);
        } finally {
            lock.writeLock().unlock();
            locationService.getLock().writeLock().unlock();
            userService.getLock().writeLock().unlock();
        }
    }

    public ShortVisits getUserVisits(Integer userId, Long fromDate, Long toDate, String country, Integer toDistance) {
        Set<Visit> userVisits = getVisitsByUser(userId); // user r lock
        if (userVisits == null) return null; // user not found
        List<ShortVisit> result = new ArrayList<>();
        for (Visit v : userVisits) {
            if (fromDate != null && v.getVisitedAt() <= fromDate) continue;
            if (toDate != null && v.getVisitedAt() >= toDate) continue;
            if (country != null && !country.equals(v.getLocationCountry())) continue;
            if (toDistance != null && v.getLocationDistance() >= toDistance) continue;
            result.add(ShortVisit.fromVisit(v));
        }
        result.sort((o1, o2) -> (int) (o1.getVisitedAt() - o2.getVisitedAt()));
        ShortVisits shortVisits = new ShortVisits();
        shortVisits.setVisits(result);
        return shortVisits; // return full or empty list here, for ok response
    }

    // used for data loading
    public Integer load(List<Visit> visitList) {
        Set<Integer> userIds = new HashSet<>(visitList.size());
        Set<Integer> locationIds = new HashSet<>(visitList.size());
        visitList.forEach(vst -> {
            userIds.add(vst.getUserId());
            locationIds.add(vst.getLocationId());
        });
        Map<Integer, User> users = userService.getUsers(userIds);
        Map<Integer, Location> locations = locationService.getLocations(locationIds);
        visitList.forEach(vst -> enrichVisit(vst, users.get(vst.getUserId()), locations.get(vst.getLocationId())));

        lock.writeLock().lock();
        try {
            visitList.forEach(this::saveVisit);
        } finally {
            lock.writeLock().unlock();
        }
        return visitList.size();
    }

    // updates visits on location update
    public void updateVisitWithLocationWithoutLock(Integer locId, Location loc) {
        Set<Visit> visitList = visitsByLocation.get(locId);
        if (CollectionUtils.isNotEmpty(visitList)) {
            boolean updateCountry = loc.getCountry() != null;
            boolean updatePlace = loc.getPlace() != null;
            boolean updateDistance = loc.getDistance() != null;
            for (Visit vst : visitList) {
                if (updateCountry) vst.setLocationCountry(loc.getCountry());
                if (updatePlace) vst.setLocationPlace(loc.getPlace());
                if (updateDistance) vst.setLocationDistance(loc.getDistance());
            }
        }
    }

    // updates visits on user update
    public void updateVisitWithUserWithoutLock(Integer userId, User newUser) {
        Set<Visit> visitList = visitsByUser.get(userId);
        if (CollectionUtils.isNotEmpty(visitList)) {
            boolean updateGender = newUser.getGender() != null;
            boolean updateAge = newUser.getBirthDate() != null;
            for (Visit vst : visitList) {
                if (updateGender) vst.setUserGender(newUser.getGender());
                if (updateAge) vst.setUserAge(newUser.getAge());
            }
        }
    }

    List<Visit> getVisitsByLocationId(Integer locId) {
        lock.readLock().lock();
        try {
            if (!visitsByLocation.containsKey(locId)) return Collections.emptyList();
            lock.readLock().unlock();
            lock.writeLock().lock();
            try {
                Set<Visit> locationVisits = visitsByLocation.get(locId);
                return CollectionUtils.isNotEmpty(locationVisits) ? new ArrayList<>(locationVisits) : Collections.emptyList();
            } finally {
                lock.readLock().lock();
                lock.writeLock().unlock();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    private Set<Visit> getVisitsByUser(Integer userId) {
        lock.readLock().lock();
        try {
            if (!userService.userExist(userId)) return null; // user not found
            Set<Visit> visitByUser = visitsByUser.get(userId);
            return visitByUser == null ? Collections.emptySet() : visitByUser;
        } finally {
            lock.readLock().unlock();
        }
    }

    private void saveVisit(Visit visit) {
        visits.put(visit.getId(), visit);
        visitsByUser.compute(visit.getUserId(), (userId, list) -> {
            if (list == null) list = new HashSet<>(1000);
            list.add(visit);
            return list;
        });
        visitsByLocation.compute(visit.getLocationId(), (locId, list) -> {
            if (list == null) list = new HashSet<>(1000);
            list.add(visit);
            return list;
        });
    }

    private void enrichVisit(Visit vst, User usr, Location loc) {
        vst.setUserAge(usr.getAge());
        vst.setUserGender(usr.getGender());
        vst.setLocationCountry(loc.getCountry());
        vst.setLocationDistance(loc.getDistance());
        vst.setLocationPlace(loc.getPlace());
    }

    private Visit remapVisit(Visit oldVisit, Visit newVisit, User newUser, Location newLocation) {
        if (newVisit.getLocationId() != null) {
            // assuming newLocation is not null here
            oldVisit.setLocationId(newVisit.getLocationId());
            oldVisit.setLocationDistance(newLocation.getDistance());
            oldVisit.setLocationPlace(newLocation.getPlace());
            oldVisit.setLocationCountry(newLocation.getCountry());
        }
        if (newVisit.getUserId() != null) {
            // assuming newUser is not null here
            oldVisit.setUserId(newVisit.getUserId());
            oldVisit.setUserGender(newUser.getGender());
            oldVisit.setUserAge(newUser.getAge());
        }
        if (newVisit.getVisitedAt() != null) oldVisit.setVisitedAt(newVisit.getVisitedAt());
        if (newVisit.getMark() != null) oldVisit.setMark(newVisit.getMark());

        return oldVisit;
    }
}
