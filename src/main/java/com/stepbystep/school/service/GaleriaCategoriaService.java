package com.stepbystep.school.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.stepbystep.school.model.GaleriaCategoria;
import com.stepbystep.school.repository.GaleriaCategoriaRepository;
import com.stepbystep.school.repository.GaleriaFotoRepository;
import com.stepbystep.school.util.ValidationUtils;

@Service
public class GaleriaCategoriaService {

    private static final String CAMPO_ID_CATEGORIA = "ID da categoria";
    private static final String MSG_CATEGORIA_NAO_ENCONTRADA = "Categoria da galeria não encontrada com ID: ";

    private final GaleriaCategoriaRepository galeriaCategoriaRepository;
    private final GaleriaFotoRepository galeriaFotoRepository;

    public GaleriaCategoriaService(
        GaleriaCategoriaRepository galeriaCategoriaRepository,
        GaleriaFotoRepository galeriaFotoRepository
    ) {
        this.galeriaCategoriaRepository = galeriaCategoriaRepository;
        this.galeriaFotoRepository = galeriaFotoRepository;
    }

    public GaleriaCategoria criarCategoria(String nome) {
        ValidationUtils.validarCampoStringObrigatorio(nome, "Nome da categoria");
        String nomeNormalizado = normalizar(nome);

        if (galeriaCategoriaRepository.existsByNomeIgnoreCase(nomeNormalizado)) {
            throw new IllegalArgumentException("Já existe uma categoria com este nome.");
        }

        GaleriaCategoria categoria = GaleriaCategoria.builder()
            .nome(nomeNormalizado)
            .build();

        return galeriaCategoriaRepository.save(categoria);
    }

    public GaleriaCategoria editarCategoria(UUID id, String nome) {
        ValidationUtils.validarCampoObrigatorio(id, CAMPO_ID_CATEGORIA);
        ValidationUtils.validarCampoStringObrigatorio(nome, "Nome da categoria");

        String nomeNormalizado = normalizar(nome);
        if (galeriaCategoriaRepository.existsByNomeIgnoreCaseAndIdNot(nomeNormalizado, id)) {
            throw new IllegalArgumentException("Já existe uma categoria com este nome.");
        }

        GaleriaCategoria categoria = obterPorId(id);
        categoria.setNome(nomeNormalizado);
        return galeriaCategoriaRepository.save(categoria);
    }

    public void excluirCategoria(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, CAMPO_ID_CATEGORIA);

        if (!galeriaCategoriaRepository.existsById(id)) {
            throw new IllegalArgumentException(MSG_CATEGORIA_NAO_ENCONTRADA + id);
        }

        long totalFotos = galeriaFotoRepository.countByCategoriaId(id);
        if (totalFotos > 0) {
            throw new IllegalStateException("Não é possível excluir a categoria, pois existem fotos vinculadas.");
        }

        galeriaCategoriaRepository.deleteById(id);
    }

    public GaleriaCategoria obterPorId(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, CAMPO_ID_CATEGORIA);
        return galeriaCategoriaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(MSG_CATEGORIA_NAO_ENCONTRADA + id));
    }

    public List<GaleriaCategoria> listarCategorias() {
        return galeriaCategoriaRepository.findAllByOrderByNomeAsc();
    }

    private String normalizar(String nome) {
        if (nome == null) {
            return "";
        }

        String valor = nome.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (valor.isBlank()) {
            return "";
        }

        return Character.toUpperCase(valor.charAt(0)) + valor.substring(1);
    }
}
