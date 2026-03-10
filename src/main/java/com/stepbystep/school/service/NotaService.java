package com.stepbystep.school.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.stepbystep.school.model.Aluno;
import com.stepbystep.school.model.Nota;
import com.stepbystep.school.repository.NotaRepository;
import com.stepbystep.school.util.ValidationUtils;

@Service
public class NotaService {

    private static final String CAMPO_BIMESTRE = "Bimestre";
    private static final String MSG_NOTA_NAO_ENCONTRADA = "Nota não encontrada com ID: ";

    private final AlunoService alunoService;
    private final NotaRepository notaRepository;

    public NotaService(AlunoService alunoService, NotaRepository notaRepository) {
        this.alunoService = alunoService;
        this.notaRepository = notaRepository;
    }

    public Nota cadastrarNota(UUID alunoId, Nota nota) {
        ValidationUtils.validarCampoObrigatorio(alunoId, "ID do aluno");
        ValidationUtils.validarCampoObrigatorio(nota, "Nota");
        ValidationUtils.validarCampoObrigatorio(nota.getValor(), "Valor da nota");
        ValidationUtils.validarCampoObrigatorio(nota.getBimestre(), CAMPO_BIMESTRE);

        if (nota.getBimestre() < 1 || nota.getBimestre() > 4) {
            throw new IllegalArgumentException("Bimestre deve ser entre 1 e 4");
        }

        Aluno aluno = alunoService.obterAlunoPorId(alunoId);
        nota.setAluno(aluno);

        return notaRepository.save(nota);
    }

    public Nota editarNota(UUID notaId, Nota nota) {
        ValidationUtils.validarCampoObrigatorio(notaId, "ID da nota");
        ValidationUtils.validarCampoObrigatorio(nota, "Nota");
        ValidationUtils.validarCampoObrigatorio(nota.getValor(), "Valor da nota");
        ValidationUtils.validarCampoObrigatorio(nota.getBimestre(), CAMPO_BIMESTRE);
        if (nota.getBimestre() < 1 || nota.getBimestre() > 4) {
            throw new IllegalArgumentException("Bimestre deve ser entre 1 e 4");
        }

        Nota notaExistente = notaRepository.findById(notaId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_NOTA_NAO_ENCONTRADA + notaId));

        notaExistente.setValor(nota.getValor());
        notaExistente.setBimestre(nota.getBimestre());
        notaExistente.setDescricao(nota.getDescricao());

        return notaRepository.save(notaExistente);
    }
     
    public void excluirNota(UUID notaId) {
        ValidationUtils.validarCampoObrigatorio(notaId, "ID da nota");
        if (!notaRepository.existsById(notaId)) {
            throw new IllegalArgumentException(MSG_NOTA_NAO_ENCONTRADA + notaId);
        }
        notaRepository.deleteById(notaId);
    }
}
