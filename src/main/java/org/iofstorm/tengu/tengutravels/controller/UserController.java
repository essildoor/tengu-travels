package org.iofstorm.tengu.tengutravels.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iofstorm.tengu.tengutravels.model.User;
import org.iofstorm.tengu.tengutravels.service.UserService;
import org.iofstorm.tengu.tengutravels.service.VisitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.iofstorm.tengu.tengutravels.Constants.NOT_FOUND;
import static org.iofstorm.tengu.tengutravels.Constants.OK;

@RestController
@RequestMapping("/users")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserService userService;
    private final VisitService visitService;
    private final ControllerHelper controllerHelper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public UserController(UserService userService, VisitService visitService, ControllerHelper controllerHelper) {
        this.userService = Objects.requireNonNull(userService);
        this.visitService = Objects.requireNonNull(visitService);
        this.controllerHelper = Objects.requireNonNull(controllerHelper);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{userId}")
    public Future<ResponseEntity<String>> getUserAsync(@PathVariable("userId") Integer userId) throws JsonProcessingException {
        Future<ResponseEntity<String>> res;
        if (userId == null) {
            res = controllerHelper.futureBadRequest();
        } else {
            res = CompletableFuture.supplyAsync(() -> userService.getUser(userId))
                    .thenApply(userOptional -> {
                        ResponseEntity<String> response;
                        if (userOptional.isPresent()) {
                            String user;
                            try {
                                user = objectMapper.writeValueAsString(userOptional.get());
                                response = ResponseEntity.status(HttpStatus.OK)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .contentLength(user.getBytes().length)
                                        .body(user);
                            } catch (JsonProcessingException e) {
                                log.error("json serialization error {}", e.getMessage());
                                response = controllerHelper.badRequest();
                            }
                        } else {
                            response = controllerHelper.notFound();
                        }
                        return response;
                    });
        }
        return res;
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{userId}/visits", produces = MediaType.APPLICATION_JSON_VALUE)
    public Future<ResponseEntity<String>> getUserVisitsAsync(@PathVariable("userId") Object userId,
                                                             @RequestParam(value = "fromDate", required = false) Object fromDate,
                                                             @RequestParam(value = "toDate", required = false) Object toDate,
                                                             @RequestParam(value = "country", required = false) Object country,
                                                             @RequestParam(value = "toDistance", required = false) Object toDistance) {
        Future<ResponseEntity<String>> res;
        if (!validateGetUserVisitsParams(userId, fromDate, toDate, country, toDistance)) {
            res = controllerHelper.futureBadRequest();
        } else {
            res = visitService.getUserVisitsAsync(Integer.valueOf((String) userId), (Long) fromDate, (Long) toDate, (String) country, (Integer) toDistance)
                    .thenApply(visitsOptional -> {
                        ResponseEntity<String> response;
                        if (visitsOptional.isPresent()) {
                            try {
                                String visits = objectMapper.writeValueAsString(visitsOptional.get());
                                response = ResponseEntity.ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .contentLength(visits.getBytes().length)
                                        .body(visits);
                            } catch (JsonProcessingException e) {
                                log.error("json serialization error {}", e.getMessage());
                                response = controllerHelper.badRequest();
                            }
                        } else {
                            response = controllerHelper.notFound();
                        }
                        return response;
                    });
        }
        return res;
    }

    @RequestMapping(method = RequestMethod.POST, path = "/new", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Future<ResponseEntity<String>> createUserAsync(@RequestBody User user) {
        return userService.createUserAsync(user).thenApply(code -> Objects.equals(code, OK) ? controllerHelper.okEmpty() : controllerHelper.badRequest());
    }

    @RequestMapping(method = RequestMethod.POST, path = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Future<ResponseEntity<String>> updateUserAsync(@PathVariable("userId") Integer userId, @RequestBody Map<String, Object> user) {
        Future<ResponseEntity<String>> res;
        if (userId == null) {
            res = controllerHelper.futureBadRequest();
        } else {
            res = userService.updateUserAsync(userId, user).thenApply(code -> {
                ResponseEntity<String> response;
                if (Objects.equals(code, OK)) response = controllerHelper.okEmpty();
                else if (Objects.equals(code, NOT_FOUND)) response = controllerHelper.notFound();
                else response = controllerHelper.badRequest();
                return response;
            });
        }
        return res;
    }

    @RequestMapping(path = "/ping", method = RequestMethod.GET)
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("ok");
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<String> handleMyException(Exception exception, HttpServletRequest request) {
        return controllerHelper.notFound();
    }

    private boolean validateGetUserVisitsParams(Object userId, Object fromDate, Object toDate, Object country, Object toDistance) {
        if (userId == null) {
            return false;
        } else {
            try {
                Integer.valueOf((String) userId);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if (fromDate != null && !(fromDate instanceof Long)) {
            return false;
        }
        if (toDate != null && !(toDate instanceof Long)) {
            return false;
        }
        if (country != null && !(country instanceof String)) {
            return false;
        }
        if (toDistance != null && !(toDistance instanceof Integer)) return false;
        return true;
    }
}
