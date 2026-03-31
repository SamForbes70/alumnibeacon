package com.alumnibeacon.controller;

import com.alumnibeacon.dto.CreateInvestigationRequest;
import com.alumnibeacon.dto.InvestigationDto;
import com.alumnibeacon.dto.OsintResultDto;
import com.alumnibeacon.model.Investigation;
import com.alumnibeacon.security.TenantDetails;
import com.alumnibeacon.service.CreditService;
import com.alumnibeacon.service.InvestigationService;
import com.alumnibeacon.service.PdfReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequiredArgsConstructor
public class InvestigationController {

    private final InvestigationService investigationService;
    private final PdfReportService pdfReportService;
    private final CreditService creditService;

    // ── List investigations ───────────────────────────────────────────────────
    @GetMapping("/investigations")
    public String list(Authentication auth, Model model,
                       @RequestParam(defaultValue = "") String search,
                       @RequestParam(defaultValue = "") String status,
                       @RequestParam(defaultValue = "0") int page) {
        TenantDetails td = (TenantDetails) auth.getDetails();
        String tenantId = td.tenantId();

        List<InvestigationDto> all = investigationService.listByTenantFiltered(tenantId, search, status);

        int pageSize   = 20;
        int totalPages = (int) Math.ceil((double) all.size() / pageSize);
        int safePage   = Math.max(0, Math.min(page, Math.max(0, totalPages - 1)));
        int from = safePage * pageSize;
        int to   = Math.min(from + pageSize, all.size());

        model.addAttribute("investigations",   all.subList(from, to));
        model.addAttribute("totalCount",       investigationService.countByTenant(tenantId));
        model.addAttribute("completedCount",   investigationService.countByTenantAndStatus(tenantId, "COMPLETED"));
        model.addAttribute("processingCount",  investigationService.countByTenantAndStatus(tenantId, "PROCESSING"));
        model.addAttribute("creditsUsed",      creditService.getUsed(tenantId));
        model.addAttribute("creditsRemaining", creditService.getBalance(tenantId));
        model.addAttribute("totalPages",       totalPages);
        model.addAttribute("currentPage",      safePage);
        model.addAttribute("pageSize",         pageSize);
        model.addAttribute("search",           search);
        model.addAttribute("status",           status);
        return "investigation/list";
    }

    // ── Investigation detail ──────────────────────────────────────────────────
    @GetMapping("/investigations/{id}")
    public String detail(@PathVariable String id, Authentication auth, Model model) {
        TenantDetails td = (TenantDetails) auth.getDetails();
        InvestigationDto inv = investigationService.getById(id, td.tenantId());
        model.addAttribute("investigation", inv);
        model.addAttribute("creditsRemaining", creditService.getBalance(td.tenantId()));
        // Parse OSINT result JSON into structured DTO for display
        OsintResultDto osintResult = investigationService.parseOsintResult(inv.resultJson());
        model.addAttribute("osintResult", osintResult);
        return "investigation/detail";
    }

    // ── New investigation form ────────────────────────────────────────────────
    @GetMapping("/investigations/new")
    public String newForm(Authentication auth, Model model) {
        TenantDetails td = (TenantDetails) auth.getDetails();
        model.addAttribute("creditsRemaining", creditService.getBalance(td.tenantId()));
        return "investigation/new";
    }

    // ── Create investigation ──────────────────────────────────────────────────
    @PostMapping("/investigations")
    public String create(@Valid @ModelAttribute CreateInvestigationRequest req,
                         Authentication auth,
                         RedirectAttributes redirectAttributes) {
        TenantDetails td = (TenantDetails) auth.getDetails();

        // Check credits before creating
        if (!creditService.hasCredits(td.tenantId())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Insufficient credits. Please top up to continue.");
            return "redirect:/investigations/new";
        }

        InvestigationDto inv = investigationService.create(req, td.tenantId(), td.userId());
        return "redirect:/investigations/" + inv.id();
    }

    // ── HTMX: status polling fragment ─────────────────────────────────────────
    @GetMapping("/investigations/{id}/status")
    public String statusFragment(@PathVariable String id,
                                  Authentication auth,
                                  Model model,
                                  HttpServletResponse response) {
        TenantDetails td = (TenantDetails) auth.getDetails();
        InvestigationDto inv = investigationService.getById(id, td.tenantId());
        model.addAttribute("investigation", inv);
        // Tell HTMX to do a full page reload when job is done
        if ("COMPLETED".equals(inv.status()) || "FAILED".equals(inv.status())) {
            response.setHeader("HX-Refresh", "true");
        }
        return "investigation/detail :: statusCard";
    }

    // ── PDF report download ───────────────────────────────────────────────────
    @GetMapping("/investigations/{id}/report")
    public ResponseEntity<byte[]> downloadReport(@PathVariable String id,
                                                   Authentication auth) {
        TenantDetails td = (TenantDetails) auth.getDetails();
        Investigation inv = investigationService.getEntityById(id, td.tenantId());

        if (!"COMPLETED".equals(inv.getStatus().name())) {
            return ResponseEntity.badRequest().build();
        }

        byte[] pdf = pdfReportService.generateReport(inv);
        String filename = "AlumniBeacon-" + inv.getSubjectName().replaceAll("\\s+", "-") + "-Report.pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ── Retry failed investigation ────────────────────────────────────────────
    @PostMapping("/investigations/{id}/retry")
    public String retry(@PathVariable String id,
                        Authentication auth,
                        RedirectAttributes redirectAttributes) {
        TenantDetails td = (TenantDetails) auth.getDetails();

        // Check credits
        if (!creditService.hasCredits(td.tenantId())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Insufficient credits to retry. Please top up.");
            return "redirect:/investigations/" + id;
        }

        try {
            investigationService.retry(id, td.tenantId());
            redirectAttributes.addFlashAttribute("successMessage", "Investigation requeued successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not retry: " + e.getMessage());
        }

        return "redirect:/investigations/" + id;
    }

    // ── Delete investigation ──────────────────────────────────────────────────
    @PostMapping("/investigations/{id}/delete")
    public String delete(@PathVariable String id,
                         Authentication auth,
                         RedirectAttributes redirectAttributes) {
        TenantDetails td = (TenantDetails) auth.getDetails();
        investigationService.delete(id, td.tenantId());
        redirectAttributes.addFlashAttribute("successMessage", "Investigation deleted.");
        return "redirect:/investigations";
    }
}
