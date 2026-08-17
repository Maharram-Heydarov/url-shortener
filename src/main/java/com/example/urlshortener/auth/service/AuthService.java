package com.example.urlshortener.auth.service;

import com.example.urlshortener.auth.dto.AuthResponse;
import com.example.urlshortener.auth.dto.LoginRequest;
import com.example.urlshortener.auth.dto.RegisterRequest;
import com.example.urlshortener.auth.entity.User;
import com.example.urlshortener.auth.repository.UserRepository;
import com.example.urlshortener.auth.security.JwtService;
import com.example.urlshortener.common.exception.EmailAlreadyExistsException;
import com.example.urlshortener.common.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .createdAt(LocalDateTime.now())
                .build();

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return toAuthResponse(savedUser);

        } catch (DataIntegrityViolationException exception) {

            if (userRepository.existsByEmail(email)) {
                throw new EmailAlreadyExistsException(email);
            }

            throw exception;
        }
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordMatches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                TOKEN_TYPE,
                user.getId(),
                user.getEmail()
        );
    }

    private boolean passwordMatches(
            String rawPassword,
            String encodedPassword
    ) {
        try {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String normalizeEmail(String email) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
