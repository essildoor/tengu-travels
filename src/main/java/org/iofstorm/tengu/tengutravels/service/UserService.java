package org.iofstorm.tengu.tengutravels.service;

import org.iofstorm.tengu.tengutravels.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import static org.iofstorm.tengu.tengutravels.Constants.BIRTH_DATE_MAX;
import static org.iofstorm.tengu.tengutravels.Constants.BIRTH_DATE_MIN;
import static org.iofstorm.tengu.tengutravels.Constants.EMAIL_LENGTH;
import static org.iofstorm.tengu.tengutravels.Constants.NAME_LENGTH;
import static org.iofstorm.tengu.tengutravels.Constants.NOT_FOUND;
import static org.iofstorm.tengu.tengutravels.Constants.OK;
import static org.iofstorm.tengu.tengutravels.model.User.BIRTH_DATE;
import static org.iofstorm.tengu.tengutravels.model.User.EMAIL;
import static org.iofstorm.tengu.tengutravels.model.User.FIRST_NAME;
import static org.iofstorm.tengu.tengutravels.model.User.GENDER;
import static org.iofstorm.tengu.tengutravels.model.User.ID;
import static org.iofstorm.tengu.tengutravels.model.User.LAST_NAME;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final Map<Integer, User> users;
    private final ReadWriteLock lock;
    private final VisitService visitService;

    public UserService(VisitService visitService) {
        users = new HashMap<>(50_000);
        lock = new ReentrantReadWriteLock(true);
        this.visitService = Objects.requireNonNull(visitService);
    }

    public Optional<User> getUser(Integer id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(users.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    public CompletableFuture<Integer> createUserAsync(User user) {
        return CompletableFuture.supplyAsync(() -> {
            if (!validateOnCreate(user)) return BAD_REQUEST;
            lock.readLock().lock();
            try {
                if (users.containsKey(user.getId())) return BAD_REQUEST;
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    if (users.containsKey(user.getId())) {
                        return BAD_REQUEST;
                    } else {
                        users.put(user.getId(), user);
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

    // updates user async, call async updating on visits for this user
    public CompletableFuture<Integer> updateUserAsync(Integer userId, Map<String, Object> userUpdate) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                if (!users.containsKey(userId)) {
                    return NOT_FOUND;
                } else {
                    lock.readLock().unlock();
                    lock.writeLock().lock();
                    try {
                        if (!users.containsKey(userId)) {
                            return NOT_FOUND;
                        } else {
                            if (validateUpdate(userUpdate)) {
                                users.compute(userId, (id, oldUser) -> remapUser(oldUser, userUpdate));
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
                visitService.updateVisitWithUserAsync(userId, userUpdate).get();
            } catch (InterruptedException | ExecutionException e) {
                return BAD_REQUEST;
            }
            return code;
        });
    }

    // used for data loading
    public void load(List<User> userList) {
        lock.writeLock().lock();
        try {
            userList.forEach(usr -> users.put(usr.getId(), usr));
        } finally {
            lock.writeLock().unlock();
        }
    }

    // used for data loading
    Map<Integer, User> getUsers(Set<Integer> ids) {
        final Map<Integer, User> res = new HashMap<>(ids.size(), 1f);
        lock.readLock().lock();
        try {
            for (Integer id : ids) res.put(id, users.get(id));
        } finally {
            lock.readLock().unlock();
        }
        return res;
    }

    private User remapUser(User oldUser, Map<String, Object> newUser) {
        if (newUser.containsKey(EMAIL)) oldUser.setEmail((String) newUser.get(EMAIL));
        if (newUser.containsKey(FIRST_NAME)) oldUser.setFirstName((String) newUser.get(FIRST_NAME));
        if (newUser.containsKey(LAST_NAME)) oldUser.setLastName((String) newUser.get(LAST_NAME));
        if (newUser.containsKey(BIRTH_DATE)) oldUser.setBirthDate((Long) newUser.get(BIRTH_DATE));
        if (newUser.containsKey(GENDER)) oldUser.setGender((String) newUser.get(GENDER));
        return oldUser;
    }

    private boolean validateUpdate(Map<String, Object> user) {
        if (user.containsKey(ID)) return false;
        if (user.containsKey(EMAIL)) {
            Object o = user.get(EMAIL);
            if (o == null || !(o instanceof String) || ((String) o).length() > EMAIL_LENGTH) return false;
        }
        if (user.containsKey(FIRST_NAME)) {
            Object o = user.get(FIRST_NAME);
            if (o == null || !(o instanceof String) || ((String) o).length() > NAME_LENGTH) return false;
        }
        if (user.containsKey(LAST_NAME)) {
            Object o = user.get(LAST_NAME);
            if (o == null || !(o instanceof String) || ((String) o).length() > NAME_LENGTH) return false;
        }
        if (user.containsKey(GENDER)) {
            Object o = user.get(GENDER);
            if (o == null || !(o instanceof String) || notMorF((String) o) ) return false;
        }
        if (user.containsKey(BIRTH_DATE)) {
            Object o = user.get(BIRTH_DATE);
            if (o == null || !(o instanceof Long) || ((Long)o < BIRTH_DATE_MIN || (Long)o > BIRTH_DATE_MAX)) return false;
        }
        return true;
    }

    private boolean validateOnCreate(User user) {
        if (user == null) {
//            log.debug("user validation failed: user is null");
            return false;
        }
        if (user.getId() == null) {
//            log.debug("user validation failed: id={}, isCreate={}", user.getId(), true);
            return false;
        }
        if (user.getEmail() == null || (user.getEmail() != null && user.getEmail().length() > EMAIL_LENGTH)) {
//            log.debug("user validation failed: email={}, isCreate={}", user.getEmail(), true);
            return false;
        }
        if (user.getFirstName() == null || (user.getFirstName() != null && user.getFirstName().length() > NAME_LENGTH)) {
//            log.debug("user validation failed: firstName={}, isCreate={}", user.getFirstName(), true);
            return false;
        }
        if (user.getLastName() == null || (user.getLastName() != null && user.getLastName().length() > NAME_LENGTH)) {
//            log.debug("user validation failed: lastName={}, isCreate={}", user.getLastName(), true);
            return false;
        }
        if (user.getBirthDate() == null || (user.getBirthDate() != null && (user.getBirthDate() < BIRTH_DATE_MIN || user.getBirthDate() > BIRTH_DATE_MAX))) {
//            log.debug("user validation failed: birthDate={}, isCreate={}", user.getBirthDate(), true);
            return false;
        }
        if (user.getGender() == null || (user.getGender() != null && notMorF(user.getGender()))) {
            log.debug("user validation failed: gender={}, isCreate={}", user.getGender(), true);
            return false;
        }
        return true;
    }

    private boolean notMorF(String s) {
        return !"m".equalsIgnoreCase(s) && !"f".equalsIgnoreCase(s);
    }
}
