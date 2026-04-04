package com.stepbystep.school.controller;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

import com.stepbystep.school.model.GaleriaFoto;
import com.stepbystep.school.service.GaleriaCategoriaService;
import com.stepbystep.school.service.GaleriaFotoService;

@Controller
public class GaleriaController {

    private final GaleriaFotoService galeriaFotoService;
    private final GaleriaCategoriaService galeriaCategoriaService;

    public GaleriaController(GaleriaFotoService galeriaFotoService, GaleriaCategoriaService galeriaCategoriaService) {
        this.galeriaFotoService = galeriaFotoService;
        this.galeriaCategoriaService = galeriaCategoriaService;
    }

    @GetMapping("/galeria")
    public String galeria(Model model) {
        List<GaleriaCategoriaFiltroItem> categorias = galeriaCategoriaService.listarCategorias().stream()
            .map(categoria -> new GaleriaCategoriaFiltroItem(categoria.getNome(), normalizarChaveCategoria(categoria.getNome())))
            .toList();

        List<GaleriaFotoViewItem> fotos = galeriaFotoService.listarTodas().stream()
            .sorted(Comparator.comparing(GaleriaFoto::getDataUpload, Comparator.nullsLast(LocalDate::compareTo)).reversed())
            .map(foto -> {
                String categoriaNome = foto.getCategoria() == null ? "Sem categoria" : foto.getCategoria().getNome();
                String categoriaChave = foto.getCategoria() == null ? "sem-categoria" : normalizarChaveCategoria(foto.getCategoria().getNome());

                return new GaleriaFotoViewItem(
                    foto.getLegenda(),
                    foto.getUrlImagem(),
                    categoriaNome,
                    categoriaChave
                );
            })
            .toList();

        model.addAttribute("galeriaFotos", fotos);
        model.addAttribute("galeriaCategorias", categorias);
        return "galeria";
    }

    private String normalizarChaveCategoria(String nomeCategoria) {
        if (nomeCategoria == null || nomeCategoria.isBlank()) {
            return "sem-categoria";
        }

        return nomeCategoria.trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-");
    }

    public record GaleriaCategoriaFiltroItem(String nome, String chave) {
    }

    public record GaleriaFotoViewItem(String legenda, String urlImagem, String categoriaNome, String categoriaChave) {
    }

}
