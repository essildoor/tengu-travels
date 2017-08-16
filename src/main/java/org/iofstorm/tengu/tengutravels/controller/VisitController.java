package org.iofstorm.tengu.tengutravels.controller;

import org.iofstorm.tengu.tengutravels.model.Entity;
import org.iofstorm.tengu.tengutravels.model.Visit;
import org.iofstorm.tengu.tengutravels.service.VisitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Objects;

import static org.iofstorm.tengu.tengutravels.Constants.NOT_FOUND;
import static org.iofstorm.tengu.tengutravels.Constants.OK;

@Controller
@RequestMapping("/visits")
public class VisitController {

    private final VisitService visitService;
    private final ControllerHelper controllerHelper;

    @Autowired
    public VisitController(VisitService visitService, ControllerHelper controllerHelper) {
        this.visitService = Objects.requireNonNull(visitService);
        this.controllerHelper = Objects.requireNonNull(controllerHelper);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{visitId}")
    public ResponseEntity<Entity> getVisit(@PathVariable("visitId") Integer visitId) {
        if (visitId == null) return controllerHelper.badRequestResponse();
        return visitService.getVisit(visitId).<ResponseEntity<Entity>>map(ResponseEntity::ok).orElseGet(controllerHelper::badRequestResponse);
    }

    @RequestMapping(method = RequestMethod.POST, path = "/new")
    public ResponseEntity<Entity> createVisit(@RequestBody Visit visit) {
        return visitService.createVisit(visit) == OK ? controllerHelper.okEmptyResponse() : controllerHelper.badRequestResponse();
    }

    @RequestMapping(method = RequestMethod.POST, path = "/{visitId}")
    public ResponseEntity<Entity> updateVisit(@PathVariable("visitId") Integer visitId, @RequestBody Visit visit) {
        if (visitId == null) return controllerHelper.badRequestResponse();
        int status = visitService.updateVisit(visitId, visit);
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
}
