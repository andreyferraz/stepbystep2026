package com.stepbystep.school.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.stepbystep.school.model.Turma;
import com.stepbystep.school.repository.TurmaRepository;
import com.stepbystep.school.util.ValidationUtils;

@Service
public class TurmaService {

    private static final String CAMPO_ID_TURMA = "ID da Turma";
    private static final String MSG_TURMA_NAO_ENCONTRADA = "Turma não encontrada com ID: ";

    private final TurmaRepository turmaRepository;

    public TurmaService(TurmaRepository turmaRepository) {
        this.turmaRepository = turmaRepository;
    }

    public Turma criarTurma(Turma turma) {
        ValidationUtils.validarCampoObrigatorio(turma, "Turma");
        ValidationUtils.validarCampoStringObrigatorio(turma.getNome(), "Nome da Turma");
        ValidationUtils.validarCampoStringObrigatorio(turma.getHorario(), "Horário da Turma");
        ValidationUtils.validarCampoStringObrigatorio(turma.getDiasSemana(), "Dias da Semana da Turma");
        ValidationUtils.validarCampoObrigatorio(turma.getNivelAtual(), "Nível da Turma");
        validarVagas(turma.getTotalVagas(), turma.getVagasOcupadas());

        turma.setId(null);
        turma.setNome(turma.getNome().trim());
        turma.setHorario(turma.getHorario().trim());
        turma.setDiasSemana(turma.getDiasSemana().trim());
        turma.setObservacoes(normalizarTextoOpcional(turma.getObservacoes()));

        return turmaRepository.save(turma);
    }

    public Turma editarTurma(UUID id, Turma turmaAtualizada) {
        ValidationUtils.validarCampoObrigatorio(id, CAMPO_ID_TURMA);
        ValidationUtils.validarCampoObrigatorio(turmaAtualizada, "Turma");
        ValidationUtils.validarCampoStringObrigatorio(turmaAtualizada.getNome(), "Nome da Turma");
        ValidationUtils.validarCampoStringObrigatorio(turmaAtualizada.getHorario(), "Horário da Turma");
        ValidationUtils.validarCampoStringObrigatorio(turmaAtualizada.getDiasSemana(), "Dias da Semana da Turma");
        ValidationUtils.validarCampoObrigatorio(turmaAtualizada.getNivelAtual(), "Nível da Turma");

        Turma turmaExistente = turmaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(MSG_TURMA_NAO_ENCONTRADA + id));

        int alunosVinculados = turmaExistente.getAlunos() == null ? 0 : turmaExistente.getAlunos().size();
        validarVagas(turmaAtualizada.getTotalVagas(), alunosVinculados);

        turmaExistente.setNome(turmaAtualizada.getNome().trim());
        turmaExistente.setHorario(turmaAtualizada.getHorario().trim());
        turmaExistente.setDiasSemana(turmaAtualizada.getDiasSemana().trim());
        turmaExistente.setNivelAtual(turmaAtualizada.getNivelAtual());
        turmaExistente.setTotalVagas(turmaAtualizada.getTotalVagas());
        turmaExistente.setVagasOcupadas(alunosVinculados);
        turmaExistente.setObservacoes(normalizarTextoOpcional(turmaAtualizada.getObservacoes()));

        return turmaRepository.save(turmaExistente);
    }

    public List<Turma> listarTurmas() {
        return turmaRepository.findAll();
    }

    public List<Turma> listarTurmasFiltradas(String termoBusca) {
        String termoNormalizado = termoBusca == null ? "" : termoBusca.trim().toLowerCase(Locale.ROOT);

        return turmaRepository.findAll().stream()
            .filter(turma -> termoNormalizado.isEmpty()
                || contemTexto(turma.getNome(), termoNormalizado)
                || contemTexto(turma.getHorario(), termoNormalizado)
                || contemTexto(turma.getNivelAtual() == null ? null : turma.getNivelAtual().name(), termoNormalizado))
            .sorted(Comparator.comparing(Turma::getNome, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public Turma obterTurmaPorId(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, CAMPO_ID_TURMA);
        return turmaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(MSG_TURMA_NAO_ENCONTRADA + id));
    }

    public void excluirTurma(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, CAMPO_ID_TURMA);
        Turma turmaExistente = turmaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(MSG_TURMA_NAO_ENCONTRADA + id));

        turmaRepository.delete(turmaExistente);
    }

    private void validarVagas(int totalVagas, int vagasOcupadas) {
        if (totalVagas <= 0) {
            throw new IllegalArgumentException("Total de vagas da turma deve ser maior que zero.");
        }

        if (vagasOcupadas < 0) {
            throw new IllegalArgumentException("Vagas ocupadas da turma não pode ser negativo.");
        }

        if (vagasOcupadas > totalVagas) {
            throw new IllegalArgumentException("Vagas ocupadas não pode ser maior que o total de vagas da turma.");
        }
    }

    private String normalizarTextoOpcional(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private boolean contemTexto(String origem, String termoNormalizado) {
        return origem != null && origem.toLowerCase(Locale.ROOT).contains(termoNormalizado);
    }
}
