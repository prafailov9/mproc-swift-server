package com.ntros.mprocswift.handler;

import org.springframework.http.ResponseEntity;

public interface RestExceptionHandler {

    boolean supports(Class<?> type);
    ResponseEntity<?> handle(Throwable ex);
}
