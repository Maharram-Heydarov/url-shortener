package com.example.urlshortener.shorturl.mapper;

import com.example.urlshortener.shorturl.dto.response.ShortUrlResponse;
import com.example.urlshortener.shorturl.entity.ShortUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ShortUrlMapper {

    private final String baseUrl;

    public ShortUrlMapper(
            @Value("${app.base-url}") String baseUrl
    ) {
        this.baseUrl = baseUrl;
    }

    public ShortUrlResponse toResponse(ShortUrl shortUrl) {

        return new ShortUrlResponse(
                shortUrl.getId(),
                shortUrl.getShortCode(),
                baseUrl + "/r/" + shortUrl.getShortCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.getStatus(),
                shortUrl.getCreatedAt(),
                shortUrl.getExpiresAt()
        );
    }
}