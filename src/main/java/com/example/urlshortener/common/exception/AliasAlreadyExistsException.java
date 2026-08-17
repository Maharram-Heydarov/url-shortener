package com.example.urlshortener.common.exception;

public class AliasAlreadyExistsException extends RuntimeException {

    public AliasAlreadyExistsException(String alias) {
        super("Alias already exists: " + alias);
    }
}