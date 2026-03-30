package com.alumnibeacon.controller;

import com.alumnibeacon.model.Tenant;
import com.alumnibeacon.model.User;
import com.alumnibeacon.repository.InvestigationRepository;
import com.alumnibeacon.repository.TenantRepository;
import com.alumnibeacon.repository.UserRepository;
import com.alumnibeacon.security.TenantDetails;
import com.alumnibeacon.service.CreditService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final InvestigationRepository investigationRepository;
    private final CreditService creditService;

    public AdminController(TenantRepository tenantRepository,
                           UserRepository userRepository,
                           InvestigationRepository investigationRepository,
                           CreditService creditService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.investigationRepository = investigationRepository;
        this.creditService = creditService;
    }

    // ── Tenant settings page ─────────────────────────────────────────────────
    @GetMapping
    public String adminIndex(Authentication auth, Model model) {
        TenantDetails td = (TenantDetails) auth.getDetails();
        String tenantId = td.tenantId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        List<User> users = userRepository.findByTenantId(tenantId);

        long totalInvestigations = investigationRepository.countByTenantId(tenantId);
        long completedInvestigations = investigationRepository.countCompletedByTenantId(tenantId);

        model.addAttribute("tenant", tenant);
        model.addAttribute("users", users);
        model.addAttribute("totalInvestigations", totalInvestigations);
        model.addAttribute("completedInvestigations", completedInvestigations);
        model.addAttribute("creditsRemaining", creditService.getBalance(tenantId));
        model.addAttribute("creditsUsed", creditService.getUsed(tenantId));
        model.addAttribute("currentUserId", td.userId());

        return "admin/index";
    }

    // ── Update tenant settings ────────────────────────────────────────────────
    @PostMapping("/settings")
    public String updateSettings(Authentication auth,
                                  @RequestParam String organisationName,
                                  @RequestParam(required = false) String contactEmail,
                                  RedirectAttributes redirectAttributes) {
        TenantDetails td = (TenantDetails) auth.getDetails();
        String tenantId = td.tenantId();

        tenantRepository.findById(tenantId).ifPresent(tenant -> {
            tenant.setName(organisationName);
            tenantRepository.save(tenant);
        });

        redirectAttributes.addFlashAttribute("successMessage", "Settings updated successfully.");
        return "redirect:/admin";
    }

    // ── Remove user ───────────────────────────────────────────────────────────
    @PostMapping("/users/{userId}/remove")
    public String removeUser(Authentication auth,
                              @PathVariable String userId,
                              RedirectAttributes redirectAttributes) {
        TenantDetails td = (TenantDetails) auth.getDetails();
        String tenantId = td.tenantId();

        // Prevent self-removal
        if (userId.equals(td.userId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot remove yourself.");
            return "redirect:/admin";
        }

        userRepository.findById(userId).ifPresent(user -> {
            if (user.getTenantId().equals(tenantId)) {
                userRepository.delete(user);
            }
        });

        redirectAttributes.addFlashAttribute("successMessage", "User removed.");
        return "redirect:/admin";
    }

    // ── HTMX: credit balance fragment ─────────────────────────────────────────
    @GetMapping("/credits")
    public String creditsFragment(Authentication auth, Model model) {
        TenantDetails td = (TenantDetails) auth.getDetails();
        String tenantId = td.tenantId();
        model.addAttribute("creditsRemaining", creditService.getBalance(tenantId));
        model.addAttribute("creditsUsed", creditService.getUsed(tenantId));
        return "admin/index :: creditsFragment";
    }
}
