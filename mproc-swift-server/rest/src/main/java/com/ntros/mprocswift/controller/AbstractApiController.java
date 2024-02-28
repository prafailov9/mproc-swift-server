package com.ntros.mprocswift.controller;

import com.ntros.mprocswift.handler.RestExceptionHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.Executor;

@RestController
public abstract class AbstractApiController {

    protected static final Logger log = LoggerFactory.getLogger(AbstractApiController.class);

    @Autowired
    @Qualifier("taskExecutor")
    protected Executor executor;



    protected <T> ResponseEntity<?> handleResponseAsync(T obj, Throwable ex) {
        if (ex == null) {
            return ResponseEntity.ok(obj);
        }
        log.error(ex.getMessage(), ex.getCause());
        return RestExceptionHandlerRegistry.handleException(ex.getCause());
    }

    protected <T> ResponseEntity<?> handleResponseAsync(T obj, Throwable ex, HttpStatus failureHttpStatus) {
        if (ex == null) {
            return ResponseEntity.ok(obj);
        }
        log.error(ex.getMessage(), ex);

        return ResponseEntity.status(failureHttpStatus).body(Map.of("error", ex.getMessage()));
    }

}
