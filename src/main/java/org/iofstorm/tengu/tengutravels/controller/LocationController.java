package org.iofstorm.tengu.tengutravels.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iofstorm.tengu.tengutravels.model.Location;
import org.iofstorm.tengu.tengutravels.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.iofstorm.tengu.tengutravels.Constants.MAX_AGE;
import static org.iofstorm.tengu.tengutravels.Constants.MIN_AGE;
import static org.iofstorm.tengu.tengutravels.Constants.NOT_FOUND;
import static org.iofstorm.tengu.tengutravels.Constants.OK;
import static org.iofstorm.tengu.tengutravels.Constants.VISITED_AT_MAX;
import static org.iofstorm.tengu.tengutravels.Constants.VISITED_AT_MIN;

@Controller
@RequestMapping("/locations")
public class LocationController {
    private static final Logger log = LoggerFactory.getLogger(LocationController.class);

    private final LocationService locationService;
    private final ControllerHelper controllerHelper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public LocationController(LocationService locationService, ControllerHelper controllerHelper) {
        this.locationService = Objects.requireNonNull(locationService);
        this.controllerHelper = Objects.requireNonNull(controllerHelper);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{locationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Future<ResponseEntity<String>> getLocationAsync(@PathVariable("locationId") Integer locationId) throws JsonProcessingException {
        Future<ResponseEntity<String>> res;
        if (locationId == null) {
            res = controllerHelper.futureBadRequest();
        } else {
            res = CompletableFuture.supplyAsync(() -> locationService.getLocation(locationId))
                    .thenApply(locationOptional -> {
                        ResponseEntity<String> response;
                        if (locationOptional.isPresent()) {
                            String location;
                            try {
                                location = objectMapper.writeValueAsString(locationOptional.get());
                                response = ResponseEntity.ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .contentLength(location.getBytes().length)
                                        .body(location);
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

    @RequestMapping(method = RequestMethod.POST, path = "/new", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Future<ResponseEntity<String>> createLocationAsync(@RequestBody Location location) {
        return locationService.createLocationAsync(location).thenApply(code -> Objects.equals(code, OK) ? controllerHelper.okEmpty() : controllerHelper.badRequest());
    }

    @RequestMapping(method = RequestMethod.POST, path = "/{locationId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Future<ResponseEntity<String>> updateLocationAsync(@PathVariable("locationId") Integer locationId, @RequestBody Map<String, Object> location) {
        Future<ResponseEntity<String>> res;
        if (locationId == null) {
            res = controllerHelper.futureBadRequest();
        } else {
            res = locationService.updateLocationAsync(locationId, location).thenApply(code -> {
                ResponseEntity<String> response;
                if (Objects.equals(code, OK)) response = controllerHelper.okEmpty();
                else if (Objects.equals(code, NOT_FOUND)) response = controllerHelper.notFound();
                else response = controllerHelper.badRequest();
                return response;
            });
        }
        return res;
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{locationId}/avg", produces = MediaType.APPLICATION_JSON_VALUE)
    public Future<ResponseEntity<String>> getAverageMarkAsync(@PathVariable("locationId") Integer locationId,
                                                              @RequestParam(value = "fromDate", required = false) Long fromDate,
                                                              @RequestParam(value = "toDate", required = false) Long toDate,
                                                              @RequestParam(value = "fromAge", required = false) Integer fromAge,
                                                              @RequestParam(value = "toAge", required = false) Integer toAge,
                                                              @RequestParam(value = "gender", required = false) String gender) {
        Future<ResponseEntity<String>> res;
        if (!validateGetAverageMarkParams(locationId, fromDate, toDate, fromAge, toAge, gender)) {
            res = controllerHelper.futureBadRequest();
        } else {
            res = locationService.getAverageMarkAsync(locationId, fromDate, toDate, fromAge, toAge, gender)
                    .thenApply(markOptional -> {
                        ResponseEntity<String> response;
                        if (markOptional.isPresent()) {
                            try {
                                String mark = objectMapper.writeValueAsString(markOptional.get());
                                response = ResponseEntity.ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .contentLength(mark.getBytes().length)
                                        .body(mark);
                            } catch (JsonProcessingException e) {
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

    @RequestMapping(path = "/ping", method = RequestMethod.GET)
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("ok");
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<String> handleMyException(Exception exception, HttpServletRequest request) {
        return controllerHelper.notFound();
    }

    private boolean validateGetAverageMarkParams(Integer locId, Long fromDate, Long toDate, Integer fromAge, Integer toAge, String gender) {
        if (locId == null) {
            return false;
        }
        if (fromDate != null && (fromDate < VISITED_AT_MIN || fromDate > VISITED_AT_MAX)) {
            return false;
        }
        if (toDate != null && (toDate < VISITED_AT_MIN || toDate > VISITED_AT_MAX)) {
            return false;
        }
        if (fromAge != null && (fromAge < MIN_AGE || fromAge > MAX_AGE)) {
            return false;
        }
        if (toAge != null && (toAge < MIN_AGE || toAge > MAX_AGE)) {
            return false;
        }
        if (gender != null && (!"m".equals(gender) && !"f".equals(gender))) {
            return false;
        }
        return true;
    }
}
