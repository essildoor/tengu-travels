package org.iofstorm.tengu.tengutravels.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.iofstorm.tengu.tengutravels.Utils;
import org.iofstorm.tengu.tengutravels.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final Map<Integer, User> users;
    private final ReadWriteLock lock;
    private final ObjectMapper objectMapper;
    private final Utils utils;

    public UserService(ObjectMapper objectMapper, Utils utils) {
        users = new HashMap<>();
        lock = new ReentrantReadWriteLock(true);
        this.objectMapper = objectMapper;
        this.utils = utils;
    }

    public User getUser(Integer id) {
        if (id == null) return null;
        lock.readLock().lock();
        try {
            return users.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean userExist(Integer id) {
        lock.readLock().lock();
        try {
            return users.containsKey(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Integer createUser(User user) {
        lock.readLock().lock();
        try {
            if (users.containsKey(user.getId())) return BAD_REQUEST;
            lock.readLock().unlock();
            lock.writeLock().lock();
            try {
                if (users.containsKey(user.getId())) {
                    return BAD_REQUEST;
                } else {
                    utils.setCachedResponse(user, objectMapper);
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

    public Integer updateUser(Integer userId, User userUpdate) {
        lock.readLock().lock();
        try {
            if (!users.containsKey(userId)) return NOT_FOUND;

            lock.readLock().unlock();
            lock.writeLock().lock();
            try {
                if (!users.containsKey(userId)) return NOT_FOUND;
                if (userUpdate == null) return BAD_REQUEST;

                users.compute(userId, (id, oldUser) -> remapUser(oldUser, userUpdate));
                return OK;
            } finally {
                lock.readLock().lock();
                lock.writeLock().unlock();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    // used for data loading
    public Integer load(List<User> userList) {
        lock.writeLock().lock();
        try {
            userList.forEach(usr -> {
                usr.setAge(utils.calcAge(usr.getBirthDate()));
                utils.setCachedResponse(usr, objectMapper);
                users.put(usr.getId(), usr);
            });
        } finally {
            lock.writeLock().unlock();
        }
        return userList.size();
    }

    // used for data loading
    Map<Integer, User> getUsers(Set<Integer> ids) {
        Map<Integer, User> res = new HashMap<>(ids.size());
        for (Integer id : ids) res.put(id, users.get(id));
        return res;
    }

    private User remapUser(User oldUser, User newUser) {
        if (newUser.getEmail() != null) oldUser.setEmail(newUser.getEmail());
        if (newUser.getFirstName() != null) oldUser.setFirstName(newUser.getFirstName());
        if (newUser.getLastName() != null) oldUser.setLastName(newUser.getLastName());
        if (newUser.getBirthDate() != null) {
            oldUser.setBirthDate(newUser.getBirthDate());
            oldUser.setAge(newUser.getAge());
        }
        if (newUser.getGender() != null) oldUser.setGender(newUser.getGender());

        utils.setCachedResponse(oldUser, objectMapper);

        return oldUser;
    }

    // for testing
    void addUserForTest(User user) {
        users.put(user.getId(), user);
    }
}
