package com.ntros.mprocswift.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.function.Function;

public class GenericExceptionHandler implements RestExceptionHandler {
    private final Class<?> exceptionType;
    private final HttpStatus status;
    private final Function<Throwable, String> messageProvider;


    private GenericExceptionHandler(Class<?> exceptionType, HttpStatus status, Function<Throwable, String> messageProvider) {
        this.exceptionType = exceptionType;
        this.status = status;
        this.messageProvider = messageProvider;
    }

    public static GenericExceptionHandler of(Class<?> exceptionType, HttpStatus status, Function<Throwable, String> messageProvider) {
        return new GenericExceptionHandler(exceptionType, status, messageProvider);
    }


    @Override
    public boolean supports(Class<?> exceptionType) {
        return this.exceptionType.equals(exceptionType);
    }

    @Override
    public ResponseEntity<?> handle(Throwable exception) {
        String message = messageProvider.apply(exception);
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
