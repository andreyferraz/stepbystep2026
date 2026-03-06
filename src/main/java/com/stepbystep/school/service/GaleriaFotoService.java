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

    private final GaleriaFotoRepository galeriaFotoRepository;
    private final FileUploadService fileUploadService;

    public GaleriaFotoService(GaleriaFotoRepository galeriaFotoRepository, FileUploadService fileUploadService) {
        this.galeriaFotoRepository = galeriaFotoRepository;
        this.fileUploadService = fileUploadService;
    }

    public void salvarImagem(GaleriaFoto galeriaFoto, MultipartFile file) {
        
        ValidationUtils.validarCampoObrigatorio(file, "Imagem é obrigatória");
        if(file != null && !file.isEmpty()) {
            String fileName = fileUploadService.salvarImagem(file);
            galeriaFoto.setUrlImagem(fileName);
        }
        ValidationUtils.validarCampoObrigatorio(galeriaFoto.getUrlImagem(), "URL da imagem é obrigatória");
        galeriaFotoRepository.save(galeriaFoto);
    }

    public List<GaleriaFoto> listarTodas() {
        return galeriaFotoRepository.findAll();
    }

    public void excluirImagem(UUID id) {
        galeriaFotoRepository.deleteById(id);
    }

}
