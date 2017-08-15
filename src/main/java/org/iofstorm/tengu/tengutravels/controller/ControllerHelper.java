package org.iofstorm.tengu.tengutravels.controller;

import org.iofstorm.tengu.tengutravels.model.Entity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ControllerHelper {
    private static final Entity EMPTY = new Entity();
    private static final ResponseEntity<Entity> OK_EMPTY_RESPONSE = ResponseEntity.ok(EMPTY);
    private static final ResponseEntity<Entity> BAD_REQUEST_RESPONSE = ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    private static final ResponseEntity<Entity> NOT_FOUND_RESPONSE = ResponseEntity.status(HttpStatus.NOT_FOUND).build();

    ResponseEntity<Entity> okEmptyResponse() {
        return OK_EMPTY_RESPONSE;
    }

    ResponseEntity<Entity> badRequestResponse() {
        return BAD_REQUEST_RESPONSE;
    }

    ResponseEntity<Entity> notFoundResponse() {
        return NOT_FOUND_RESPONSE;
    }
}
