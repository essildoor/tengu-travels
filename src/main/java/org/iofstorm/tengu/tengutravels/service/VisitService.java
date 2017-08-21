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
    private final Map<Integer, List<Visit>> visitsByUser;
    private final Map<Integer, List<Visit>> visitsByLocation;
    private final ReadWriteLock lock;

    @Autowired
    private UserService userService;

    @Autowired
    private LocationService locationService;

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
            lock.writeLock().lock();
            try {
                if (visits.containsKey(visit.getId())) return BAD_REQUEST; // visit already exist

                User user = userService.getUser(visit.getUserId());
                Location location = locationService.getLocation(visit.getLocationId());
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

            User newUser = userService.getUser(newVisit.getUserId());
            Location newLocation = locationService.getLocation(newVisit.getLocationId());

            if (newUser != null) visitsByUser.get(oldVisit.getUserId()).remove(oldVisit);
            if (newLocation != null) visitsByLocation.get(oldVisit.getLocationId()).remove(oldVisit);

            Visit updatedVisit = remapVisit(oldVisit, newVisit, newUser, newLocation);

            saveVisit(updatedVisit);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ShortVisits getUserVisits(Integer userId, Long fromDate, Long toDate, String country, Integer toDistance) {
        List<Visit> userVisits = getVisitsByUser(userId); // user r lock
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
    public void updateVisitWithLocation(Integer locId, Location loc) {
        lock.writeLock().lock();
        try {
            List<Visit> visitList = visitsByLocation.get(locId);
            if (CollectionUtils.isNotEmpty(visitList)) {
                for (Visit vst : visitsByLocation.get(locId)) {
                    if (loc.getCountry() != null) vst.setLocationCountry(loc.getCountry());
                    if (loc.getPlace() != null) vst.setLocationPlace(loc.getPlace());
                    if (loc.getDistance() != null) vst.setLocationDistance(loc.getDistance());
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // updates visits on user update
    public void updateVisitWithUser(Integer userId, User newUser) {
        lock.writeLock().lock();
        try {
            List<Visit> visitList = visitsByUser.get(userId);
            if (CollectionUtils.isNotEmpty(visitList)) {
                for (Visit vst : visitsByUser.get(userId)) {
                    if (newUser.getGender() != null) vst.setUserGender(newUser.getGender());
                    if (newUser.getBirthDate() != null) vst.setUserAge(newUser.getAge());
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
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
            if (!userService.userExist(userId)) return null; // user not found
            List<Visit> visitByUser = visitsByUser.get(userId);
            return visitByUser == null ? Collections.emptyList() : visitByUser;
        } finally {
            lock.readLock().unlock();
        }
    }

    private void saveVisit(Visit visit) {
        if (visit.getId().equals(2666)) {
            System.out.println();
        }
        visits.put(visit.getId(), visit);
        visitsByUser.compute(visit.getUserId(), (userId, list) -> {
            if (list == null) list = new ArrayList<>();
            list.add(visit);
            return list;
        });
        visitsByLocation.compute(visit.getLocationId(), (locId, list) -> {
            if (list == null) list = new ArrayList<>();
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
