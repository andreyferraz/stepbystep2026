package com.stepbystep.school.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LivroController {

    @GetMapping("/livros")
    public String livros() {
        return "livros";
    }

}
