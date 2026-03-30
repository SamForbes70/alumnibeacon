package com.alumnibeacon.controller;

import com.alumnibeacon.security.TenantDetails;
import com.alumnibeacon.service.InvestigationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final InvestigationService investigationService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        TenantDetails td = (TenantDetails) auth.getDetails();
        model.addAttribute("investigations",
            investigationService.listByTenant(td.tenantId()));
        model.addAttribute("stats",
            investigationService.getDashboardStats(td.tenantId()));
        return "dashboard/index";
    }

    @GetMapping("/")
    public String root() { return "redirect:/dashboard"; }
}
