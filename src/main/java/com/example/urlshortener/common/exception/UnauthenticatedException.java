package com.example.urlshortener.common.exception;

public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException() {
        super("Authentication is required");
    }
}
