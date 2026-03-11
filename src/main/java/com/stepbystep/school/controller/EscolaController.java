package com.stepbystep.school.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EscolaController {

    @GetMapping("/escola")
    public String escola() {
        return "escola";
    }
}
