package com.alumnibeacon.service;

import com.alumnibeacon.config.StripeConfig;
import com.alumnibeacon.model.Tenant;
import com.alumnibeacon.repository.TenantRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;

import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    private final StripeConfig stripeConfig;
    private final TenantRepository tenantRepository;

    public boolean isStripeEnabled() { return stripeConfig.isEnabled(); }

    // ── Checkout ──────────────────────────────────────────────────────────────

    /**
     * Create a Stripe Checkout Session for the given plan.
     * Returns the checkout URL, or null if Stripe is not configured.
     */
    public String createCheckoutSession(Tenant tenant, String planKey) throws StripeException {
        if (!stripeConfig.isEnabled()) {
            log.warn("Stripe not configured — returning mock checkout URL");
            return stripeConfig.getBaseUrl() + "/billing?mock=true&plan=" + planKey;
        }

        String priceId = resolvePriceId(planKey);
        String customerId = ensureStripeCustomer(tenant);

        SessionCreateParams params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .setCustomer(customerId)
            .addLineItem(SessionCreateParams.LineItem.builder()
                .setPrice(priceId)
                .setQuantity(1L)
                .build())
            .setSuccessUrl(stripeConfig.getBaseUrl() + "/billing?success=true")
            .setCancelUrl(stripeConfig.getBaseUrl() + "/billing?cancelled=true")
            .putMetadata("tenant_id", tenant.getId())
            .putMetadata("plan", planKey)
            .build();

        Session session = Session.create(params);
        log.info("Stripe checkout session created for tenant {} plan {}", tenant.getId(), planKey);
        return session.getUrl();
    }

    /**
     * Create a Stripe Customer Portal session for managing subscriptions.
     */
    public String createPortalSession(Tenant tenant) throws StripeException {
        if (!stripeConfig.isEnabled()) {
            return stripeConfig.getBaseUrl() + "/billing?mock=portal";
        }

        String customerId = ensureStripeCustomer(tenant);
        com.stripe.param.billingportal.SessionCreateParams params = com.stripe.param.billingportal.SessionCreateParams.builder()
            .setCustomer(customerId)
            .setReturnUrl(stripeConfig.getBaseUrl() + "/billing")
            .build();

        com.stripe.model.billingportal.Session session = com.stripe.model.billingportal.Session.create(params);
        return session.getUrl();
    }

    // ── Webhook ───────────────────────────────────────────────────────────────

    public void handleWebhook(String payload, String sigHeader) {
        if (!stripeConfig.isEnabled()) {
            log.debug("Stripe not configured — ignoring webhook");
            return;
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.error("Stripe webhook signature verification failed", e);
            throw new RuntimeException("Invalid webhook signature");
        }

        log.info("Stripe webhook received: {}", event.getType());

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
            case "invoice.payment_failed" -> handlePaymentFailed(event);
            default -> log.debug("Unhandled Stripe event: {}", event.getType());
        }
    }

    // ── Plan Limits ───────────────────────────────────────────────────────────

    public boolean hasInvestigationsRemaining(Tenant tenant) {
        return tenant.getInvestigationsUsedThisMonth() < tenant.getMonthlyInvestigationLimit();
    }

    public void incrementInvestigationUsage(Tenant tenant) {
        tenant.setInvestigationsUsedThisMonth(tenant.getInvestigationsUsedThisMonth() + 1);
        tenantRepository.save(tenant);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String ensureStripeCustomer(Tenant tenant) throws StripeException {
        if (tenant.getStripeCustomerId() != null) return tenant.getStripeCustomerId();

        CustomerCreateParams params = CustomerCreateParams.builder()
            .setEmail(tenant.getContactEmail())
            .setName(tenant.getName())
            .putMetadata("tenant_id", tenant.getId())
            .build();

        Customer customer = Customer.create(params);
        tenant.setStripeCustomerId(customer.getId());
        tenantRepository.save(tenant);
        log.info("Created Stripe customer {} for tenant {}", customer.getId(), tenant.getId());
        return customer.getId();
    }

    private String resolvePriceId(String planKey) {
        return switch (planKey.toLowerCase()) {
            case "professional" -> stripeConfig.getProfessionalPriceId();
            default -> stripeConfig.getStarterPriceId();
        };
    }

    private void handleCheckoutCompleted(Event event) {
        event.getDataObjectDeserializer().getObject().ifPresent(obj -> {
            Session session = (Session) obj;
            String tenantId = session.getMetadata().get("tenant_id");
            String planKey  = session.getMetadata().get("plan");
            tenantRepository.findById(tenantId).ifPresent(tenant -> {
                tenant.setPlan(parsePlan(planKey));
                tenant.setStripeSubscriptionId(session.getSubscription());
                tenant.setSubscriptionStatus("active");
                tenant.setMonthlyInvestigationLimit(limitForPlan(parsePlan(planKey)));
                tenantRepository.save(tenant);
                log.info("Tenant {} upgraded to plan {}", tenantId, planKey);
            });
        });
    }

    private void handleSubscriptionUpdated(Event event) {
        event.getDataObjectDeserializer().getObject().ifPresent(obj -> {
            Subscription sub = (Subscription) obj;
            tenantRepository.findByStripeSubscriptionId(sub.getId()).ifPresent(tenant -> {
                tenant.setSubscriptionStatus(sub.getStatus());
                if (sub.getCurrentPeriodEnd() != null) {
                    tenant.setSubscriptionPeriodEnd(
                        LocalDateTime.ofInstant(Instant.ofEpochSecond(sub.getCurrentPeriodEnd()),
                            ZoneId.systemDefault()).toString());
                }
                tenantRepository.save(tenant);
                log.info("Subscription updated for tenant {}: {}", tenant.getId(), sub.getStatus());
            });
        });
    }

    private void handleSubscriptionDeleted(Event event) {
        event.getDataObjectDeserializer().getObject().ifPresent(obj -> {
            Subscription sub = (Subscription) obj;
            tenantRepository.findByStripeSubscriptionId(sub.getId()).ifPresent(tenant -> {
                tenant.setPlan(Tenant.Plan.STARTER);
                tenant.setSubscriptionStatus("cancelled");
                tenant.setMonthlyInvestigationLimit(10);
                tenantRepository.save(tenant);
                log.info("Subscription cancelled for tenant {}", tenant.getId());
            });
        });
    }

    private void handlePaymentFailed(Event event) {
        event.getDataObjectDeserializer().getObject().ifPresent(obj -> {
            Invoice invoice = (Invoice) obj;
            log.warn("Payment failed for customer {}", invoice.getCustomer());
        });
    }

    private Tenant.Plan parsePlan(String planKey) {
        if (planKey == null) return Tenant.Plan.STARTER;
        return switch (planKey.toLowerCase()) {
            case "professional" -> Tenant.Plan.PROFESSIONAL;
            case "enterprise"   -> Tenant.Plan.ENTERPRISE;
            default             -> Tenant.Plan.STARTER;
        };
    }

    private int limitForPlan(Tenant.Plan plan) {
        return switch (plan) {
            case PROFESSIONAL -> 250;
            case ENTERPRISE   -> 9999;
            default           -> 50;
        };
    }
}
