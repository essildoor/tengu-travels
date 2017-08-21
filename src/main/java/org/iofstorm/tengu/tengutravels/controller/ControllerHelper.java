package org.iofstorm.tengu.tengutravels.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Component
public class ControllerHelper {
    // response codes
    public static final Integer OK = 200;
    public static final Integer BAD_REQUEST = 400;
    public static final Integer NOT_FOUND = 404;

    private static ResponseEntity<String> OK_EMPTY_RESPONSE = ResponseEntity.ok().contentLength(2).contentType(MediaType.APPLICATION_JSON).header(HttpHeaders.CONNECTION, "keep-alive").body("{}");
    private static final ResponseEntity<String> BAD_REQUEST_RESPONSE = ResponseEntity.status(HttpStatus.BAD_REQUEST).header(HttpHeaders.CONNECTION, "keep-alive").build();
    private static final ResponseEntity<String> NOT_FOUND_RESPONSE = ResponseEntity.status(HttpStatus.NOT_FOUND).header(HttpHeaders.CONNECTION, "keep-alive").build();
    private static final Future<ResponseEntity<String>> FUTURE_BAD_REQUEST = CompletableFuture.completedFuture(BAD_REQUEST_RESPONSE);
    private static final Future<ResponseEntity<String>> FUTURE_NOT_FOUND = CompletableFuture.completedFuture(NOT_FOUND_RESPONSE);

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

    Future<ResponseEntity<String>> futureNotFound() {
        return FUTURE_NOT_FOUND;
    }


}
