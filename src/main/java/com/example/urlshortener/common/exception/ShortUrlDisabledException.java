package com.example.urlshortener.common.exception;

public class ShortUrlDisabledException extends RuntimeException {

    public ShortUrlDisabledException(String shortCode) {
        super("Short URL is disabled: " + shortCode);
    }
}