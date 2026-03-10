package com.stepbystep.school.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.stepbystep.school.model.PreInscricao;
import com.stepbystep.school.repository.PreInscricaoRepository;
import com.stepbystep.school.util.ValidationUtils;

@Service
public class PreInscricaoService {

    private final PreInscricaoRepository preInscricaoRepository;

    public PreInscricaoService(PreInscricaoRepository preInscricaoRepository) {
        this.preInscricaoRepository = preInscricaoRepository;
    }

    public PreInscricao criarPreInscricao(String nomeInteressado, String whatsapp, String mensagem) {
        ValidationUtils.validarCampoStringObrigatorio(nomeInteressado, "Nome");
        ValidationUtils.validarCampoStringObrigatorio(whatsapp, "WhatsApp");

        PreInscricao preInscricao = PreInscricao.builder()
                .nomeInteressado(nomeInteressado.trim())
                .whatsapp(whatsapp.trim())
                .mensagem(mensagem == null ? null : mensagem.trim())
                .dataLead(LocalDateTime.now())
                .respondido(false)
                .build();

        return preInscricaoRepository.save(preInscricao);
    }

    public List<PreInscricao> listarPreInscricoes() {
        return preInscricaoRepository.findAllByOrderByDataLeadDesc();
    }

    public void marcarComoRespondido(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, "ID da pré-inscrição");

        PreInscricao lead = preInscricaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pré-inscrição não encontrada para o ID: " + id));

        lead.setRespondido(true);
        preInscricaoRepository.save(lead);
    }

}
