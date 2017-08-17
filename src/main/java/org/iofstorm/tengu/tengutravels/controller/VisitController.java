package org.iofstorm.tengu.tengutravels.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iofstorm.tengu.tengutravels.model.Visit;
import org.iofstorm.tengu.tengutravels.service.VisitService;
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

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.iofstorm.tengu.tengutravels.Constants.NOT_FOUND;
import static org.iofstorm.tengu.tengutravels.Constants.OK;

@Controller
@RequestMapping("/visits")
public class VisitController {
    private static final Logger log = LoggerFactory.getLogger(VisitController.class);

    private final VisitService visitService;
    private final ControllerHelper controllerHelper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public VisitController(VisitService visitService, ControllerHelper controllerHelper) {
        this.visitService = Objects.requireNonNull(visitService);
        this.controllerHelper = Objects.requireNonNull(controllerHelper);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{visitId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Future<ResponseEntity<String>> getVisitAsync(@PathVariable("visitId") Integer visitId) throws JsonProcessingException {
        Future<ResponseEntity<String>> res;
        if (visitId == null) {
            res = controllerHelper.futureBadRequest();
        } else {
            res = CompletableFuture.supplyAsync(() -> visitService.getVisit(visitId)).thenApply(visitOptional -> {
                ResponseEntity<String> response;
                if (visitOptional.isPresent()) {
                    String visit;
                    try {
                        visit = objectMapper.writeValueAsString(visitOptional.get());
                        response = ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .contentLength(visit.getBytes().length)
                                .body(visit);
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
    public Future<ResponseEntity<String>> createVisitAsync(@RequestBody Visit visit) {
        return visitService.createVisitAsync(visit).thenApply(code -> Objects.equals(code, OK) ? controllerHelper.okEmpty() : controllerHelper.badRequest());
    }

    @RequestMapping(method = RequestMethod.POST, path = "/{visitId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Future<ResponseEntity<String>> updateVisitAsync(@PathVariable("visitId") Integer visitId, @RequestBody Map<String, Object> visit) {
        Future<ResponseEntity<String>> res;
        if (visitId == null) {
            res = controllerHelper.futureBadRequest();
        } else {
            res = visitService.updateVisitAsync(visitId, visit).thenApply(code -> {
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
}
