package com.example.urlshortener.shorturl.controller;

import com.example.urlshortener.common.response.PageResponse;
import com.example.urlshortener.shorturl.dto.request.CreateShortUrlRequest;
import com.example.urlshortener.shorturl.dto.request.UpdateShortUrlRequest;
import com.example.urlshortener.shorturl.dto.response.ShortUrlResponse;
import com.example.urlshortener.shorturl.service.ShortUrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
@Tag(
        name = "Short URLs",
        description = "Authenticated URL creation and management endpoints"
)
@SecurityRequirement(name = "bearerAuth")
public class ShortUrlController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT = "createdAt,desc";

    private final ShortUrlService shortUrlService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a short URL",
            description = "Creates a short URL for the authenticated user. Supports optional custom alias and optional expiration time. Creation is rate-limited per user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Short URL created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid URL, alias, or expiration date"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "409", description = "Custom alias already exists"),
            @ApiResponse(responseCode = "429", description = "Create URL rate limit exceeded"),
            @ApiResponse(responseCode = "500", description = "Unable to generate a unique short code")
    })
    public ShortUrlResponse create(
            @Valid @RequestBody CreateShortUrlRequest request
    ) {
        return shortUrlService.create(request);
    }

    @GetMapping
    @Operation(
            summary = "List my short URLs",
            description = "Returns a paginated list of short URLs that belong to the authenticated user only. Use sort format like createdAt,desc or shortCode,asc."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of short URLs returned"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid")
    })
    public PageResponse<ShortUrlResponse> findAll(
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size. Values above 100 are capped.", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(
                    description = "Sort format: property,direction. Allowed properties: createdAt, shortCode, originalUrl, status, expiresAt, clickCount, lastAccessedAt.",
                    example = "createdAt,desc"
            )
            @RequestParam(defaultValue = DEFAULT_SORT) String sort
    ) {
        return PageResponse.from(
                shortUrlService.findAll(toPageable(page, size, sort))
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get one short URL",
            description = "Returns one short URL by id after verifying that it belongs to the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Short URL returned"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Short URL belongs to another user"),
            @ApiResponse(responseCode = "404", description = "Short URL does not exist")
    })
    public ShortUrlResponse findById(
            @Parameter(description = "Database id of the short URL", example = "1")
            @PathVariable Long id
    ) {
        return shortUrlService.findById(id);
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Update a short URL",
            description = "Updates editable fields of an owned short URL, currently original URL and expiration time. Redis cache is invalidated when data changes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Short URL updated"),
            @ApiResponse(responseCode = "400", description = "Invalid URL or expiration date"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Short URL belongs to another user"),
            @ApiResponse(responseCode = "404", description = "Short URL does not exist")
    })
    public ShortUrlResponse update(
            @Parameter(description = "Database id of the short URL", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UpdateShortUrlRequest request
    ) {
        return shortUrlService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Disable a short URL",
            description = "Logically disables an owned short URL by setting status to DISABLED. The database row is kept and Redis cache is invalidated."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Short URL disabled"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Short URL belongs to another user"),
            @ApiResponse(responseCode = "404", description = "Short URL does not exist")
    })
    public void disable(
            @Parameter(description = "Database id of the short URL", example = "1")
            @PathVariable Long id
    ) {
        shortUrlService.disable(id);
    }

    private Pageable toPageable(
            int page,
            int size,
            String sort
    ) {
        return PageRequest.of(
                Math.max(page, DEFAULT_PAGE),
                normalizeSize(size),
                parseSort(sort)
        );
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }

        return Math.min(size, MAX_SIZE);
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",", 2);
        String property = allowedSortProperty(parts[0].trim());
        Sort.Direction direction = Sort.Direction.DESC;

        if (parts.length == 2) {
            direction = Sort.Direction.fromOptionalString(parts[1].trim())
                    .orElse(Sort.Direction.DESC);
        }

        return Sort.by(direction, property);
    }

    private String allowedSortProperty(String property) {
        return switch (property) {
            case "createdAt",
                 "shortCode",
                 "originalUrl",
                 "status",
                 "expiresAt",
                 "clickCount",
                 "lastAccessedAt" -> property;
            default -> "createdAt";
        };
    }
}
