package com.example.urlshortener.common.exception;

public class ShortUrlAccessDeniedException extends RuntimeException {

    public ShortUrlAccessDeniedException(Long id) {
        super("Access denied for short URL: " + id);
    }
}
