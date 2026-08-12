package com.example.urlshortener.redirect.controller;

import com.example.urlshortener.redirect.service.RedirectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Tag(
        name = "Redirect",
        description = "Public short-code redirect endpoint"
)
public class RedirectController {

    private final RedirectService redirectService;

    @GetMapping("/r/{shortCode}")
    @Operation(
            summary = "Redirect to original URL",
            description = "Public endpoint that resolves a short code through Redis cache and PostgreSQL, records a click, and returns 302 Found with the original URL in the Location header."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirects to the original URL"),
            @ApiResponse(responseCode = "404", description = "Short code does not exist"),
            @ApiResponse(responseCode = "410", description = "Short URL is expired or disabled")
    })
    public ResponseEntity<Void> redirect(
            @Parameter(description = "Short code or custom alias", example = "abc1234")
            @PathVariable String shortCode
    ) {

        URI targetUrl =
                redirectService.resolveTargetUrl(shortCode);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(targetUrl)
                .build();
    }
}
