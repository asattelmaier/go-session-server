package com.go.server.auth;

import com.go.server.auth.exception.UserAlreadyExistsException;
import com.go.server.auth.exception.UserAuthenticationException;
import com.go.server.auth.jwt.JwtService;
import com.go.server.auth.model.input.AuthenticateUserDto;
import com.go.server.auth.model.input.RegisterUserDto;
import com.go.server.auth.model.output.TokensDto;
import com.go.server.auth.user_details.CustomUserDetails;
import com.go.server.user.model.Guest;
import com.go.server.user.model.User;
import com.go.server.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthenticationService {
    @Value("${application.security.guest-password}")
    private String guestPassword;

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(
            final UserRepository repository,
            final JwtService jwtService,
            final PasswordEncoder passwordEncoder,
            final AuthenticationManager authenticationManager
    ) {
        this.repository = repository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public TokensDto register(final RegisterUserDto dto) {
        final var password = passwordEncoder.encode(dto.password);
        final var accessToken = jwtService.generateAccessToken(dto.username);
        final var refreshToken = jwtService.generateRefreshToken(dto.username);
        final var user = new User(UUID.randomUUID(), dto.username, password, accessToken);

        if (repository.findByUsername(dto.username).isEmpty()) {
            repository.createUser(user);

            return new TokensDto(accessToken, refreshToken);
        }

        throw new UserAlreadyExistsException(dto.username);
    }

    public TokensDto registerGuest() {
        final var id = UUID.randomUUID();
        final var username = Guest.createGuestUserName(id);
        final var password = passwordEncoder.encode(guestPassword);
        final var accessToken = jwtService.generateAccessToken(username);
        final var refreshToken = jwtService.generateRefreshToken(username);
        final var user = new Guest(id, password, accessToken);

        repository.createUser(user);

        return new TokensDto(accessToken, refreshToken);
    }

    public TokensDto authenticate(AuthenticateUserDto dto) {
        final var user = repository.findByUsername(dto.username);

        if (isAuthenticated(dto) && user.isPresent()) {
            final var accessToken = jwtService.generateAccessToken(user.get().getUsername());
            // TODO: Invalidate the old refresh token
            final var refreshToken = jwtService.generateRefreshToken(user.get().getUsername());

            user.get().setToken(accessToken);
            repository.updateUser(user.get());

            return new TokensDto(accessToken, refreshToken);
        }

        throw new UserAuthenticationException();
    }

    private boolean isAuthenticated(AuthenticateUserDto dto) {
        try {
            return authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(dto.username, dto.password))
                    .isAuthenticated();
        } catch (AuthenticationException error) {
            return false;
        }
    }

    public TokensDto refreshToken(final String authorizationHeader) {
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new UserAuthenticationException();
        }
        final var refreshToken = authorizationHeader.substring(7);
        final var username = jwtService.extractUsername(refreshToken);

        if (username.isEmpty()) {
            throw new UserAuthenticationException();
        }

        final var user = repository.findByUsername(username.get());

        if (user.isPresent() && jwtService.isTokenValid(refreshToken, user.get().getUsername())) {
            final var accessToken = jwtService.generateAccessToken(user.get().getUsername());

            user.get().setToken(accessToken);
            repository.updateUser(user.get());

            return new TokensDto(accessToken, refreshToken);
        }

        throw new UserAuthenticationException();
    }

    public void logout(final User user) {
        user.clearToken();
        repository.updateUser(user);
        SecurityContextHolder.clearContext();
    }
}
