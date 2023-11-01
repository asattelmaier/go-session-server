package com.go.server.auth;

import com.go.server.auth.model.input.AuthenticateUserDto;
import com.go.server.auth.model.input.RegisterUserDto;
import com.go.server.auth.model.output.TokensDto;
import com.go.server.user.model.User;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    public AuthenticationController(final AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public TokensDto register(@RequestBody RegisterUserDto dto) {
        return authenticationService.register(dto);
    }

    @PostMapping("/register/guest")
    public TokensDto registerGuest() {
        return authenticationService.registerGuest();
    }

    @PostMapping("/authenticate")
    public TokensDto authenticate(@RequestBody AuthenticateUserDto dto) {
        return authenticationService.authenticate(dto);
    }

    @PostMapping("/refresh-token")
    public TokensDto refreshToken(@NonNull @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return authenticationService.refreshToken(authorization);
    }

    @PostMapping("/logout")
    public void logout() {
        authenticationService.logout(User.getFromSecurityContext());
    }
}
