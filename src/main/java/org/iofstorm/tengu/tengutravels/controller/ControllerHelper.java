package org.iofstorm.tengu.tengutravels.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ControllerHelper {
    public static final Integer OK = 200;
    public static final Integer BAD_REQUEST = 400;
    public static final Integer NOT_FOUND = 404;

    private static ResponseEntity<String> OK_EMPTY_RESPONSE = ResponseEntity.ok().contentLength(2).contentType(MediaType.APPLICATION_JSON).body("{}");
    private static final ResponseEntity<String> BAD_REQUEST_RESPONSE =  ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    private static final ResponseEntity<String> NOT_FOUND_RESPONSE =    ResponseEntity.status(HttpStatus.NOT_FOUND).build();

    ResponseEntity<String> okEmpty() {
        return OK_EMPTY_RESPONSE;
    }

    ResponseEntity<String> badRequest() {
        return BAD_REQUEST_RESPONSE;
    }

    ResponseEntity<String> notFound() {
        return NOT_FOUND_RESPONSE;
    }
}
