package org.iofstorm.tengu.tengutravels.controller;

import com.google.gson.Gson;
import org.iofstorm.tengu.tengutravels.Utils;
import org.iofstorm.tengu.tengutravels.model.Gender;
import org.iofstorm.tengu.tengutravels.model.ShortVisits;
import org.iofstorm.tengu.tengutravels.model.User;
import org.iofstorm.tengu.tengutravels.service.UserService;
import org.iofstorm.tengu.tengutravels.service.VisitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static org.iofstorm.tengu.tengutravels.controller.ControllerHelper.NOT_FOUND;
import static org.iofstorm.tengu.tengutravels.controller.ControllerHelper.OK;
import static org.iofstorm.tengu.tengutravels.model.User.BIRTH_DATE;
import static org.iofstorm.tengu.tengutravels.model.User.BIRTH_DATE_MAX;
import static org.iofstorm.tengu.tengutravels.model.User.BIRTH_DATE_MIN;
import static org.iofstorm.tengu.tengutravels.model.User.EMAIL;
import static org.iofstorm.tengu.tengutravels.model.User.EMAIL_LENGTH;
import static org.iofstorm.tengu.tengutravels.model.User.FIRST_NAME;
import static org.iofstorm.tengu.tengutravels.model.User.GENDER;
import static org.iofstorm.tengu.tengutravels.model.User.ID;
import static org.iofstorm.tengu.tengutravels.model.User.LAST_NAME;
import static org.iofstorm.tengu.tengutravels.model.User.NAME_LENGTH;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private VisitService visitService;
    @Autowired
    private ControllerHelper controllerHelper;
    @Autowired
    private Gson gson;
    @Autowired
    private Utils utils;

    @RequestMapping(method = RequestMethod.GET, path = "/{userId}")
    public ResponseEntity<String> getUser(@PathVariable("userId") Integer userId) {
        if (userId == null) return controllerHelper.badRequest();
        User user = userService.getUserWithoutLock(userId);
        if (user == null) return controllerHelper.notFound();
        String userStr = gson.toJson(user);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(userStr.getBytes().length)
                .body(userStr);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{userId}/visits", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> getUserVisits(@PathVariable("userId") Integer userId,
                                                @RequestParam(value = "fromDate", required = false) Long fromDate,
                                                @RequestParam(value = "toDate", required = false) Long toDate,
                                                @RequestParam(value = "country", required = false) String country,
                                                @RequestParam(value = "toDistance", required = false) Integer toDistance) {
        if (userId == null) return controllerHelper.badRequest();
        ShortVisits shortVisits = visitService.getUserVisits(userId, fromDate, toDate, country, toDistance);
        if (shortVisits == null) return controllerHelper.notFound();
        String visits = gson.toJson(shortVisits);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(visits.getBytes().length)
                .body(visits);
    }

    @RequestMapping(method = RequestMethod.POST, path = "/new", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createUser(@RequestBody User user) {
        if (!validateOnCreate(user)) return controllerHelper.badRequest();
        int code = userService.createUser(user);
        return code == OK ? controllerHelper.okEmpty() : controllerHelper.badRequest();
    }

    @RequestMapping(method = RequestMethod.POST, path = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateUser(@PathVariable("userId") Integer userId, @RequestBody Map<String, String> userProps) {
        if (userId == null) return controllerHelper.badRequest();
        User newUser = validateUpdate(userProps);
        int code = userService.updateUser(userId, newUser);
        if (code == OK) return controllerHelper.okEmpty();
        else if (code == NOT_FOUND) return controllerHelper.notFound();
        else return controllerHelper.badRequest();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleMyException(Exception exception, HttpServletRequest request) {
        if (exception instanceof HttpMessageNotReadableException) return controllerHelper.badRequest();
        if (exception instanceof MethodArgumentTypeMismatchException) {
            String basePath = request.getServletPath();
            int i = request.getServletPath().indexOf('?');
            if (i != -1) basePath = basePath.substring(0, i);
            if (!basePath.matches("/users/[0-9]+[/]?(visits|new)?")) return controllerHelper.notFound();
        }
        return controllerHelper.badRequest();
    }

    private User validateUpdate(Map<String, String> userProps) {
        User user = new User();
        if (userProps.containsKey(ID)) return null;
        if (userProps.containsKey(EMAIL)) {
            String o = userProps.get(EMAIL);
            if (o != null) {
                if (o.length() > EMAIL_LENGTH) return null;
                else user.setEmail(o);
            } else {
                return null;
            }
        }
        if (userProps.containsKey(FIRST_NAME)) {
            String o = userProps.get(FIRST_NAME);
            if (o != null) {
                if (o.length() > NAME_LENGTH) return null;
                else user.setFirstName(o);
            } else {
                return null;
            }
        }
        if (userProps.containsKey(LAST_NAME)) {
            String o = userProps.get(LAST_NAME);
            if (o != null) {
                if (o.length() > NAME_LENGTH) return null;
                else user.setLastName(o);
            } else {
                return null;
            }
        }
        if (userProps.containsKey(GENDER)) {
            String o = userProps.get(GENDER);
            if (o != null) {
                if (utils.notMorF(o)) return null;
                else user.setGender(Gender.fromString(o));
            } else {
                return null;
            }
        }
        if (userProps.containsKey(BIRTH_DATE)) {
            String o = userProps.get(BIRTH_DATE);
            if (o != null) {
                try {
                    long bd = Long.valueOf(o);
                    if (bd < BIRTH_DATE_MIN || bd > BIRTH_DATE_MAX) {
                        return null;
                    } else {
                        user.setBirthDate(bd);
                        user.setAge(utils.calcAge(bd));
                    }
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                return null;
            }
        }

        return user;
    }

    private boolean validateOnCreate(User user) {
        if (user == null) return false;
        if (user.getId() == null) return false;
        if (user.getEmail() == null || (user.getEmail() != null && user.getEmail().length() > EMAIL_LENGTH))
            return false;
        if (user.getFirstName() == null || (user.getFirstName() != null && user.getFirstName().length() > NAME_LENGTH))
            return false;
        if (user.getLastName() == null || (user.getLastName() != null && user.getLastName().length() > NAME_LENGTH))
            return false;
        if (user.getBirthDate() == Long.MIN_VALUE || (user.getBirthDate() < BIRTH_DATE_MIN || user.getBirthDate() > BIRTH_DATE_MAX)) {
            return false;
        } else {
            user.setAge(utils.calcAge(user.getBirthDate()));
        }
        if (user.getGender() == null || (user.getGender() != null && user.getGender() == Gender.UNKNOWN)) return false;
        return true;
    }
}
