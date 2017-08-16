package org.iofstorm.tengu.tengutravels.controller;

import org.iofstorm.tengu.tengutravels.model.Entity;
import org.iofstorm.tengu.tengutravels.model.User;
import org.iofstorm.tengu.tengutravels.service.UserService;
import org.iofstorm.tengu.tengutravels.service.VisitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.concurrent.Future;

import static org.iofstorm.tengu.tengutravels.Constants.COUNTRY_LENGTH;
import static org.iofstorm.tengu.tengutravels.Constants.NOT_FOUND;
import static org.iofstorm.tengu.tengutravels.Constants.OK;
import static org.iofstorm.tengu.tengutravels.Constants.VISITED_AT_MAX;
import static org.iofstorm.tengu.tengutravels.Constants.VISITED_AT_MIN;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final VisitService visitService;
    private final ControllerHelper controllerHelper;

    @Autowired
    public UserController(UserService userService, VisitService visitService, ControllerHelper controllerHelper) {
        this.userService = Objects.requireNonNull(userService);
        this.visitService = Objects.requireNonNull(visitService);
        this.controllerHelper = Objects.requireNonNull(controllerHelper);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{userId}")
    public ResponseEntity<Entity> getUser(@PathVariable("userId") Integer userId) {
        if (userId == null) return controllerHelper.badRequestResponse();
        return userService.getUser(userId).<ResponseEntity<Entity>>map(ResponseEntity::ok).orElseGet(controllerHelper::badRequestResponse);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{id}/visits")
    public Future<ResponseEntity<Entity>> getUserVisits(@PathVariable("userId") Integer userId,
                                                        @RequestParam("fromDate") Long fromDate,
                                                        @RequestParam("toDate") Long toDate,
                                                        @RequestParam("country") String country,
                                                        @RequestParam("toDistance") Integer toDistance) {
        if (!validateGetUserVisitsParams(userId, fromDate, toDate, country)) return controllerHelper.futureBadRequest();
        return visitService.getVisits(userId, fromDate, toDate, country, toDistance);
    }

    @RequestMapping(method = RequestMethod.POST, path = "/new")
    public ResponseEntity<Entity> createUser(@RequestBody User user) {
        return userService.createUser(user) == OK ? controllerHelper.okEmptyResponse() : controllerHelper.badRequestResponse();
    }

    @RequestMapping(method = RequestMethod.POST, path = "/{userId}")
    public ResponseEntity<Entity> updateUser(@PathVariable("userId") Integer userId, @RequestBody User user) {
        if (userId == null) return controllerHelper.badRequestResponse();
        int status = userService.updateUser(userId, user);
        ResponseEntity<Entity> response;
        switch (status) {
            case OK: {
                response = ResponseEntity.ok(new Entity());
                break;
            }
            case NOT_FOUND: {
                response = controllerHelper.notFoundResponse();
                break;
            }
            default:
                response = controllerHelper.badRequestResponse();
        }
        return response;
    }

    private boolean validateGetUserVisitsParams(Integer userId, Long fromDate, Long toDate, String country) {
        if (userId == null) return false;
        if (fromDate != null && (fromDate < VISITED_AT_MIN || fromDate > VISITED_AT_MAX)) return false;
        if (toDate != null && (toDate < VISITED_AT_MIN || toDate > VISITED_AT_MAX)) return false;
        if (fromDate != null && toDate != null && fromDate > toDate) return false;
        if (country != null && country.length() > COUNTRY_LENGTH) return false;
        return true;
    }
}
