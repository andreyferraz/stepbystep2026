package com.stepbystep.school.service;

import java.util.List;
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

        turma.setId(null);
        turma.setNome(turma.getNome().trim());
        turma.setHorario(turma.getHorario().trim());
        turma.setDiasSemana(turma.getDiasSemana().trim());

        return turmaRepository.save(turma);
    }

    public Turma editarTurma(UUID id, Turma turmaAtualizada) {
        ValidationUtils.validarCampoObrigatorio(id, CAMPO_ID_TURMA);
        ValidationUtils.validarCampoObrigatorio(turmaAtualizada, "Turma");
        ValidationUtils.validarCampoStringObrigatorio(turmaAtualizada.getNome(), "Nome da Turma");
        ValidationUtils.validarCampoStringObrigatorio(turmaAtualizada.getHorario(), "Horário da Turma");
        ValidationUtils.validarCampoStringObrigatorio(turmaAtualizada.getDiasSemana(), "Dias da Semana da Turma");

        Turma turmaExistente = turmaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(MSG_TURMA_NAO_ENCONTRADA + id));

        turmaExistente.setNome(turmaAtualizada.getNome().trim());
        turmaExistente.setHorario(turmaAtualizada.getHorario().trim());
        turmaExistente.setDiasSemana(turmaAtualizada.getDiasSemana().trim());

        return turmaRepository.save(turmaExistente);
    }

    public List<Turma> listarTurmas() {
        return turmaRepository.findAll();
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
}
