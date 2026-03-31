package com.alumnibeacon.controller;

import com.alumnibeacon.dto.LoginRequest;
import com.alumnibeacon.dto.LoginResponse;
import com.alumnibeacon.dto.RegisterRequest;
import com.alumnibeacon.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Controls whether the JWT cookie requires HTTPS.
     * Set to false in dev/test (HTTP localhost), true in production (HTTPS).
     * Configured via cookie.secure property (default: true for safety).
     */
    @Value("${cookie.secure:true}")
    private boolean cookieSecure;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String logout, Model model) {
        if (logout != null) model.addAttribute("message", "Logged out successfully");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage() { return "auth/register"; }

    @PostMapping("/auth/login")
    public String login(@Valid @ModelAttribute LoginRequest req,
                        HttpServletResponse response, Model model) {
        try {
            LoginResponse lr = authService.login(req);
            setJwtCookie(response, lr.token());
            return "redirect:/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Invalid email or password");
            return "auth/login";
        }
    }

    @PostMapping("/auth/register")
    public String register(@Valid @ModelAttribute RegisterRequest req,
                           HttpServletResponse response, Model model) {
        try {
            LoginResponse lr = authService.register(req);
            setJwtCookie(response, lr.token());
            return "redirect:/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    private void setJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);   // false in dev (HTTP), true in prod (HTTPS)
        cookie.setPath("/");
        cookie.setMaxAge(86400);
        response.addCookie(cookie);
    }
}
