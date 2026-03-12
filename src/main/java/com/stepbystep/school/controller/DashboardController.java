package com.stepbystep.school.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("isDashboard", true);
        return "admin/dashboard";
    }

    @GetMapping("/aluno/dashboard")
    public String alunoDashboard(Model model) {
        model.addAttribute("isDashboard", true);
        return "aluno/dashboard";
    }

    @GetMapping("/acesso-negado")
    public String acessoNegado() {
        return "errors/403";
    }
}
