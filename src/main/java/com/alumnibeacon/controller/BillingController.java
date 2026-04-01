package com.alumnibeacon.controller;

import com.alumnibeacon.model.Tenant;
import com.alumnibeacon.repository.TenantRepository;
import com.alumnibeacon.security.TenantDetails;
import com.alumnibeacon.service.StripeService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class BillingController {

    private final StripeService stripeService;
    private final TenantRepository tenantRepository;

    /** Public pricing page — no auth required. */
    @GetMapping("/pricing")
    public String pricingPage(Model model) {
        model.addAttribute("pageTitle", "Pricing");
        return "billing/pricing";
    }

    /** Authenticated billing dashboard. */
    @GetMapping("/billing")
    public String billingDashboard(
            Authentication auth,
            @RequestParam(required = false) String success,
            @RequestParam(required = false) String cancelled,
            @RequestParam(required = false) String mock,
            Model model) {

        Tenant tenant = resolveTenant(auth);
        int usagePercent = tenant.getMonthlyInvestigationLimit() > 0
            ? (tenant.getInvestigationsUsedThisMonth() * 100 / tenant.getMonthlyInvestigationLimit())
            : 0;

        model.addAttribute("tenant", tenant);
        model.addAttribute("stripeEnabled", stripeService.isStripeEnabled());
        model.addAttribute("usagePercent", usagePercent);
        model.addAttribute("pageTitle", "Billing");

        if ("true".equals(success))    model.addAttribute("successMsg", "Your subscription has been activated!");
        if ("true".equals(cancelled))  model.addAttribute("cancelMsg",  "Checkout cancelled — no charge made.");
        if (mock != null)              model.addAttribute("mockMsg",    "Stripe not configured — this is a demo redirect.");

        return "billing/dashboard";
    }

    /** Redirect to Stripe Checkout for the chosen plan. */
    @GetMapping("/billing/checkout/{plan}")
    public String checkout(
            @PathVariable String plan,
            Authentication auth,
            RedirectAttributes ra) {
        try {
            Tenant tenant = resolveTenant(auth);
            String url = stripeService.createCheckoutSession(tenant, plan);
            return "redirect:" + url;
        } catch (StripeException e) {
            log.error("Stripe checkout error", e);
            ra.addFlashAttribute("errorMsg", "Could not start checkout: " + e.getMessage());
            return "redirect:/billing";
        }
    }

    /** Redirect to Stripe Customer Portal. */
    @GetMapping("/billing/portal")
    public String portal(Authentication auth, RedirectAttributes ra) {
        try {
            Tenant tenant = resolveTenant(auth);
            String url = stripeService.createPortalSession(tenant);
            return "redirect:" + url;
        } catch (StripeException e) {
            log.error("Stripe portal error", e);
            ra.addFlashAttribute("errorMsg", "Could not open billing portal: " + e.getMessage());
            return "redirect:/billing";
        }
    }

    /** Stripe webhook endpoint — must be public (no JWT). */
    @PostMapping("/billing/webhook")
    @ResponseBody
    public ResponseEntity<String> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        try {
            stripeService.handleWebhook(payload, sigHeader);
            return ResponseEntity.ok("ok");
        } catch (RuntimeException e) {
            log.error("Webhook error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Tenant resolveTenant(Authentication auth) {
        TenantDetails td = (TenantDetails) auth.getDetails();
        return tenantRepository.findById(td.tenantId())
            .orElseThrow(() -> new RuntimeException("Tenant not found: " + td.tenantId()));
    }
}
