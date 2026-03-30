package com.stepbystep.school.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.stepbystep.school.model.PreInscricao;
import com.stepbystep.school.repository.PreInscricaoRepository;
import com.stepbystep.school.util.ValidationUtils;

@Service
public class PreInscricaoService {

    private static final String STATUS_PENDENTE = "pendente";
    private static final String PRE_INSCRICAO = "ID da pré-inscrição";

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

    public List<PreInscricao> listarPreInscricoesFiltradas(String busca, String status) {
        String termoBusca = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
        String statusNormalizado = normalizarStatus(status);

        return listarPreInscricoes().stream()
            .filter(item -> termoBusca.isEmpty()
                || contemTexto(item.getNomeInteressado(), termoBusca)
                || contemTexto(item.getWhatsapp(), termoBusca)
                || contemTexto(item.getMensagem(), termoBusca))
            .filter(item -> statusNormalizado.isEmpty() || correspondeStatus(item, statusNormalizado))
            .toList();
    }

    public long contarPendentes(List<PreInscricao> preInscricoes) {
        return preInscricoes.stream().filter(item -> !item.isRespondido()).count();
    }

    public long contarContatadas(List<PreInscricao> preInscricoes) {
        return preInscricoes.stream().filter(PreInscricao::isRespondido).count();
    }

    public long contarNovasUltimosDias(List<PreInscricao> preInscricoes, int dias) {
        LocalDateTime limite = LocalDateTime.now().minusDays(Math.max(1, dias));
        return preInscricoes.stream()
            .filter(item -> item.getDataLead() != null)
            .filter(item -> !item.getDataLead().isBefore(limite))
            .count();
    }

    public PreInscricao criarPreInscricaoAdmin(
        String nomeInteressado,
        String whatsapp,
        String interesse,
        String origem,
        LocalDateTime dataLead,
        String status,
        String mensagem
    ) {
        ValidationUtils.validarCampoStringObrigatorio(nomeInteressado, "Nome");
        ValidationUtils.validarCampoStringObrigatorio(whatsapp, "WhatsApp");

        String statusNormalizado = normalizarStatus(status);
        boolean respondido = !statusNormalizado.isBlank() && !STATUS_PENDENTE.equals(statusNormalizado);

        PreInscricao preInscricao = PreInscricao.builder()
            .nomeInteressado(nomeInteressado.trim())
            .whatsapp(whatsapp.trim())
            .mensagem(construirMensagemAdmin(interesse, origem, mensagem))
            .dataLead(dataLead == null ? LocalDateTime.now() : dataLead)
            .respondido(respondido)
            .build();

        return preInscricaoRepository.save(preInscricao);
    }

    public void registrarContatoLead(UUID id, String status) {
        ValidationUtils.validarCampoObrigatorio(id, PRE_INSCRICAO);

        PreInscricao lead = preInscricaoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Pré-inscrição não encontrada para o ID: " + id));

        String statusNormalizado = normalizarStatus(status);
        lead.setRespondido(!STATUS_PENDENTE.equals(statusNormalizado));
        preInscricaoRepository.save(lead);
    }

    public void marcarComoRespondido(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, PRE_INSCRICAO);

        PreInscricao lead = preInscricaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pré-inscrição não encontrada para o ID: " + id));

        lead.setRespondido(true);
        preInscricaoRepository.save(lead);
    }

    public void excluirPreInscricao(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, PRE_INSCRICAO);

        if (!preInscricaoRepository.existsById(id)) {
            throw new IllegalArgumentException("Pré-inscrição não encontrada para o ID: " + id);
        }

        preInscricaoRepository.deleteById(id);
    }

    private boolean correspondeStatus(PreInscricao item, String status) {
        if (STATUS_PENDENTE.equals(status)) {
            return !item.isRespondido();
        }

        if ("contatado".equals(status) || "respondido".equals(status)) {
            return item.isRespondido();
        }

        return true;
    }

    private boolean contemTexto(String origem, String termo) {
        return origem != null && origem.toLowerCase(Locale.ROOT).contains(termo);
    }

    private String normalizarStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
    }

    private String construirMensagemAdmin(String interesse, String origem, String mensagem) {
        String interesseNormalizado = interesse == null ? "" : interesse.trim();
        String origemNormalizada = origem == null ? "" : origem.trim();
        String mensagemNormalizada = mensagem == null ? "" : mensagem.trim();

        StringBuilder builder = new StringBuilder();
        if (!interesseNormalizado.isBlank()) {
            builder.append("Interesse: ").append(interesseNormalizado);
        }

        if (!origemNormalizada.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }
            builder.append("Origem: ").append(origemNormalizada);
        }

        if (!mensagemNormalizada.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }
            builder.append("Observações: ").append(mensagemNormalizada);
        }

        return builder.toString();
    }

}
