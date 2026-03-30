package com.alumnibeacon.dto;
import jakarta.validation.constraints.*;
public record RegisterRequest(
    @NotBlank String organisationName,
    @NotBlank String fullName,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password
) {}
