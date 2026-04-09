package com.stepbystep.school.config;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.stepbystep.school.enums.Role;
import com.stepbystep.school.model.Aluno;
import com.stepbystep.school.model.Usuario;
import com.stepbystep.school.repository.AlunoRepository;
import com.stepbystep.school.repository.UsuarioRepository;

@Component
public class AlunoUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AlunoUserInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final AlunoRepository alunoRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${aluno.test.email:aluno@stepbystep.com.br}")
    private String alunoEmail;

    @Value("${aluno.test.name:Aluno Teste}")
    private String alunoName;

    @Value("${aluno.test.password:}")
    private String alunoPassword;

    public AlunoUserInitializer(
        UsuarioRepository usuarioRepository,
        AlunoRepository alunoRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.alunoRepository = alunoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (alunoPassword == null || alunoPassword.trim().isEmpty()) {
            log.warn("aluno.test.password não configurada. Usuário aluno de teste não foi criado automaticamente.");
            return;
        }

        String email = alunoEmail.trim().toLowerCase(Locale.ROOT);
        String senhaPlana = alunoPassword.trim();

        Usuario usuarioExistente = usuarioRepository.findByEmail(email).orElse(null);
        if (usuarioExistente != null) {
            atualizarUsuarioExistente(usuarioExistente, senhaPlana, email);
            return;
        }

        criarUsuarioAlunoTeste(email, senhaPlana);
    }

    private void atualizarUsuarioExistente(Usuario usuarioExistente, String senhaPlana, String email) {
        boolean precisaAtualizar = false;

        if (!passwordEncoder.matches(senhaPlana, usuarioExistente.getSenha())) {
            usuarioExistente.setSenha(passwordEncoder.encode(senhaPlana));
            precisaAtualizar = true;
        }

        if (!usuarioExistente.isAtivo()) {
            usuarioExistente.setAtivo(true);
            precisaAtualizar = true;
        }

        if (usuarioExistente.getRole() != Role.ALUNO) {
            usuarioExistente.setRole(Role.ALUNO);
            precisaAtualizar = true;
        }

        if (usuarioExistente.getAluno() == null) {
            Aluno alunoVinculo = localizarAlunoParaVinculo();
            if (alunoVinculo != null) {
                usuarioExistente.setAluno(alunoVinculo);
                precisaAtualizar = true;
                log.info("Usuário aluno de teste vinculado ao aluno {}", alunoVinculo.getNome());
            }
        }

        if (precisaAtualizar) {
            usuarioRepository.save(usuarioExistente);
            log.info("Usuário aluno de teste atualizado com sucesso para o e-mail {}", email);
        }
    }

    private void criarUsuarioAlunoTeste(String email, String senhaPlana) {
        Usuario aluno = Usuario.builder()
            .nome(alunoName.trim())
            .email(email)
            .senha(passwordEncoder.encode(senhaPlana))
            .role(Role.ALUNO)
            .ativo(true)
            .aluno(localizarAlunoParaVinculo())
            .build();

        usuarioRepository.save(aluno);
        log.info("Usuário aluno de teste criado com sucesso para o e-mail {}", email);
    }

    private Aluno localizarAlunoParaVinculo() {
        String nomeAluno = alunoName == null ? "" : alunoName.trim().toLowerCase(Locale.ROOT);
        if (nomeAluno.isBlank()) {
            return null;
        }

        java.util.List<Aluno> candidatos = alunoRepository.findAll().stream()
            .filter(aluno -> aluno.getNome() != null && aluno.getNome().trim().toLowerCase(Locale.ROOT).equals(nomeAluno))
            .filter(aluno -> usuarioRepository.findByAlunoId(aluno.getId()).isEmpty())
            .toList();

        if (candidatos.size() == 1) {
            return candidatos.get(0);
        }

        if (candidatos.size() > 1) {
            log.warn("Encontrados {} alunos com o nome '{}'. Vínculo automático não realizado.", candidatos.size(), alunoName);
        }

        return null;
    }
}
