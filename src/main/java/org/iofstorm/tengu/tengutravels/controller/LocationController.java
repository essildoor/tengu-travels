package org.iofstorm.tengu.tengutravels.controller;

import com.google.gson.Gson;
import org.iofstorm.tengu.tengutravels.Utils;
import org.iofstorm.tengu.tengutravels.model.Gender;
import org.iofstorm.tengu.tengutravels.model.Location;
import org.iofstorm.tengu.tengutravels.model.Mark;
import org.iofstorm.tengu.tengutravels.service.LocationService;
import org.iofstorm.tengu.tengutravels.service.VisitService;
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

import static org.iofstorm.tengu.tengutravels.controller.ControllerHelper.NOT_FOUND;
import static org.iofstorm.tengu.tengutravels.controller.ControllerHelper.OK;
import static org.iofstorm.tengu.tengutravels.model.Location.CITY;
import static org.iofstorm.tengu.tengutravels.model.Location.CITY_LENGTH;
import static org.iofstorm.tengu.tengutravels.model.Location.COUNTRY;
import static org.iofstorm.tengu.tengutravels.model.Location.COUNTRY_LENGTH;
import static org.iofstorm.tengu.tengutravels.model.Location.DISTANCE;
import static org.iofstorm.tengu.tengutravels.model.Location.ID;
import static org.iofstorm.tengu.tengutravels.model.Location.PLACE;

@Controller
@RequestMapping("/locations")
public class LocationController {
    @Autowired
    private Gson gson;

    @Autowired
    private VisitService visitService;

    @Autowired
    private LocationService locationService;

    @Autowired
    private Utils utils;

    @Autowired
    private ControllerHelper controllerHelper;

    @RequestMapping(method = RequestMethod.GET, path = "/{locationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getLocation(@PathVariable("locationId") Integer locationId) {
        if (locationId == null) return controllerHelper.badRequest();
        Location location = locationService.getLocationWithoutLock(locationId);
        if (location == null) return controllerHelper.notFound();
        String body = gson.toJson(location);
        return ResponseEntity.ok()
                .contentLength(body.getBytes().length)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @RequestMapping(method = RequestMethod.POST, path = "/new", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createLocation(@RequestBody Location location) {
        if (!validateOnCreate(location)) return controllerHelper.badRequest();
        int code = locationService.createLocation(location);
        return code == OK ? controllerHelper.okEmpty() : controllerHelper.badRequest();
    }

    @RequestMapping(method = RequestMethod.POST, path = "/{locationId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateLocation(@PathVariable("locationId") Integer locationId, @RequestBody Map<String, String> location) {
        if (locationId == null) return controllerHelper.badRequest();

        Location newLocation = validateOnUpdate(location);
        int code = locationService.updateLocation(locationId, newLocation);

        if (code == OK) return controllerHelper.okEmpty();
        if (code == NOT_FOUND) return controllerHelper.notFound();
        else return controllerHelper.badRequest();
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{locationId}/avg", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAverageMark(@PathVariable("locationId") Integer locationId,
                                                 @RequestParam(value = "fromDate", required = false) Long fromDate,
                                                 @RequestParam(value = "toDate", required = false) Long toDate,
                                                 @RequestParam(value = "fromAge", required = false) Integer fromAge,
                                                 @RequestParam(value = "toAge", required = false) Integer toAge,
                                                 @RequestParam(value = "gender", required = false) String gender) {
        if (locationId == null) return controllerHelper.badRequest();
        if (gender != null && utils.notMorF(gender)) return controllerHelper.badRequest();
        Mark mark = locationService.getAverageMark(locationId, fromDate, toDate, fromAge, toAge, Gender.fromString(gender));
        if (mark == null) return controllerHelper.notFound();
        String body = gson.toJson(mark);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(body.getBytes().length)
                .body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleMyException(Exception exception, HttpServletRequest request) {
        return controllerHelper.badRequest();
    }

    private boolean validateOnCreate(Location location) {
        if (location == null) return false;
        if (location.getId() == null) return false;
        if (location.getPlace() == null) return false;
        if (location.getCountry() == null || (location.getCountry() != null && location.getCountry().length() > COUNTRY_LENGTH))
            return false;
        if (location.getCity() == null || (location.getCity() != null && location.getCity().length() > CITY_LENGTH))
            return false;
        if (location.getDistance() == 0) return false;
        return true;
    }

    private Location validateOnUpdate(Map<String, String> loc) {
        Location location = new Location();
        if (loc.containsKey(ID)) return null;
        if (loc.containsKey(PLACE)) {
            if (loc.get(PLACE) == null) return null;
            else location.setPlace(loc.get(PLACE));
        }
        if (loc.containsKey(COUNTRY)) {
            String o = loc.get(COUNTRY);
            if (o == null || o.length() > COUNTRY_LENGTH) return null;
            else location.setCountry(o);
        }
        if (loc.containsKey(CITY)) {
            String o = loc.get(CITY);
            if (o == null || o.length() > CITY_LENGTH) return null;
            else location.setCity(o);
        }
        if (loc.containsKey(DISTANCE)) {
            String o = loc.get(DISTANCE);
            if (o == null) return null;
            try {
                location.setDistance(Integer.valueOf(o));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return location;
    }
}
