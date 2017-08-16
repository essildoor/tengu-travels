package org.iofstorm.tengu.tengutravels.service;

import org.iofstorm.tengu.tengutravels.model.User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.iofstorm.tengu.tengutravels.Constants.BAD_REQUEST;
import static org.iofstorm.tengu.tengutravels.Constants.NOT_FOUND;
import static org.iofstorm.tengu.tengutravels.Constants.OK;

@Service
public class UserService {

    private final Map<Integer, User> users;
    private final ReadWriteLock lock;

    public UserService() {
        users = new HashMap<>(10_000, .6f);
        lock = new ReentrantReadWriteLock(true);
    }

    public boolean exist(Integer id) {
        lock.readLock().lock();
        try {
            return users.containsKey(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<User> getUser(Integer id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(users.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    public int createUser(User user) {
        if (validate(user, true)) return BAD_REQUEST;
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

    public int updateUser(Integer userId, User user) {
        if (!exist(userId)) return NOT_FOUND;
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
                        if (validate(user, false)) {
                            users.compute(userId, (id, oldUser) -> remapUser(oldUser, user));
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

    public CompletableFuture<List<User>> getUsers(Long birthDateFrom, Long birthDateTo, String gender) {
        return CompletableFuture.supplyAsync(() -> {
            Stream<User> us = users.values().stream();
            if (birthDateFrom != null) us = us.filter(usr -> usr.getBirthDate() >= birthDateFrom);
            if (birthDateTo != null) us = us.filter(usr -> usr.getBirthDate() <= birthDateTo);
            if (gender != null) us = us.filter(usr -> usr.getGender().equals(gender));
            return us.collect(Collectors.toList());
        });
    }

    private User remapUser(User oldUser, User newUser) {
        if (newUser.getEmail() != null) oldUser.setEmail(newUser.getEmail());
        if (newUser.getFirstName() != null) oldUser.setFirstName(newUser.getFirstName());
        if (newUser.getLastName() != null) oldUser.setLastName(newUser.getLastName());
        if (newUser.getBirthDate() != null) oldUser.setBirthDate(newUser.getBirthDate());
        if (newUser.getGender() != null) oldUser.setGender(newUser.getGender());
        return oldUser;
    }

    public boolean validate(User user, boolean isCreate) {
        if (user == null) {
            return false;
        }
        // validate id
        if ((!isCreate && user.getId() != null) || (isCreate && user.getId() == null)) {
            return false;
        }
        // validate email
        if ((isCreate && user.getEmail() == null) || user.getEmail().length() > 100) {
            return false;
        }
        // validate firstName
        if ((isCreate && user.getFirstName() == null) || user.getFirstName().length() > 50) {
            return false;
        }
        // validate lastName
        if ((isCreate && user.getLastName() == null) || user.getLastName().length() > 50) {
            return false;
        }
        // validate gender
        if ((isCreate && user.getGender() == null) || notMorF(user.getGender())) {
            return false;
        }
        return true;
    }

    private boolean notMorF(String s) {
        return !"m".equalsIgnoreCase(s) && !"f".equalsIgnoreCase(s);
    }
}
