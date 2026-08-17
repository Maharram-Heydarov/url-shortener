package com.example.urlshortener.config;

import com.example.urlshortener.auth.dto.AuthResponse;
import com.example.urlshortener.auth.service.AuthService;
import com.example.urlshortener.redirect.service.RedirectService;
import com.example.urlshortener.shorturl.service.ShortUrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityConfigIntegrationTest.TestBeans.class)
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private RedirectService redirectService;

    @Test
    void registerIsPublic() throws Exception {
        when(authService.register(any()))
                .thenReturn(new AuthResponse(
                        "token",
                        "Bearer",
                        1L,
                        "security@example.com"
                ));

        String body = """
                {
                  "email": "security-%s@example.com",
                  "password": "Secret123!"
                }
                """.formatted(System.nanoTime());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void loginIsPublic() throws Exception {
        when(authService.login(any()))
                .thenReturn(new AuthResponse(
                        "token",
                        "Bearer",
                        1L,
                        "security@example.com"
                ));

        String body = """
                {
                  "email": "security@example.com",
                  "password": "Secret123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void redirectIsPublic() throws Exception {
        String shortCode = "security-" + System.nanoTime();
        when(redirectService.resolveTargetUrl(shortCode))
                .thenReturn(URI.create("https://example.com"));

        mockMvc.perform(get("/r/" + shortCode))
                .andExpect(status().isFound());
    }

    @Test
    void managementUrlsRequireJwt() throws Exception {
        mockMvc.perform(get("/api/v1/urls"))
                .andExpect(status().is(HttpStatus.UNAUTHORIZED.value()));
    }

    @Test
    void unmatchedEndpointIsNotImplicitlyPublic() throws Exception {
        mockMvc.perform(get("/internal/not-declared-" + System.nanoTime()))
                .andExpect(status().is(HttpStatus.UNAUTHORIZED.value()));
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        @Primary
        AuthService mockAuthService() {
            return mock(AuthService.class);
        }

        @Bean
        @Primary
        RedirectService mockRedirectService() {
            return mock(RedirectService.class);
        }

        @Bean
        @Primary
        ShortUrlService mockShortUrlService() {
            return mock(ShortUrlService.class);
        }
    }
}
