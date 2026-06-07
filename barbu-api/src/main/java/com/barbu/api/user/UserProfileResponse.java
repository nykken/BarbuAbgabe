package com.barbu.api.user;

import java.util.UUID;

public record UserProfileResponse(UUID id, String username) {}