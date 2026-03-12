package com.stepbystep.school.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ContatoController {

    @GetMapping("/contato")
    public String contato() {
        return "contato";
    }

}
