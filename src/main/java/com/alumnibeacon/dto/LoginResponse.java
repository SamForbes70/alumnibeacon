package com.alumnibeacon.dto;
public record LoginResponse(String token, String userId, String tenantId, String role, String fullName) {}
