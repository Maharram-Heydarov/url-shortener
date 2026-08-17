package com.example.urlshortener.shorturl.service;

import com.example.urlshortener.auth.entity.User;
import com.example.urlshortener.auth.security.CurrentUserService;
import com.example.urlshortener.common.exception.AliasAlreadyExistsException;
import com.example.urlshortener.common.exception.InvalidUrlException;
import com.example.urlshortener.common.exception.ShortUrlAccessDeniedException;
import com.example.urlshortener.redirect.cache.ShortUrlCacheInvalidationService;
import com.example.urlshortener.redirect.cache.ShortUrlCacheService;
import com.example.urlshortener.shorturl.dto.request.CreateShortUrlRequest;
import com.example.urlshortener.shorturl.dto.request.UpdateShortUrlRequest;
import com.example.urlshortener.shorturl.dto.response.ShortUrlResponse;
import com.example.urlshortener.shorturl.entity.ShortUrl;
import com.example.urlshortener.shorturl.entity.ShortUrlStatus;
import com.example.urlshortener.shorturl.generator.ShortCodeGenerator;
import com.example.urlshortener.shorturl.mapper.ShortUrlMapper;
import com.example.urlshortener.shorturl.repository.ShortUrlRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ShortUrlServiceTest {

    private ShortUrlRepository shortUrlRepository;
    private CurrentUserService currentUserService;
    private EntityManager entityManager;
    private ShortUrlCacheService shortUrlCacheService;
    private ShortUrlMapper shortUrlMapper;
    private ShortUrlService shortUrlService;

    @BeforeEach
    void setUp() {
        shortUrlRepository = mock(ShortUrlRepository.class);
        currentUserService = mock(CurrentUserService.class);
        entityManager = mock(EntityManager.class);
        shortUrlCacheService = mock(ShortUrlCacheService.class);
        shortUrlMapper = mock(ShortUrlMapper.class);

        shortUrlService = new ShortUrlService(
                shortUrlRepository,
                mock(ShortCodeGenerator.class),
                shortUrlMapper,
                currentUserService,
                new ShortUrlCacheInvalidationService(shortUrlCacheService),
                entityManager
        );
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void createRejectsNonHttpUrl() {
        CreateShortUrlRequest request = new CreateShortUrlRequest(
                "ftp://example.com/file",
                null,
                null
        );

        assertThrows(
                InvalidUrlException.class,
                () -> shortUrlService.create(request)
        );
        verifyNoInteractions(currentUserService);
    }

    @Test
    void createRejectsUrlWithoutHost() {
        CreateShortUrlRequest request = new CreateShortUrlRequest(
                "https:///missing-host",
                null,
                null
        );

        assertThrows(
                InvalidUrlException.class,
                () -> shortUrlService.create(request)
        );
        verifyNoInteractions(currentUserService);
    }

    @Test
    void createWithDuplicateCustomAliasThrowsConflict() {
        when(currentUserService.currentUserId()).thenReturn(1L);
        when(entityManager.getReference(User.class, 1L))
                .thenReturn(User.builder().id(1L).build());
        when(shortUrlRepository.existsByShortCode("taken"))
                .thenReturn(true);

        CreateShortUrlRequest request = new CreateShortUrlRequest(
                "https://example.com/products/1",
                "taken",
                LocalDateTime.now().plusDays(1)
        );

        assertThrows(
                AliasAlreadyExistsException.class,
                () -> shortUrlService.create(request)
        );
    }

    @Test
    void findByIdRejectsShortUrlOwnedByAnotherUser() {
        when(currentUserService.currentUserId()).thenReturn(2L);
        when(shortUrlRepository.findByIdAndUserId(1L, 2L))
                .thenReturn(Optional.empty());
        when(shortUrlRepository.existsById(1L)).thenReturn(true);

        assertThrows(
                ShortUrlAccessDeniedException.class,
                () -> shortUrlService.findById(1L)
        );
    }

    @Test
    void updateEvictsCacheOnlyAfterCommit() {
        ShortUrl shortUrl = ownedActiveShortUrl();
        when(currentUserService.currentUserId()).thenReturn(1L);
        when(shortUrlRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(shortUrl));
        when(shortUrlMapper.toResponse(shortUrl))
                .thenReturn(shortUrlResponse(shortUrl));

        TransactionSynchronizationManager.initSynchronization();

        shortUrlService.update(
                1L,
                new UpdateShortUrlRequest(
                        "https://example.org/updated",
                        null
                )
        );

        verify(shortUrlCacheService, never()).evict("abc1234");

        TransactionSynchronizationManager
                .getSynchronizations()
                .forEach(synchronization -> synchronization.afterCommit());

        verify(shortUrlCacheService).evict("abc1234");
    }

    @Test
    void updateRollbackDoesNotEvictCache() {
        ShortUrl shortUrl = ownedActiveShortUrl();
        when(currentUserService.currentUserId()).thenReturn(1L);
        when(shortUrlRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(shortUrl));
        when(shortUrlMapper.toResponse(shortUrl))
                .thenReturn(shortUrlResponse(shortUrl));

        TransactionSynchronizationManager.initSynchronization();

        shortUrlService.update(
                1L,
                new UpdateShortUrlRequest(
                        "https://example.org/updated",
                        null
                )
        );

        TransactionSynchronizationManager.clearSynchronization();

        verify(shortUrlCacheService, never()).evict(any());
    }

    @Test
    void disableEvictsCacheOnlyAfterCommit() {
        ShortUrl shortUrl = ownedActiveShortUrl();
        when(currentUserService.currentUserId()).thenReturn(1L);
        when(shortUrlRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(shortUrl));

        TransactionSynchronizationManager.initSynchronization();

        shortUrlService.disable(1L);

        verify(shortUrlCacheService, never()).evict("abc1234");

        TransactionSynchronizationManager
                .getSynchronizations()
                .forEach(synchronization -> synchronization.afterCommit());

        verify(shortUrlCacheService).evict("abc1234");
    }

    private ShortUrl ownedActiveShortUrl() {
        return ShortUrl.builder()
                .id(1L)
                .shortCode("abc1234")
                .originalUrl("https://example.com")
                .status(ShortUrlStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .clickCount(0L)
                .user(User.builder().id(1L).build())
                .build();
    }

    private ShortUrlResponse shortUrlResponse(ShortUrl shortUrl) {
        return new ShortUrlResponse(
                shortUrl.getId(),
                shortUrl.getShortCode(),
                "http://localhost:8081/r/" + shortUrl.getShortCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.getStatus(),
                shortUrl.getCreatedAt(),
                shortUrl.getExpiresAt()
        );
    }
}
