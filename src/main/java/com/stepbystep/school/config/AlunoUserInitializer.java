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
import com.stepbystep.school.model.Usuario;
import com.stepbystep.school.repository.UsuarioRepository;

@Component
public class AlunoUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AlunoUserInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${aluno.test.email:aluno@stepbystep.com.br}")
    private String alunoEmail;

    @Value("${aluno.test.name:Aluno Teste}")
    private String alunoName;

    @Value("${aluno.test.password:}")
    private String alunoPassword;

    public AlunoUserInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
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

            if (precisaAtualizar) {
                usuarioRepository.save(usuarioExistente);
                log.info("Usuário aluno de teste atualizado com sucesso para o e-mail {}", email);
            }

            return;
        }

        Usuario aluno = Usuario.builder()
            .nome(alunoName.trim())
            .email(email)
            .senha(passwordEncoder.encode(senhaPlana))
            .role(Role.ALUNO)
            .ativo(true)
            .aluno(null)
            .build();

        usuarioRepository.save(aluno);
        log.info("Usuário aluno de teste criado com sucesso para o e-mail {}", email);
    }
}
