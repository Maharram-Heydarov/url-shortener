package com.example.urlshortener.analytics.controller;

import com.example.urlshortener.analytics.dto.ShortUrlStatsResponse;
import com.example.urlshortener.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
@Tag(
        name = "Analytics",
        description = "Basic click statistics for owned short URLs"
)
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/{id}/stats")
    @Operation(
            summary = "Get short URL stats",
            description = "Returns basic analytics for an owned short URL: click count, creation time, last accessed time, expiration time, and status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stats returned"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Short URL belongs to another user"),
            @ApiResponse(responseCode = "404", description = "Short URL does not exist")
    })
    public ShortUrlStatsResponse getStats(
            @Parameter(description = "Database id of the short URL", example = "1")
            @PathVariable Long id
    ) {
        return analyticsService.getStats(id);
    }
}
