package org.iofstorm.tengu.tengutravels.controller;

import org.iofstorm.tengu.tengutravels.model.Entity;
import org.iofstorm.tengu.tengutravels.model.Location;
import org.iofstorm.tengu.tengutravels.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Objects;
import java.util.concurrent.Future;

import static org.iofstorm.tengu.tengutravels.Constants.MAX_AGE;
import static org.iofstorm.tengu.tengutravels.Constants.MIN_AGE;
import static org.iofstorm.tengu.tengutravels.Constants.NOT_FOUND;
import static org.iofstorm.tengu.tengutravels.Constants.OK;
import static org.iofstorm.tengu.tengutravels.Constants.VISITED_AT_MAX;
import static org.iofstorm.tengu.tengutravels.Constants.VISITED_AT_MIN;

@Controller
@RequestMapping("/location")
public class LocationController {

    private final LocationService locationService;
    private final ControllerHelper controllerHelper;

    @Autowired
    public LocationController(LocationService locationService, ControllerHelper controllerHelper) {
        this.locationService = Objects.requireNonNull(locationService);
        this.controllerHelper = Objects.requireNonNull(controllerHelper);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{locationId}")
    public ResponseEntity<Entity> getLocation(@PathVariable("locationId") Integer locationId) {
        if (locationId == null) return controllerHelper.badRequestResponse();
        return locationService.getLocation(locationId).<ResponseEntity<Entity>>map(ResponseEntity::ok).orElseGet(controllerHelper::badRequestResponse);
    }

    @RequestMapping(method = RequestMethod.POST, path = "/new")
    public ResponseEntity<Entity> createLocation(@RequestBody Location location) {
        return locationService.createLocation(location) == OK ? controllerHelper.okEmptyResponse() : controllerHelper.badRequestResponse();
    }

    @RequestMapping(method = RequestMethod.POST, path = "/{locationId}")
    public ResponseEntity<Entity> updateLocation(@PathVariable("locationId") Integer locationId, @RequestBody Location location) {
        if (locationId == null) return controllerHelper.badRequestResponse();
        int status = locationService.updateLocation(locationId, location);
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

    @RequestMapping(method = RequestMethod.GET, path = "/{locationId}/avg")
    public Future<ResponseEntity<Entity>> getAverageMark(@PathVariable("locationId") Integer locationId,
                                                       @RequestParam("fromDate") Long fromDate,
                                                       @RequestParam("toDate") Long toDate,
                                                       @RequestParam("fromAge") Integer fromAge,
                                                       @RequestParam("toAge") Integer toAge,
                                                       @RequestParam("gender") String gender) {
        if (!validateGetAverageMarkParams(locationId, fromDate, toDate, fromAge, toAge, gender)) controllerHelper.futureBadRequest();
        return locationService.getAverageMark(locationId, fromDate, toDate, fromAge, toAge, gender);
    }

    private boolean validateGetAverageMarkParams(Integer locId, Long fromDate, Long toDate, Integer fromAge, Integer toAge, String gender) {
        if (locId == null) return false;
        if (fromDate != null && (fromDate < VISITED_AT_MIN || fromDate > VISITED_AT_MAX)) return false;
        if (toDate != null && (toDate < VISITED_AT_MIN || toDate > VISITED_AT_MAX)) return false;
        if (fromDate != null && toDate != null && fromDate > toDate) return false;
        if (fromAge != null && (fromAge < MIN_AGE || fromAge > MAX_AGE)) return false;
        if (toAge != null && (toAge < MIN_AGE || toAge > MAX_AGE)) return false;
        if (fromAge != null && toAge != null && fromAge > toAge) return false;
        if (gender != null && (!"m".equals(gender) && !"f".equals(gender))) return false;
        return true;
    }
}
