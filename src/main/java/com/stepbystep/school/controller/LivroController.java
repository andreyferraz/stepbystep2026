package com.stepbystep.school.controller;

import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

import com.stepbystep.school.model.Livro;
import com.stepbystep.school.service.LivroService;

@Controller
public class LivroController {

    private final LivroService livroService;

    public LivroController(LivroService livroService) {
        this.livroService = livroService;
    }

    @GetMapping("/livros")
    public String livros(Model model) {
        List<Livro> livros = StreamSupport.stream(livroService.listarTodosLivros().spliterator(), false)
            .sorted(Comparator.comparing(Livro::getAnoLancamento).reversed())
            .toList();

        model.addAttribute("livros", livros);
        return "livros";
    }

}
