package com.alumnibeacon.service;

import com.alumnibeacon.dto.LoginRequest;
import com.alumnibeacon.dto.LoginResponse;
import com.alumnibeacon.dto.RegisterRequest;
import com.alumnibeacon.model.Tenant;
import com.alumnibeacon.model.User;
import com.alumnibeacon.repository.TenantRepository;
import com.alumnibeacon.repository.UserRepository;
import com.alumnibeacon.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        String slug = req.organisationName().toLowerCase()
            .replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-");

        // Ensure unique slug
        String finalSlug = slug;
        int i = 1;
        while (tenantRepository.existsBySlug(finalSlug)) {
            finalSlug = slug + "-" + i++;
        }

        Tenant tenant = Tenant.builder()
            .id(UUID.randomUUID().toString())
            .name(req.organisationName())
            .slug(finalSlug)
            .plan(Tenant.Plan.STARTER)
            .creditsRemaining(100)
            .creditsTotal(100)
            .build();
        tenantRepository.save(tenant);

        User user = User.builder()
            .id(UUID.randomUUID().toString())
            .tenantId(tenant.getId())
            .email(req.email())
            .passwordHash(passwordEncoder.encode(req.password()))
            .fullName(req.fullName())
            .role(User.Role.ADMIN)
            .build();
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), tenant.getId(),
            user.getEmail(), user.getRole().name());
        return new LoginResponse(token, user.getId(), tenant.getId(),
            user.getRole().name(), user.getFullName());
    }

    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getTenantId(),
            user.getEmail(), user.getRole().name());
        return new LoginResponse(token, user.getId(), user.getTenantId(),
            user.getRole().name(), user.getFullName());
    }
}
