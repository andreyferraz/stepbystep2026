package com.stepbystep.school.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.stepbystep.school.model.MaterialEstudo;
import com.stepbystep.school.repository.MaterialEstudoRepository;
import com.stepbystep.school.util.ValidationUtils;

@Service
public class MaterialEstudoService {

    private static final String CAMPO_ID_MATERIAL_OBRIGATORIO = "ID do material de estudo é obrigatório";
    private static final String MSG_MATERIAL_NAO_ENCONTRADO = "Material de estudo não encontrado com ID: ";

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
        ValidationUtils.validarCampoObrigatorio(materialEstudo.getId(), CAMPO_ID_MATERIAL_OBRIGATORIO);
        ValidationUtils.validarCampoStringObrigatorio(materialEstudo.getTitulo(), "Título é obrigatório");
        ValidationUtils.validarCampoStringObrigatorio(materialEstudo.getDescricao(), "Descrição é obrigatória");
        ValidationUtils.validarCampoObrigatorio(materialEstudo.getTurma(), "Turma é obrigatória");

        MaterialEstudo materialExistente = materialEstudoRepository.findById(materialEstudo.getId())
            .orElseThrow(() -> new IllegalArgumentException(MSG_MATERIAL_NAO_ENCONTRADO + materialEstudo.getId()));

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

    public List<MaterialEstudo> listarMateriaisFiltrados(String termoBusca, UUID turmaId) {
        String termoNormalizado = termoBusca == null ? "" : termoBusca.trim().toLowerCase(Locale.ROOT);

        return materialEstudoRepository.findAllByOrderByDataUploadDesc().stream()
            .filter(material -> turmaId == null
                || (material.getTurma() != null && turmaId.equals(material.getTurma().getId())))
            .filter(material -> termoNormalizado.isEmpty()
                || contemTexto(material.getTitulo(), termoNormalizado)
                || contemTexto(material.getDescricao(), termoNormalizado)
                || contemTexto(material.getTurma() == null ? null : material.getTurma().getNome(), termoNormalizado))
            .sorted(Comparator.comparing(MaterialEstudo::getDataUpload,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    public long contarUploadsUltimosDias(List<MaterialEstudo> materiais, int dias) {
        LocalDateTime limite = LocalDateTime.now().minusDays(dias);
        return materiais.stream()
            .filter(material -> material.getDataUpload() != null)
            .filter(material -> !material.getDataUpload().isBefore(limite))
            .count();
    }

    public MaterialEstudo obterPorId(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, CAMPO_ID_MATERIAL_OBRIGATORIO);
        return materialEstudoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(MSG_MATERIAL_NAO_ENCONTRADO + id));
    }
    
    public void excluirMaterialEstudo(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, CAMPO_ID_MATERIAL_OBRIGATORIO);
        MaterialEstudo material = materialEstudoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(MSG_MATERIAL_NAO_ENCONTRADO + id));

        try {
            fileUploadService.removerArquivo(material.getUrlArquivo());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Não foi possível excluir o arquivo do material.");
        }

        materialEstudoRepository.delete(material);
    }

    private boolean contemTexto(String origem, String termoNormalizado) {
        return origem != null && origem.toLowerCase(Locale.ROOT).contains(termoNormalizado);
    }
    
}
