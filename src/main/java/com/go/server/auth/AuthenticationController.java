package com.go.server.auth;

import com.go.server.auth.model.input.AuthenticateUserDto;
import com.go.server.auth.model.input.RefreshTokenDto;
import com.go.server.auth.model.input.RegisterUserDto;
import com.go.server.auth.model.output.TokensDto;
import com.go.server.user.model.User;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AuthenticationController.AUTHENTICATION_PATH)
public class AuthenticationController {
    public static final String AUTHENTICATION_PATH = "/auth";
    private final AuthenticationService authenticationService;

    public AuthenticationController(final AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public TokensDto register(@RequestBody @Valid RegisterUserDto dto) {
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
    public TokensDto refreshToken(@RequestBody RefreshTokenDto dto) {
        return authenticationService.refreshToken(dto);
    }

    @PostMapping("/logout")
    public void logout() {
        authenticationService.logout(User.getFromSecurityContext());
    }
}
