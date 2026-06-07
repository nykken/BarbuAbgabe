package com.barbu.api.authentication;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "Register, log in, and log out.")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(AuthService authService,
                          AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @Operation(summary = "Register",
            description = "Creates a new account and immediately logs the user in. Returns 201 on success.")
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request,
                                           HttpServletRequest httpRequest,
                                           HttpServletResponse httpResponse) {
        authService.register(request);
        authenticateAndSetContext(request.username(), request.password(), httpRequest, httpResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully!");
    }

    @Operation(summary = "Log in",
            description = "Authenticates the user and opens a session cookie. Returns 401 for invalid credentials.")
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest request,
                                        HttpServletRequest httpRequest,
                                        HttpServletResponse httpResponse) {
        try {
            authenticateAndSetContext(request.username(), request.password(), httpRequest, httpResponse);
            return ResponseEntity.ok("Login successful!");
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    @Operation(summary = "Log out", description = "Invalidates the current session. Intercepted by Spring Security.")
    @PostMapping("/logout")
    public void logout() {
        // intercepted by Spring Security's logout filter before this runs. Here because of swagger.
    }


    private void authenticateAndSetContext(String username, String password,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(username, password)
        );
        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(auth);
        SecurityContextHolder.setContext(sc);
        securityContextRepository.saveContext(sc, request, response);
    }
}