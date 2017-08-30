package org.iofstorm.tengu.tengutravels.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import org.iofstorm.tengu.tengutravels.model.Visit;
import org.iofstorm.tengu.tengutravels.service.LocationService;
import org.iofstorm.tengu.tengutravels.service.UserService;
import org.iofstorm.tengu.tengutravels.service.VisitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static org.iofstorm.tengu.tengutravels.controller.ControllerHelper.OK;
import static org.iofstorm.tengu.tengutravels.model.Location.VISITED_AT_MAX;
import static org.iofstorm.tengu.tengutravels.model.Location.VISITED_AT_MIN;
import static org.iofstorm.tengu.tengutravels.model.Visit.ID;
import static org.iofstorm.tengu.tengutravels.model.Visit.LOCATION_ID;
import static org.iofstorm.tengu.tengutravels.model.Visit.MARK;
import static org.iofstorm.tengu.tengutravels.model.Visit.USER_ID;
import static org.iofstorm.tengu.tengutravels.model.Visit.VISITED_AT;

@Controller
@RequestMapping("/visits")
public class VisitController {
    private static final Logger log = LoggerFactory.getLogger(VisitController.class);

    @Autowired
    private VisitService visitService;
    @Autowired
    private UserService userService;
    @Autowired
    private LocationService locationService;
    @Autowired
    private ControllerHelper controllerHelper;
    @Autowired
    private Gson gson;

    @RequestMapping(method = RequestMethod.GET, path = "/{visitId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getVisit(@PathVariable("visitId") Integer visitId) throws JsonProcessingException {
        if (visitId == null) return controllerHelper.badRequest();
        Visit visit = visitService.getVisitWithoutLock(visitId);
        if (visit == null) return controllerHelper.notFound();
        String body = gson.toJson(visit);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(body.getBytes().length)
                .body(body);
    }

    @RequestMapping(method = RequestMethod.POST, path = "/new", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createVisit(@RequestBody Visit visit) {
        if (!validateOnCreate(visit)) return controllerHelper.badRequest();
        int code = visitService.createVisit(visit);
        return code == OK ? controllerHelper.okEmpty() : controllerHelper.badRequest();
    }

    @RequestMapping(method = RequestMethod.POST, path = "/{visitId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateVisit(@PathVariable("visitId") Integer visitId, @RequestBody Map<String, String> visitProps) {
        if (visitId == null) return controllerHelper.badRequest();
        if (!visitService.visitExist(visitId)) return controllerHelper.notFound();
        Visit visit = validateOnUpdate(visitProps);
        if (visit == null) return controllerHelper.badRequest();
        visitService.updateVisit(visitId, visit);
        return controllerHelper.okEmpty();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleMyException(Exception exception, HttpServletRequest request) {
        if (exception instanceof HttpMessageNotReadableException) return controllerHelper.badRequest();
        if (exception instanceof MethodArgumentTypeMismatchException) {
            String basePath = request.getServletPath();
            int i = request.getServletPath().indexOf('?');
            if (i != -1) basePath = basePath.substring(0, i);
            if (!basePath.matches("/visits/[0-9]+[/]?(new)?")) return controllerHelper.notFound();
        }
        return controllerHelper.badRequest();
    }

    private boolean validateOnCreate(Visit visit) {
        if (visit == null) return false;
        if (visit.getId() == null) return false;
        if (visit.getLocationId() == null) return false;
        if (visit.getUserId() == null) return false;
        if (visit.getVisitedAt() < VISITED_AT_MIN || visit.getVisitedAt() > VISITED_AT_MAX) return false;
        if (visit.getMark() < 0 || visit.getMark() > 5) return false;
        return true;
    }

    private Visit validateOnUpdate(Map<String, String> visitProperties) {
        if (visitProperties.containsKey(ID)) return null;
        if (visitProperties.containsKey(LOCATION_ID)) {
            String o = visitProperties.get(LOCATION_ID);
            if (o == null) return null;
            try {
                Integer.valueOf(o);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (visitProperties.containsKey(USER_ID)) {
            String o = visitProperties.get(USER_ID);
            if (o == null) return null;
            try {
                Integer.valueOf(o);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (visitProperties.containsKey(VISITED_AT)) {
            String o = visitProperties.get(VISITED_AT);
            if (o == null) return null;
            Long va;
            try {
                va = Long.valueOf(o);
                if (va < VISITED_AT_MIN || va > VISITED_AT_MAX) return null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (visitProperties.containsKey(MARK)) {
            String o = visitProperties.get(MARK);
            if (o == null) return null;
            Integer m;
            try {
                m = Integer.valueOf(o);
                if (m < 0 || m > 5) return null;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // all is fine here, huh
        Visit visit = new Visit();
        if (visitProperties.containsKey(LOCATION_ID))
            visit.setLocation(locationService.getLocationWithoutLock(Integer.valueOf(visitProperties.get(LOCATION_ID))));
        if (visitProperties.containsKey(USER_ID))
            visit.setUser(userService.getUserWithoutLock(Integer.valueOf(visitProperties.get(USER_ID))));
        if (visitProperties.containsKey(VISITED_AT)) visit.setVisitedAt(Long.valueOf(visitProperties.get(VISITED_AT)));
        if (visitProperties.containsKey(MARK)) visit.setMark(Integer.valueOf(visitProperties.get(MARK)));
        return visit;
    }
}
