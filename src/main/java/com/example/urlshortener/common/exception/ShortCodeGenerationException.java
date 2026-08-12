package com.example.urlshortener.common.exception;

public class ShortCodeGenerationException extends RuntimeException {

    public ShortCodeGenerationException() {
        super("Unable to generate unique short code");
    }
}