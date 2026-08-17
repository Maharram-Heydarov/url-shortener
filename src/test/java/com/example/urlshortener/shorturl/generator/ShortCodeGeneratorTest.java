package com.example.urlshortener.shorturl.generator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortCodeGeneratorTest {

    private static final String BASE62_PATTERN = "^[0-9A-Za-z]{7}$";

    private final ShortCodeGenerator shortCodeGenerator =
            new ShortCodeGenerator();

    @Test
    void generatedCodeUsesSevenBase62Characters() {
        for (int i = 0; i < 100; i++) {
            String code = shortCodeGenerator.generate();

            assertEquals(7, code.length());
            assertTrue(code.matches(BASE62_PATTERN));
        }
    }
}
