package com.alumnibeacon.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /** Public paths that never require authentication. */
    private static final Set<String> PUBLIC_PREFIXES = Set.of(
        "/auth/", "/login", "/register",
        "/forgot-password", "/reset-password",
        "/css/", "/js/", "/images/", "/webjars/",
        "/actuator/health", "/error"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String token = extractToken(request);

        if (token != null && jwtUtil.isTokenValid(token)) {
            // Valid JWT — set authentication in SecurityContext
            String userId   = jwtUtil.extractUserId(token);
            String tenantId = jwtUtil.extractTenantId(token);
            String role     = jwtUtil.extractRole(token);

            var auth = new UsernamePasswordAuthenticationToken(
                    userId, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            auth.setDetails(new TenantDetails(tenantId, userId, role));
            SecurityContextHolder.getContext().setAuthentication(auth);

        } else if (!isPublicPath(path)) {
            // No valid token and path requires auth — redirect to login
            // This handles both browser and API clients cleanly
            String loginUrl = request.getContextPath() + "/login";
            response.sendRedirect(loginUrl);
            return;  // Stop filter chain — response already committed
        }

        filterChain.doFilter(request, response);
    }

    /** Extract JWT from Authorization header or jwt cookie. */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /** Returns true if the path is publicly accessible without authentication. */
    private boolean isPublicPath(String path) {
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
