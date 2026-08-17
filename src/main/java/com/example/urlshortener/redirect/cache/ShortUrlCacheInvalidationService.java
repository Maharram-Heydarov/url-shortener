package com.example.urlshortener.redirect.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class ShortUrlCacheInvalidationService {

    private final ShortUrlCacheService shortUrlCacheService;

    public void evictAfterCommit(String shortCode) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {

                        @Override
                        public void afterCommit() {
                            shortUrlCacheService.evict(shortCode);
                        }
                    }
            );

            return;
        }

        shortUrlCacheService.evict(shortCode);
    }
}
