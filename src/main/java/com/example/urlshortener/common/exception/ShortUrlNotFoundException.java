package com.example.urlshortener.common.exception;

public class ShortUrlNotFoundException extends RuntimeException {

    public ShortUrlNotFoundException(String shortCode) {
        super("Short URL not found: " + shortCode);
    }

    public ShortUrlNotFoundException(Long id) {
        super("Short URL not found: " + id);
    }
}
