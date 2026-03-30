package com.alumnibeacon.service;

import com.alumnibeacon.model.Tenant;
import com.alumnibeacon.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditService {

    private static final Logger log = LoggerFactory.getLogger(CreditService.class);
    private static final int CREDITS_PER_SEARCH = 1;

    private final TenantRepository tenantRepository;

    public CreditService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /** Check if tenant has sufficient credits. */
    public boolean hasCredits(String tenantId) {
        return tenantRepository.findById(tenantId)
                .map(t -> t.getCreditsRemaining() >= CREDITS_PER_SEARCH)
                .orElse(false);
    }

    /** Deduct credits for a search. Returns false if insufficient. */
    @Transactional
    public boolean deductCredits(String tenantId) {
        return tenantRepository.findById(tenantId).map(tenant -> {
            int remaining = tenant.getCreditsRemaining();
            if (remaining < CREDITS_PER_SEARCH) {
                log.warn("Tenant {} has insufficient credits: {}", tenantId, remaining);
                return false;
            }
            tenant.setCreditsRemaining(remaining - CREDITS_PER_SEARCH);
            tenantRepository.save(tenant);
            log.info("Deducted {} credit(s) from tenant {}. Remaining: {}",
                    CREDITS_PER_SEARCH, tenantId, remaining - CREDITS_PER_SEARCH);
            return true;
        }).orElse(false);
    }

    /** Refund credits on failed search. */
    @Transactional
    public void refundCredits(String tenantId) {
        tenantRepository.findById(tenantId).ifPresent(tenant -> {
            tenant.setCreditsRemaining(tenant.getCreditsRemaining() + CREDITS_PER_SEARCH);
            tenantRepository.save(tenant);
            log.info("Refunded {} credit(s) to tenant {}", CREDITS_PER_SEARCH, tenantId);
        });
    }

    /** Add credits to a tenant (admin top-up). */
    @Transactional
    public void addCredits(String tenantId, int amount) {
        tenantRepository.findById(tenantId).ifPresent(tenant -> {
            int newBalance = tenant.getCreditsRemaining() + amount;
            tenant.setCreditsRemaining(newBalance);
            tenantRepository.save(tenant);
            log.info("Added {} credits to tenant {}. New balance: {}", amount, tenantId, newBalance);
        });
    }

    /** Get credit balance for a tenant. */
    public int getBalance(String tenantId) {
        return tenantRepository.findById(tenantId)
                .map(Tenant::getCreditsRemaining)
                .orElse(0);
    }

    /** Get credits used (total - remaining). */
    public int getUsed(String tenantId) {
        return tenantRepository.findById(tenantId)
                .map(t -> t.getCreditsTotal() - t.getCreditsRemaining())
                .orElse(0);
    }
}
