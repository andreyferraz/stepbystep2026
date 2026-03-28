package com.stepbystep.school.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.stepbystep.school.model.PreInscricao;
import com.stepbystep.school.service.PreInscricaoEmailService;
import com.stepbystep.school.service.PreInscricaoService;
import com.stepbystep.school.util.ValidationUtils;

@Controller
public class HomeController {

    private final PreInscricaoService preInscricaoService;
    private final PreInscricaoEmailService preInscricaoEmailService;

    public HomeController(
        PreInscricaoService preInscricaoService,
        PreInscricaoEmailService preInscricaoEmailService
    ) {
        this.preInscricaoService = preInscricaoService;
        this.preInscricaoEmailService = preInscricaoEmailService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/pre-inscricao")
    public String enviarPreInscricao(
        @RequestParam("nome") String nome,
        @RequestParam("whatsapp") String whatsapp,
        @RequestParam("nivel") String nivel,
        @RequestParam(name = "duvidas", required = false) String duvidas,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoStringObrigatorio(nome, "Nome");
            ValidationUtils.validarCampoStringObrigatorio(whatsapp, "WhatsApp");
            ValidationUtils.validarCampoStringObrigatorio(nivel, "Nível de Inglês");

            String mensagem = montarMensagemPreInscricao(nivel, duvidas);
            PreInscricao preInscricao = preInscricaoService.criarPreInscricao(nome, whatsapp, mensagem);
            enviarNotificacaoPreInscricao(preInscricao, nivel, duvidas, redirectAttributes);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("preInscricaoErro", ex.getMessage());
        }

        return "redirect:/#pre-inscricao";
    }

    private String montarMensagemPreInscricao(String nivel, String duvidas) {
        StringBuilder mensagem = new StringBuilder();
        mensagem.append("Nivel de ingles: ")
            .append(nivel == null ? "Nao informado" : nivel.trim());

        if (duvidas != null && !duvidas.isBlank()) {
            mensagem.append(" | Duvidas/objetivos: ").append(duvidas.trim());
        }

        return mensagem.toString();
    }

    private void enviarNotificacaoPreInscricao(
        PreInscricao preInscricao,
        String nivel,
        String duvidas,
        RedirectAttributes redirectAttributes
    ) {
        try {
            preInscricaoEmailService.enviarNotificacaoNovaPreInscricao(preInscricao, nivel, duvidas);
            redirectAttributes.addFlashAttribute(
                "preInscricaoFeedback",
                "Pré-inscrição enviada com sucesso. Em breve entraremos em contato."
            );
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                "preInscricaoErro",
                "Pré-inscrição registrada, mas não foi possível enviar o e-mail automaticamente."
            );
        }
    }
}
