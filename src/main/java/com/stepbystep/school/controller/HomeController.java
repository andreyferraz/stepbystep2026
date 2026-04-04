package com.stepbystep.school.controller;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.stepbystep.school.model.Postagem;
import com.stepbystep.school.model.Livro;
import com.stepbystep.school.service.LivroService;
import com.stepbystep.school.service.PostagemService;
import com.stepbystep.school.service.PreInscricaoService;
import com.stepbystep.school.util.ValidationUtils;
import java.util.Comparator;
import java.util.stream.StreamSupport;

@Controller
public class HomeController {

    private final PreInscricaoService preInscricaoService;
    private final PostagemService postagemService;
    private final LivroService livroService;

    public HomeController(PreInscricaoService preInscricaoService, PostagemService postagemService, LivroService livroService) {
        this.preInscricaoService = preInscricaoService;
        this.postagemService = postagemService;
        this.livroService = livroService;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<Postagem> postagensRecentes = postagemService.listarPostagensPublicadas(null, null)
            .stream()
            .limit(3)
            .toList();

        List<Livro> livrosDestaqueHome = StreamSupport.stream(livroService.listarTodosLivros().spliterator(), false)
            .sorted(Comparator.comparing(Livro::getAnoLancamento).reversed())
            .limit(4)
            .toList();

        model.addAttribute("ultimasPostagensBlog", postagensRecentes);
        model.addAttribute("livrosHome", livrosDestaqueHome);
        return "index";
    }

    @PostMapping("/pre-inscricao")
    public Object enviarPreInscricao(
        @RequestParam("nome") String nome,
        @RequestParam("whatsapp") String whatsapp,
        @RequestParam("nivel") String nivel,
        @RequestParam(name = "duvidas", required = false) String duvidas,
        RedirectAttributes redirectAttributes,
        HttpServletRequest request
    ) {
        boolean requisicaoAssincrona = isRequisicaoAssincrona(request);

        try {
            ValidationUtils.validarCampoStringObrigatorio(nome, "Nome");
            ValidationUtils.validarCampoStringObrigatorio(whatsapp, "WhatsApp");
            ValidationUtils.validarCampoStringObrigatorio(nivel, "Nível de Inglês");

            String mensagem = montarMensagemPreInscricao(nivel, duvidas);
            preInscricaoService.criarPreInscricao(nome, whatsapp, mensagem);

            String feedback = "Pré-inscrição enviada com sucesso. Em breve entraremos em contato.";

            if (requisicaoAssincrona) {
                return ResponseEntity.ok(new PreInscricaoResposta(true, feedback));
            }

            redirectAttributes.addFlashAttribute(
                "preInscricaoFeedback",
                feedback
            );
        } catch (IllegalArgumentException ex) {
            if (requisicaoAssincrona) {
                return ResponseEntity.badRequest().body(new PreInscricaoResposta(false, ex.getMessage()));
            }

            redirectAttributes.addFlashAttribute("preInscricaoErro", ex.getMessage());
        } catch (Exception ex) {
            String erroGenerico = "Não foi possível processar sua pré-inscrição no momento. Tente novamente.";

            if (requisicaoAssincrona) {
                return ResponseEntity.internalServerError().body(new PreInscricaoResposta(false, erroGenerico));
            }

            redirectAttributes.addFlashAttribute("preInscricaoErro", erroGenerico);
        }

        return "redirect:/#pre-inscricao";
    }

    private boolean isRequisicaoAssincrona(HttpServletRequest request) {
        String cabecalhoRequestedWith = request.getHeader("X-Requested-With");
        if (cabecalhoRequestedWith != null && "XMLHttpRequest".equalsIgnoreCase(cabecalhoRequestedWith.trim())) {
            return true;
        }

        String cabecalhoAccept = request.getHeader("Accept");
        return cabecalhoAccept != null && cabecalhoAccept.toLowerCase().contains("application/json");
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

    private record PreInscricaoResposta(boolean sucesso, String mensagem) {
    }
}
