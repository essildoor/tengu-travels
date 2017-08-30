package org.iofstorm.tengu.tengutravels.service;

import org.iofstorm.tengu.tengutravels.Utils;
import org.iofstorm.tengu.tengutravels.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.iofstorm.tengu.tengutravels.controller.ControllerHelper.BAD_REQUEST;
import static org.iofstorm.tengu.tengutravels.controller.ControllerHelper.NOT_FOUND;
import static org.iofstorm.tengu.tengutravels.controller.ControllerHelper.OK;

@Service
public class UserService {
    public static final Map<Integer, User> users = new HashMap<>(1_041_000, 1f);
    private final ReadWriteLock lock;
    private final Utils utils;

    private VisitService visitService;

    public UserService(Utils utils) {
        lock = new ReentrantReadWriteLock(true);
        this.utils = utils;
    }

    @Autowired
    public void setVisitService(VisitService visitService) {
        this.visitService = visitService;
    }

    public ReadWriteLock getLock() {
        return lock;
    }

    public User getUserWithoutLock(Integer id) {
        return users.get(id);
    }

    public boolean userExist(Integer id) {
        lock.readLock().lock();
        try {
            return users.containsKey(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int createUser(User user) {
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
    }

    @SuppressWarnings("NumberEquality")
    public int updateUser(Integer userId, User userUpdate) {
        lock.readLock().lock();
        try {
            if (!users.containsKey(userId)) return NOT_FOUND;

            lock.readLock().unlock();
            lock.writeLock().lock();
            visitService.getLock().writeLock().lock();
            try {
                if (!users.containsKey(userId)) return NOT_FOUND;
                if (userUpdate == null) return BAD_REQUEST;

                remapUser(users.get(userId), userUpdate);
                return OK;
            } finally {
                visitService.getLock().writeLock().unlock();
                lock.readLock().lock();
                lock.writeLock().unlock();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    // used for data loading
    public void load(List<User> userList) {
        for (User usr : userList) {
            usr.setAge(utils.calcAge(usr.getBirthDate()));
            users.put(usr.getId(), usr);
        }
    }

    private User remapUser(User oldUser, User newUser) {
        if (newUser.getEmail() != null) oldUser.setEmail(newUser.getEmail());
        if (newUser.getFirstName() != null) oldUser.setFirstName(newUser.getFirstName());
        if (newUser.getLastName() != null) oldUser.setLastName(newUser.getLastName());
        if (newUser.getBirthDate() != Long.MIN_VALUE) {
            oldUser.setBirthDate(newUser.getBirthDate());
            oldUser.setAge(newUser.getAge());
        }
        if (newUser.getGender() != null) oldUser.setGender(newUser.getGender());

        return oldUser;
    }

    // for testing
    void addUserForTest(User user) {
        users.put(user.getId(), user);
    }
}
