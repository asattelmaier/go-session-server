package com.go.server.auth.filter;

import com.go.server.auth.jwt.JwtService;
import com.go.server.auth.user_details.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final static Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
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

        if (accessToken.isPresent() && !isAlreadyAuthenticated()) {
            processTokenAuthentication(accessToken.get(), request);
        }

        filterChain.doFilter(request, response);
    }

    private void processTokenAuthentication(final String token, final HttpServletRequest request) {
        try {
            final Optional<String> username = jwtService.extractUsername(token);

            if (username.isEmpty()) {
                return;
            }

            final var userDetails = userDetailsService.loadUserByUsername(username.get());

            if (jwtService.isTokenValid(token, userDetails.getUsername()) && userDetails.getToken().equals(token)) {
                final var authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.debug("Successfully authenticated user: {}", username.get());
            }

        } catch (Exception e) {
            logger.warn("Authentication failed: {}", e.getMessage());
            // Don't throw, just don't set authentication.
            // Spring Security will handle the unauthorized access.
        }
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
