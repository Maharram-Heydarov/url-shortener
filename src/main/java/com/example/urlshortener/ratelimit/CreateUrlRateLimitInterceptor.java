package com.example.urlshortener.ratelimit;

import com.example.urlshortener.auth.security.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class CreateUrlRateLimitInterceptor implements HandlerInterceptor {

    private final CurrentUserService currentUserService;
    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {

        if (HttpMethod.POST.matches(request.getMethod())) {
            rateLimitService.checkCreateUrlLimit(
                    currentUserService.currentUserId()
            );
        }

        return true;
    }
}
