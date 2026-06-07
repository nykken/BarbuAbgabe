package com.barbu.api.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "Users", description = "User info.")
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get current user", description = "Returns the authenticated user's ID and username.")
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        UserProfileResponse profile = userService.getProfileByUsername(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }
}