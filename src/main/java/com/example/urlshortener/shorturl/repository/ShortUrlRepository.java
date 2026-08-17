package com.example.urlshortener.shorturl.repository;

import com.example.urlshortener.shorturl.entity.ShortUrl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    Page<ShortUrl> findAllByUserId(Long userId, Pageable pageable);

    Optional<ShortUrl> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("""
            update ShortUrl shortUrl
            set shortUrl.clickCount = shortUrl.clickCount + 1,
                shortUrl.lastAccessedAt = :accessedAt
            where shortUrl.id = :id
            """)
    int incrementClickStats(
            @Param("id") Long id,
            @Param("accessedAt") LocalDateTime accessedAt
    );
}
