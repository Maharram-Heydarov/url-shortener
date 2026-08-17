package com.example.urlshortener.redirect.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ShortUrlCacheInvalidationServiceTest {

    private ShortUrlCacheService shortUrlCacheService;
    private ShortUrlCacheInvalidationService invalidationService;

    @BeforeEach
    void setUp() {
        shortUrlCacheService = mock(ShortUrlCacheService.class);
        invalidationService = new ShortUrlCacheInvalidationService(
                shortUrlCacheService
        );
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void evictsAfterCommitWhenTransactionSynchronizationIsActive() {
        TransactionSynchronizationManager.initSynchronization();

        invalidationService.evictAfterCommit("abc1234");

        verify(shortUrlCacheService, never()).evict("abc1234");

        TransactionSynchronizationManager
                .getSynchronizations()
                .forEach(synchronization -> synchronization.afterCommit());

        verify(shortUrlCacheService).evict("abc1234");
    }

    @Test
    void rollbackDoesNotEvictWhenTransactionSynchronizationIsActive() {
        TransactionSynchronizationManager.initSynchronization();

        invalidationService.evictAfterCommit("abc1234");
        TransactionSynchronizationManager.clearSynchronization();

        verify(shortUrlCacheService, never()).evict("abc1234");
    }

    @Test
    void evictsImmediatelyWhenNoTransactionSynchronizationIsActive() {
        invalidationService.evictAfterCommit("abc1234");

        verify(shortUrlCacheService).evict("abc1234");
    }
}
