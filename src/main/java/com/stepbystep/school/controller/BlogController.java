package com.stepbystep.school.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.stepbystep.school.model.Postagem;
import com.stepbystep.school.service.PostagemService;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BlogController {

    private final PostagemService postagemService;

    @GetMapping("/blog")
    public String blog(
        @RequestParam(name = "q", required = false) String busca,
        @RequestParam(name = "categoria", required = false) String categoria,
        Model model
    ) {
        List<Postagem> postagens = postagemService.listarPostagensPublicadas(busca, categoria);
        model.addAttribute("postagens", postagens);
        model.addAttribute("busca", busca == null ? "" : busca.trim());
        model.addAttribute("categoria", categoria == null ? "" : categoria.trim());
        return "blog";
    }

    @GetMapping("/blog/{slug}")
    public String detalhePostagem(@PathVariable("slug") String slug, Model model) {
        Postagem postagem = postagemService.buscarPostagemPublicadaPorSlug(slug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Postagem não encontrada."));

        model.addAttribute("postagem", postagem);
        return "blog.post";
    }

}
