package com.stepbystep.school.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.stepbystep.school.model.GaleriaFoto;
import com.stepbystep.school.repository.GaleriaFotoRepository;
import com.stepbystep.school.util.ValidationUtils;

@Service
public class GaleriaFotoService {

    private static final String MSG_FOTO_NAO_ENCONTRADA = "Foto não encontrada com ID: ";

    private final GaleriaFotoRepository galeriaFotoRepository;
    private final FileUploadService fileUploadService;

    public GaleriaFotoService(GaleriaFotoRepository galeriaFotoRepository, FileUploadService fileUploadService) {
        this.galeriaFotoRepository = galeriaFotoRepository;
        this.fileUploadService = fileUploadService;
    }

    public void salvarImagem(GaleriaFoto galeriaFoto, MultipartFile file) {
        ValidationUtils.validarCampoObrigatorio(galeriaFoto, "Foto da galeria");
        ValidationUtils.validarCampoStringObrigatorio(galeriaFoto.getLegenda(), "Legenda da foto");
        ValidationUtils.validarCampoObrigatorio(galeriaFoto.getDataUpload(), "Data do upload");
        ValidationUtils.validarCampoObrigatorio(galeriaFoto.getCategoria(), "Categoria da foto");
        ValidationUtils.validarCampoObrigatorio(file, "Imagem é obrigatória");
        if (file != null && !file.isEmpty()) {
            String fileName = fileUploadService.salvarImagem(file);
            galeriaFoto.setUrlImagem(montarUrlPublicaUpload(fileName));
        }

        galeriaFoto.setUrlImagem(normalizarUrlImagem(galeriaFoto.getUrlImagem()));
        ValidationUtils.validarCampoObrigatorio(galeriaFoto.getUrlImagem(), "URL da imagem é obrigatória");
        galeriaFotoRepository.save(galeriaFoto);
    }

    public GaleriaFoto editarImagem(GaleriaFoto galeriaFoto, MultipartFile file) {
        ValidationUtils.validarCampoObrigatorio(galeriaFoto, "Foto da galeria");
        ValidationUtils.validarCampoObrigatorio(galeriaFoto.getId(), "ID da foto");
        ValidationUtils.validarCampoStringObrigatorio(galeriaFoto.getLegenda(), "Legenda da foto");
        ValidationUtils.validarCampoObrigatorio(galeriaFoto.getDataUpload(), "Data do upload");
        ValidationUtils.validarCampoObrigatorio(galeriaFoto.getCategoria(), "Categoria da foto");

        GaleriaFoto fotoExistente = galeriaFotoRepository.findById(galeriaFoto.getId())
            .orElseThrow(() -> new IllegalArgumentException(MSG_FOTO_NAO_ENCONTRADA + galeriaFoto.getId()));

        if (file != null && !file.isEmpty()) {
            String fileName = fileUploadService.salvarImagem(file);
            fotoExistente.setUrlImagem(montarUrlPublicaUpload(fileName));
        }

        fotoExistente.setLegenda(galeriaFoto.getLegenda().trim());
        fotoExistente.setDataUpload(galeriaFoto.getDataUpload());
        fotoExistente.setCategoria(galeriaFoto.getCategoria());
        fotoExistente.setUrlImagem(normalizarUrlImagem(fotoExistente.getUrlImagem()));

        ValidationUtils.validarCampoObrigatorio(fotoExistente.getUrlImagem(), "URL da imagem é obrigatória");

        return galeriaFotoRepository.save(fotoExistente);
    }

    public List<GaleriaFoto> listarTodas() {
        return galeriaFotoRepository.findAll().stream()
            .map(this::copiarComUrlNormalizada)
            .toList();
    }

    public void excluirImagem(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, "ID da foto");
        if (!galeriaFotoRepository.existsById(id)) {
            throw new IllegalArgumentException(MSG_FOTO_NAO_ENCONTRADA + id);
        }
        galeriaFotoRepository.deleteById(id);
    }

    private String montarUrlPublicaUpload(String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            return "";
        }
        return "/uploads/" + nomeArquivo;
    }

    private String normalizarUrlImagem(String urlImagem) {
        if (urlImagem == null || urlImagem.isBlank()) {
            return "";
        }

        String valorNormalizado = urlImagem.trim();
        if (valorNormalizado.startsWith("http://") || valorNormalizado.startsWith("https://")) {
            return valorNormalizado;
        }
        if (valorNormalizado.startsWith("/uploads/")) {
            return valorNormalizado;
        }
        if (valorNormalizado.startsWith("uploads/")) {
            return "/" + valorNormalizado;
        }

        return montarUrlPublicaUpload(valorNormalizado);
    }

    private GaleriaFoto copiarComUrlNormalizada(GaleriaFoto foto) {
        return GaleriaFoto.builder()
            .id(foto.getId())
            .legenda(foto.getLegenda())
            .urlImagem(normalizarUrlImagem(foto.getUrlImagem()))
            .dataUpload(foto.getDataUpload())
            .categoria(foto.getCategoria())
            .build();
    }

}
