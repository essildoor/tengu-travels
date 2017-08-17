package org.iofstorm.tengu.tengutravels.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Component
public class ControllerHelper {
    private static ResponseEntity<String> OK_EMPTY_RESPONSE = ResponseEntity.ok().contentLength(2).contentType(MediaType.APPLICATION_JSON).body("{}");
    private static final ResponseEntity<String> BAD_REQUEST_RESPONSE = ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    private static final ResponseEntity<String> NOT_FOUND_RESPONSE = ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    private static final Future<ResponseEntity<String>> FUTURE_BAD_REQUEST = CompletableFuture.completedFuture(BAD_REQUEST_RESPONSE);

    ResponseEntity<String> okEmpty() {
        return OK_EMPTY_RESPONSE;
    }

    ResponseEntity<String> badRequest() {
        return BAD_REQUEST_RESPONSE;
    }

    ResponseEntity<String> notFound() {
        return NOT_FOUND_RESPONSE;
    }

    Future<ResponseEntity<String>> futureBadRequest() {
        return FUTURE_BAD_REQUEST;
    }
}
