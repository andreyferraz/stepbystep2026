package com.stepbystep.school.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.stepbystep.school.model.Aluno;
import com.stepbystep.school.repository.AlunoRepository;
import com.stepbystep.school.util.ValidationUtils;


@Service
public class AlunoService {

    private final AlunoRepository alunoRepository;

    public AlunoService(AlunoRepository alunoRepository) {
        this.alunoRepository = alunoRepository;
    }

    public void cadastrarAluno(Aluno aluno) {
        // Aqui você pode adicionar validações específicas para o aluno, se necessário
        ValidationUtils.validarCampoObrigatorio(aluno, "Aluno");
        ValidationUtils.validarCampoStringObrigatorio(aluno.getNome(), "Nome do Aluno");
        ValidationUtils.validarCampoObrigatorio(aluno.getDataNascimento(), "Data de Nascimento do Aluno");
        ValidationUtils.validarCampoStringObrigatorio(aluno.getTelefone(), "Telefone do Aluno");
        ValidationUtils.validarCampoObrigatorio(aluno.getNivelAtual(), "Nível Atual do Aluno");
        ValidationUtils.validarCampoObrigatorio(aluno.getTurma(), "Turma do Aluno");
        ValidationUtils.validarListaObrigatoria(aluno.getMensalidades(), "Mensalidades do Aluno");
        alunoRepository.save(aluno);
    }

    public List<Aluno> listarAlunos() {
        return alunoRepository.findAll();
    }

    public Aluno obterAlunoPorId(UUID id) {
        return alunoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado com ID: " + id));
    }

    public void atualizarAluno(UUID id, Aluno alunoAtualizado) {
        ValidationUtils.validarCampoObrigatorio(alunoAtualizado, "Aluno");
        ValidationUtils.validarCampoStringObrigatorio(alunoAtualizado.getNome(), "Nome do Aluno");
        ValidationUtils.validarCampoObrigatorio(alunoAtualizado.getDataNascimento(), "Data de Nascimento do Aluno");
        ValidationUtils.validarCampoStringObrigatorio(alunoAtualizado.getTelefone(), "Telefone do Aluno");
        ValidationUtils.validarCampoObrigatorio(alunoAtualizado.getNivelAtual(), "Nível Atual do Aluno");
        ValidationUtils.validarCampoObrigatorio(alunoAtualizado.getTurma(), "Turma do Aluno");
        ValidationUtils.validarListaObrigatoria(alunoAtualizado.getMensalidades(), "Mensalidades do Aluno");
        ValidationUtils.validarListaObrigatoria(alunoAtualizado.getNotas(), "Notas do Aluno");

        Aluno alunoExistente = obterAlunoPorId(id);
        // Atualize os campos do aluno existente com os valores do aluno atualizado
        alunoExistente.setNome(alunoAtualizado.getNome());
        alunoExistente.setDataNascimento(alunoAtualizado.getDataNascimento());
        alunoExistente.setTelefone(alunoAtualizado.getTelefone());
        alunoExistente.setNivelAtual(alunoAtualizado.getNivelAtual());
        alunoExistente.setTurma(alunoAtualizado.getTurma());
        alunoExistente.setNotas(alunoAtualizado.getNotas());
        alunoExistente.setMensalidades(alunoAtualizado.getMensalidades());
        // Salve as alterações no banco de dados
        alunoRepository.save(alunoExistente);
    }

    public void excluirAluno(UUID id) {
        Aluno alunoExistente = obterAlunoPorId(id);
        alunoRepository.delete(alunoExistente);
    }

}
