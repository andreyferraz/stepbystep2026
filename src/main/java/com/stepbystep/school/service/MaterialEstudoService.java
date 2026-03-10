package com.stepbystep.school.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.stepbystep.school.model.MaterialEstudo;
import com.stepbystep.school.repository.MaterialEstudoRepository;
import com.stepbystep.school.util.ValidationUtils;

@Service
public class MaterialEstudoService {

    private final FileUploadService fileUploadService;
    private final MaterialEstudoRepository materialEstudoRepository;

    public MaterialEstudoService(FileUploadService fileUploadService, MaterialEstudoRepository materialEstudoRepository) {
        this.fileUploadService = fileUploadService;
        this.materialEstudoRepository = materialEstudoRepository;
    }

    public void salvarMaterialEstudo(MaterialEstudo materialEstudo, MultipartFile file) {

        ValidationUtils.validarCampoObrigatorio(materialEstudo, "Material de estudo é obrigatório");
        ValidationUtils.validarCampoStringObrigatorio(materialEstudo.getTitulo(), "Título é obrigatório");
        ValidationUtils.validarCampoStringObrigatorio(materialEstudo.getDescricao(), "Descrição é obrigatória");
        ValidationUtils.validarCampoObrigatorio(materialEstudo.getDataUpload(), "Data de upload é obrigatória");
        ValidationUtils.validarCampoObrigatorio(materialEstudo.getTurma(), "Turma é obrigatória");

        ValidationUtils.validarCampoObrigatorio(file, "Arquivo é obrigatório");
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo é obrigatório.");
        }

        String fileName = fileUploadService.salvarDocumento(file);
        materialEstudo.setUrlArquivo(fileName);
        ValidationUtils.validarCampoObrigatorio(materialEstudo.getUrlArquivo(), "URL do arquivo é obrigatória");

        materialEstudoRepository.save(materialEstudo);
    }

    public MaterialEstudo editarMaterialEstudo(MaterialEstudo materialEstudo, MultipartFile file) {
        ValidationUtils.validarCampoObrigatorio(materialEstudo, "Material de estudo é obrigatório");
        ValidationUtils.validarCampoObrigatorio(materialEstudo.getId(), "ID do material de estudo é obrigatório");
        ValidationUtils.validarCampoStringObrigatorio(materialEstudo.getTitulo(), "Título é obrigatório");
        ValidationUtils.validarCampoStringObrigatorio(materialEstudo.getDescricao(), "Descrição é obrigatória");
        ValidationUtils.validarCampoObrigatorio(materialEstudo.getTurma(), "Turma é obrigatória");

        MaterialEstudo materialExistente = materialEstudoRepository.findById(materialEstudo.getId())
                .orElseThrow(() -> new IllegalArgumentException("Material de estudo não encontrado com ID: " + materialEstudo.getId()));

        if (file != null && !file.isEmpty()) {
            String fileName = fileUploadService.salvarDocumento(file);
            materialExistente.setUrlArquivo(fileName);
        }

        materialExistente.setTitulo(materialEstudo.getTitulo());
        materialExistente.setDescricao(materialEstudo.getDescricao());
        if (materialEstudo.getDataUpload() != null) {
            materialExistente.setDataUpload(materialEstudo.getDataUpload());
        }
        materialExistente.setTurma(materialEstudo.getTurma());

        ValidationUtils.validarCampoObrigatorio(materialExistente.getUrlArquivo(), "URL do arquivo é obrigatória");

        return materialEstudoRepository.save(materialExistente);
    }

    public List<MaterialEstudo> listarPorTurma(UUID turmaId) {
        ValidationUtils.validarCampoObrigatorio(turmaId, "ID da turma é obrigatório");
        return materialEstudoRepository.findByTurmaId(turmaId);
    } 
    
    public void excluirMaterialEstudo(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, "ID do material de estudo é obrigatório");
        materialEstudoRepository.deleteById(id);
    }
    
}
