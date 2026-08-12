package com.example.urlshortener.shorturl.service;

import com.example.urlshortener.auth.entity.User;
import com.example.urlshortener.auth.security.CurrentUserService;
import com.example.urlshortener.common.exception.AliasAlreadyExistsException;
import com.example.urlshortener.common.exception.InvalidUrlException;
import com.example.urlshortener.common.exception.ShortUrlAccessDeniedException;
import com.example.urlshortener.common.exception.ShortCodeGenerationException;
import com.example.urlshortener.common.exception.ShortUrlNotFoundException;
import com.example.urlshortener.redirect.cache.ShortUrlCacheInvalidationService;
import com.example.urlshortener.shorturl.dto.request.CreateShortUrlRequest;
import com.example.urlshortener.shorturl.dto.request.UpdateShortUrlRequest;
import com.example.urlshortener.shorturl.dto.response.ShortUrlResponse;
import com.example.urlshortener.shorturl.entity.ShortUrl;
import com.example.urlshortener.shorturl.entity.ShortUrlStatus;
import com.example.urlshortener.shorturl.generator.ShortCodeGenerator;
import com.example.urlshortener.shorturl.mapper.ShortUrlMapper;
import com.example.urlshortener.shorturl.repository.ShortUrlRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ShortUrlService {

    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final ShortUrlRepository shortUrlRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final ShortUrlMapper shortUrlMapper;
    private final CurrentUserService currentUserService;
    private final ShortUrlCacheInvalidationService shortUrlCacheInvalidationService;
    private final EntityManager entityManager;

    public ShortUrlResponse create(CreateShortUrlRequest request) {

        String originalUrl =
                validateAndNormalizeUrl(request.originalUrl());
        User user = entityManager.getReference(
                User.class,
                currentUserService.currentUserId()
        );

        if (request.customAlias() != null
                && !request.customAlias().isBlank()) {

            return createWithCustomAlias(
                    originalUrl,
                    request.customAlias(),
                    request.expiresAt(),
                    user
            );
        }

        return createWithGeneratedCode(
                originalUrl,
                request.expiresAt(),
                user
        );
    }

    public Page<ShortUrlResponse> findAll(Pageable pageable) {
        return shortUrlRepository
                .findAllByUserId(
                        currentUserService.currentUserId(),
                        pageable
                )
                .map(shortUrlMapper::toResponse);
    }

    public ShortUrlResponse findById(Long id) {
        ShortUrl shortUrl = findOwnedShortUrl(
                id,
                currentUserService.currentUserId()
        );

        return shortUrlMapper.toResponse(shortUrl);
    }

    @Transactional
    public ShortUrlResponse update(
            Long id,
            UpdateShortUrlRequest request
    ) {
        ShortUrl shortUrl = findOwnedShortUrl(
                id,
                currentUserService.currentUserId()
        );

        boolean changed = false;

        if (request.originalUrl() != null) {
            shortUrl.setOriginalUrl(
                    validateAndNormalizeUrl(request.originalUrl())
            );
            changed = true;
        }

        if (request.expiresAt() != null) {
            shortUrl.setExpiresAt(request.expiresAt());
            changed = true;
        }

        if (changed) {
            shortUrlCacheInvalidationService.evictAfterCommit(
                    shortUrl.getShortCode()
            );
        }

        return shortUrlMapper.toResponse(shortUrl);
    }

    @Transactional
    public void disable(Long id) {
        ShortUrl shortUrl = findOwnedShortUrl(
                id,
                currentUserService.currentUserId()
        );

        if (shortUrl.getStatus() != ShortUrlStatus.DISABLED) {
            shortUrl.setStatus(ShortUrlStatus.DISABLED);
        }

        shortUrlCacheInvalidationService.evictAfterCommit(
                shortUrl.getShortCode()
        );
    }

    private ShortUrlResponse createWithCustomAlias(
            String originalUrl,
            String customAlias,
            LocalDateTime expiresAt,
            User user
    ) {

        String alias = customAlias.trim();

        if (shortUrlRepository.existsByShortCode(alias)) {
            throw new AliasAlreadyExistsException(alias);
        }

        try {
            return save(originalUrl, alias, expiresAt, user);

        } catch (DataIntegrityViolationException exception) {

            if (shortUrlRepository.existsByShortCode(alias)) {
                throw new AliasAlreadyExistsException(alias);
            }

            throw exception;
        }
    }

    private ShortUrlResponse createWithGeneratedCode(
            String originalUrl,
            LocalDateTime expiresAt,
            User user
    ) {

        for (int attempt = 0;
             attempt < MAX_GENERATION_ATTEMPTS;
             attempt++) {

            String shortCode = shortCodeGenerator.generate();

            if (shortUrlRepository.existsByShortCode(shortCode)) {
                continue;
            }

            try {
                return save(
                        originalUrl,
                        shortCode,
                        expiresAt,
                        user
                );

            } catch (DataIntegrityViolationException exception) {

                if (shortUrlRepository.existsByShortCode(shortCode)) {
                    continue;
                }

                throw exception;
            }
        }

        throw new ShortCodeGenerationException();
    }

    private ShortUrlResponse save(
            String originalUrl,
            String shortCode,
            LocalDateTime expiresAt,
            User user
    ) {

        ShortUrl shortUrl = ShortUrl.builder()
                .shortCode(shortCode)
                .originalUrl(originalUrl)
                .status(ShortUrlStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .clickCount(0L)
                .user(user)
                .build();

        ShortUrl savedShortUrl =
                shortUrlRepository.saveAndFlush(shortUrl);

        return shortUrlMapper.toResponse(savedShortUrl);
    }

    private ShortUrl findOwnedShortUrl(
            Long id,
            Long userId
    ) {
        return shortUrlRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> {
                    if (shortUrlRepository.existsById(id)) {
                        return new ShortUrlAccessDeniedException(id);
                    }

                    return new ShortUrlNotFoundException(id);
                });
    }

    private String validateAndNormalizeUrl(String rawUrl) {

        String url = rawUrl.trim();

        try {
            URI uri = URI.create(url);

            String scheme = uri.getScheme();

            boolean validScheme =
                    "http".equalsIgnoreCase(scheme)
                            || "https".equalsIgnoreCase(scheme);

            if (!validScheme || uri.getHost() == null) {
                throw new InvalidUrlException(url);
            }

            return uri.toString();

        } catch (IllegalArgumentException exception) {
            throw new InvalidUrlException(url);
        }
    }
}
