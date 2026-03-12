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
public class AdminUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@stepbystep.com.br}")
    private String adminEmail;

    @Value("${admin.name:Administrador}")
    private String adminName;

    @Value("${admin.password:}")
    private String adminPassword;

    public AdminUserInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminPassword == null || adminPassword.trim().isEmpty()) {
            log.warn("admin.password não configurada. Usuário principal não foi criado automaticamente.");
            return;
        }

        String email = adminEmail.trim().toLowerCase(Locale.ROOT);
        String senhaPlana = adminPassword.trim();

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

            if (usuarioExistente.getRole() != Role.ADMIN) {
                usuarioExistente.setRole(Role.ADMIN);
                precisaAtualizar = true;
            }

            if (precisaAtualizar) {
                usuarioRepository.save(usuarioExistente);
                log.info("Usuário principal atualizado com sucesso para o e-mail {}", email);
            }

            return;
        }

        Usuario admin = Usuario.builder()
            .nome(adminName.trim())
            .email(email)
            .senha(passwordEncoder.encode(senhaPlana))
            .role(Role.ADMIN)
            .ativo(true)
            .aluno(null)
            .build();

        usuarioRepository.save(admin);
        log.info("Usuário principal criado com sucesso para o e-mail {}", email);
    }
}
