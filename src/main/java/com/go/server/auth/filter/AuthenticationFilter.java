package com.go.server.auth.filter;

import com.go.server.auth.user_details.CustomUserDetailsService;
import com.go.server.auth.jwt.JwtService;
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

@Component
public class AuthenticationFilter extends OncePerRequestFilter {
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
        final var authorizationHeader = extractAuthorizationHeader(request);
        final var hasBearerToken = authorizationHeader.startsWith("Bearer ");

        if (!hasBearerToken) {
            filterChain.doFilter(request, response);
            return;
        }

        final var accessToken = authorizationHeader.substring(7);
        final var username = jwtService.extractUsername(accessToken);

        if (username.isEmpty() || isAlreadyAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        final var userDetails = userDetailsService.loadUserByUsername(username.get());
        final var isTokenValid = userDetails.getToken().equals(accessToken);

        if (!jwtService.isTokenValid(accessToken, userDetails.getUsername()) || !isTokenValid) {
            filterChain.doFilter(request, response);
            return;
        }

        final var authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        filterChain.doFilter(request, response);
    }

    private boolean isAlreadyAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

    private String extractAuthorizationHeader(final HttpServletRequest request) {
        final var authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        return Objects.requireNonNullElse(authorizationHeader, "");
    }
}
