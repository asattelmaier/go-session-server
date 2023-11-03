package com.go.server.auth.filter;

import com.go.server.auth.jwt.JwtService;
import com.go.server.auth.user_details.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {
    private final static String AUTHORIZATION_HEADER_TOKEN_PREFIX = "Bearer ";
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public AuthenticationFilter(final JwtService jwtService, final CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            final @NotNull HttpServletRequest request,
            final @NotNull HttpServletResponse response,
            final @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        final var accessToken = extractAccessTokenFromHeader(request);
        final var userDetails = accessToken.flatMap(jwtService::extractUsername).map(userDetailsService::loadUserByUsername);
        final var isSameToken = userDetails.map(details -> details.getToken().equals(accessToken.get()));
        final var isTokenValid = userDetails.map(details -> jwtService.isTokenValid(accessToken.get(), details.getUsername()));
        final var authToken = userDetails.map(details -> new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));

        if (isSameToken.isEmpty() || !isSameToken.get() || isTokenValid.isEmpty() || !isTokenValid.get() || isAlreadyAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        authToken.get().setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken.get());
        filterChain.doFilter(request, response);
    }

    private boolean isAlreadyAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

    private Optional<String> extractAccessTokenFromHeader(final HttpServletRequest request) {
        final var authorizationHeader = Objects.requireNonNullElse(request.getHeader(HttpHeaders.AUTHORIZATION), "");

        if (authorizationHeader.startsWith(AUTHORIZATION_HEADER_TOKEN_PREFIX)) {
            return Optional.of(authorizationHeader.substring(AUTHORIZATION_HEADER_TOKEN_PREFIX.length()));
        }

        return Optional.empty();
    }
}
