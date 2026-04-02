package com.stepbystep.school.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.stepbystep.school.enums.StatusPostagem;
import com.stepbystep.school.model.Postagem;
import com.stepbystep.school.service.PostagemService;
import com.stepbystep.school.util.ValidationUtils;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AdminBlogController {

    private static final String REDIRECT_BLOG_PANEL = "redirect:/admin/dashboard?panel=blog";
    private static final String FEEDBACK_BLOG_FORM = "blogFormFeedback";
    private static final String FEEDBACK_BLOG_EDIT = "blogEditFeedback";

    private final PostagemService postagemService;

    @PostMapping("/admin/blog/postagens")
    public String criarPostagem(
        @RequestParam("titulo") String titulo,
        @RequestParam(name = "categoria", required = false) String categoria,
        @RequestParam(name = "autor", required = false) String autor,
        @RequestParam(name = "resumo", required = false) String resumo,
        @RequestParam(name = "dataPublicacao", required = false) LocalDate dataPublicacao,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam("conteudo") String conteudo,
        @RequestParam(name = "imagemCapa", required = false) MultipartFile imagemCapa,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoStringObrigatorio(titulo, "Título da postagem");
            ValidationUtils.validarCampoStringObrigatorio(conteudo, "Conteúdo da postagem");

            Postagem postagem = Postagem.builder()
                .titulo(titulo)
                .categoria(categoria)
                .autor(autor)
                .resumo(resumo)
                .conteudo(conteudo)
                .status(parseStatus(status))
                .dataPublicacao(dataPublicacao == null ? LocalDateTime.now() : dataPublicacao.atStartOfDay())
                .build();

            postagemService.criarPostagem(postagem, imagemCapa);
            redirectAttributes.addFlashAttribute(FEEDBACK_BLOG_FORM, "Postagem salva com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_BLOG_FORM, ex.getMessage());
        }

        return REDIRECT_BLOG_PANEL;
    }

    @PostMapping("/admin/blog/postagens/editar")
    public String editarPostagem(
        @RequestParam("postagemId") UUID postagemId,
        @RequestParam("titulo") String titulo,
        @RequestParam(name = "categoria", required = false) String categoria,
        @RequestParam(name = "autor", required = false) String autor,
        @RequestParam(name = "resumo", required = false) String resumo,
        @RequestParam(name = "dataPublicacao", required = false) LocalDate dataPublicacao,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam("conteudo") String conteudo,
        @RequestParam(name = "imagemCapa", required = false) MultipartFile imagemCapa,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(postagemId, "ID da postagem");
            ValidationUtils.validarCampoStringObrigatorio(titulo, "Título da postagem");
            ValidationUtils.validarCampoStringObrigatorio(conteudo, "Conteúdo da postagem");

            Postagem postagem = Postagem.builder()
                .titulo(titulo)
                .categoria(categoria)
                .autor(autor)
                .resumo(resumo)
                .conteudo(conteudo)
                .status(parseStatus(status))
                .dataPublicacao(dataPublicacao == null ? null : dataPublicacao.atStartOfDay())
                .build();

            postagemService.editarPostagem(postagemId, postagem, imagemCapa);
            redirectAttributes.addFlashAttribute(FEEDBACK_BLOG_EDIT, "Postagem atualizada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_BLOG_EDIT, ex.getMessage());
        }

        return REDIRECT_BLOG_PANEL;
    }

    @PostMapping("/admin/blog/postagens/excluir")
    public String excluirPostagem(
        @RequestParam("postagemId") UUID postagemId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            postagemService.excluirPostagem(postagemId);
            redirectAttributes.addFlashAttribute(FEEDBACK_BLOG_EDIT, "Postagem excluída com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_BLOG_EDIT, ex.getMessage());
        }

        return REDIRECT_BLOG_PANEL;
    }

    @PostMapping("/admin/blog/postagens/imagens")
    @ResponseBody
    public ResponseEntity<Map<String, String>> uploadImagemConteudo(
        @RequestParam("imagem") MultipartFile imagem
    ) {
        String url = postagemService.salvarImagemConteudo(imagem);
        return ResponseEntity.ok(Map.of("url", url));
    }

    private StatusPostagem parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return StatusPostagem.RASCUNHO;
        }

        return switch (status.trim().toLowerCase()) {
            case "publicado" -> StatusPostagem.PUBLICADO;
            case "agendado" -> StatusPostagem.AGENDADO;
            default -> StatusPostagem.RASCUNHO;
        };
    }
}
