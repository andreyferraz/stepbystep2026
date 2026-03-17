package com.stepbystep.school.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.stepbystep.school.enums.Role;
import com.stepbystep.school.model.Usuario;
import com.stepbystep.school.repository.UsuarioRepository;
import com.stepbystep.school.util.ValidationUtils;

@Service
public class UsuarioService {

    private static final String CAMPO_ID_USUARIO = "ID do Usuário";
    private static final String MSG_USUARIO_NAO_ENCONTRADO = "Usuário não encontrado com ID: ";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario criarUsuario(Usuario usuario) {
        ValidationUtils.validarCampoObrigatorio(usuario, "Usuário");
        ValidationUtils.validarCampoStringObrigatorio(usuario.getNome(), "Nome do Usuário");
        ValidationUtils.validarCampoStringObrigatorio(usuario.getEmail(), "E-mail do Usuário");
        ValidationUtils.validarCampoStringObrigatorio(usuario.getSenha(), "Senha do Usuário");
        ValidationUtils.validarCampoObrigatorio(usuario.getRole(), "Perfil do Usuário");

        if (usuario.getRole() == Role.ALUNO) {
            ValidationUtils.validarCampoObrigatorio(usuario.getAluno(), "Aluno do Usuário");
        }

        usuario.setId(null);
        usuario.setNome(usuario.getNome().trim());
        usuario.setEmail(usuario.getEmail().trim().toLowerCase(Locale.ROOT));
        usuario.setSenha(encodeIfNeeded(usuario.getSenha()));

        return usuarioRepository.save(usuario);
    }

    public Usuario editarUsuario(UUID id, Usuario usuarioAtualizado) {
        ValidationUtils.validarCampoObrigatorio(id, CAMPO_ID_USUARIO);
        ValidationUtils.validarCampoObrigatorio(usuarioAtualizado, "Usuário");
        ValidationUtils.validarCampoStringObrigatorio(usuarioAtualizado.getNome(), "Nome do Usuário");
        ValidationUtils.validarCampoStringObrigatorio(usuarioAtualizado.getEmail(), "E-mail do Usuário");
        ValidationUtils.validarCampoStringObrigatorio(usuarioAtualizado.getSenha(), "Senha do Usuário");
        ValidationUtils.validarCampoObrigatorio(usuarioAtualizado.getRole(), "Perfil do Usuário");

        if (usuarioAtualizado.getRole() == Role.ALUNO) {
            ValidationUtils.validarCampoObrigatorio(usuarioAtualizado.getAluno(), "Aluno do Usuário");
        }

        Usuario usuarioExistente = usuarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(MSG_USUARIO_NAO_ENCONTRADO + id));

        usuarioExistente.setNome(usuarioAtualizado.getNome().trim());
        usuarioExistente.setEmail(usuarioAtualizado.getEmail().trim().toLowerCase(Locale.ROOT));
        usuarioExistente.setSenha(encodeIfNeeded(usuarioAtualizado.getSenha()));
        usuarioExistente.setRole(usuarioAtualizado.getRole());
        usuarioExistente.setAtivo(usuarioAtualizado.isAtivo());

        if (usuarioAtualizado.getRole() == Role.ALUNO) {
            usuarioExistente.setAluno(usuarioAtualizado.getAluno());
        } else {
            usuarioExistente.setAluno(null);
        }

        return usuarioRepository.save(usuarioExistente);
    }

    public void excluirUsuario(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, CAMPO_ID_USUARIO);
        Usuario usuarioExistente = usuarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(MSG_USUARIO_NAO_ENCONTRADO + id));

        usuarioRepository.delete(usuarioExistente);
    }

    public void redefinirSenhaUsuario(UUID id, String novaSenha) {
        ValidationUtils.validarCampoObrigatorio(id, CAMPO_ID_USUARIO);
        ValidationUtils.validarCampoStringObrigatorio(novaSenha, "Nova senha do Usuário");

        Usuario usuarioExistente = usuarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(MSG_USUARIO_NAO_ENCONTRADO + id));

        usuarioExistente.setSenha(encodeIfNeeded(novaSenha));
        usuarioRepository.save(usuarioExistente);
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    private String encodeIfNeeded(String senha) {
        String senhaLimpa = senha.trim();

        if (senhaLimpa.startsWith("$2a$") || senhaLimpa.startsWith("$2b$") || senhaLimpa.startsWith("$2y$")) {
            return senhaLimpa;
        }

        return passwordEncoder.encode(senhaLimpa);
    }
}
