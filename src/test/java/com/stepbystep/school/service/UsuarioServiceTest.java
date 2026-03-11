package com.stepbystep.school.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stepbystep.school.enums.Role;
import com.stepbystep.school.model.Aluno;
import com.stepbystep.school.model.Usuario;
import com.stepbystep.school.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    private UUID usuarioId;
    private Aluno aluno;
    private Usuario usuarioValidoAdmin;
    private Usuario usuarioValidoAluno;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        aluno = Aluno.builder()
                .id(UUID.randomUUID())
                .nome("Aluno Vinculado")
                .build();

        usuarioValidoAdmin = Usuario.builder()
                .id(usuarioId)
                .nome("Administrador")
                .email("admin@step.com")
                .senha("123456")
                .role(Role.ADMIN)
                .ativo(true)
                .build();

        usuarioValidoAluno = Usuario.builder()
                .id(usuarioId)
                .nome("Aluno User")
                .email("aluno@step.com")
                .senha("123456")
                .role(Role.ALUNO)
                .ativo(true)
                .aluno(aluno)
                .build();
    }

    @Nested
    @DisplayName("criarUsuario")
    class CriarUsuario {

        @Test
        @DisplayName("Deve criar usuário admin com sucesso e normalizar campos")
        void deveCriarUsuarioAdminComSucessoENormalizarCampos() {
            Usuario novoUsuario = Usuario.builder()
                    .id(UUID.randomUUID())
                    .nome("  Admin Geral  ")
                    .email("  ADMIN@STEP.COM  ")
                    .senha("  123456  ")
                    .role(Role.ADMIN)
                    .ativo(true)
                    .aluno(aluno)
                    .build();

            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Usuario resultado = usuarioService.criarUsuario(novoUsuario);

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository, times(1)).save(captor.capture());
            Usuario enviadoParaSalvar = captor.getValue();

            assertNull(enviadoParaSalvar.getId());
            assertEquals("Admin Geral", enviadoParaSalvar.getNome());
            assertEquals("admin@step.com", enviadoParaSalvar.getEmail());
            assertEquals("123456", enviadoParaSalvar.getSenha());
            assertEquals(Role.ADMIN, enviadoParaSalvar.getRole());
            assertNotNull(resultado);
        }

        @Test
        @DisplayName("Deve criar usuário aluno quando vínculo é informado")
        void deveCriarUsuarioAlunoComVinculo() {
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Usuario resultado = usuarioService.criarUsuario(usuarioValidoAluno);

            assertNotNull(resultado);
            assertEquals(Role.ALUNO, resultado.getRole());
            assertEquals(aluno, resultado.getAluno());
            verify(usuarioRepository, times(1)).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário é nulo")
        void deveLancarExcecaoQuandoUsuarioNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> usuarioService.criarUsuario(null));

            assertTrue(ex.getMessage().contains("Usuário"));
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando nome é vazio")
        void deveLancarExcecaoQuandoNomeVazio() {
            usuarioValidoAdmin.setNome("   ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> usuarioService.criarUsuario(usuarioValidoAdmin));

            assertTrue(ex.getMessage().contains("Nome do Usuário"));
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando email é nulo")
        void deveLancarExcecaoQuandoEmailNulo() {
            usuarioValidoAdmin.setEmail(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> usuarioService.criarUsuario(usuarioValidoAdmin));

            assertTrue(ex.getMessage().contains("E-mail do Usuário"));
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando senha é vazia")
        void deveLancarExcecaoQuandoSenhaVazia() {
            usuarioValidoAdmin.setSenha(" ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> usuarioService.criarUsuario(usuarioValidoAdmin));

            assertTrue(ex.getMessage().contains("Senha do Usuário"));
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando perfil é nulo")
        void deveLancarExcecaoQuandoPerfilNulo() {
            usuarioValidoAdmin.setRole(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> usuarioService.criarUsuario(usuarioValidoAdmin));

            assertTrue(ex.getMessage().contains("Perfil do Usuário"));
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando perfil aluno sem vínculo")
        void deveLancarExcecaoQuandoPerfilAlunoSemVinculo() {
            usuarioValidoAluno.setAluno(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> usuarioService.criarUsuario(usuarioValidoAluno));

            assertTrue(ex.getMessage().contains("Aluno do Usuário"));
            verify(usuarioRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("editarUsuario")
    class EditarUsuario {

        @Test
        @DisplayName("Deve editar usuário com sucesso")
        void deveEditarUsuarioComSucesso() {
            Usuario existente = Usuario.builder()
                    .id(usuarioId)
                    .nome("Nome Antigo")
                    .email("old@step.com")
                    .senha("old")
                    .role(Role.ADMIN)
                    .ativo(false)
                    .aluno(null)
                    .build();

            Usuario atualizado = Usuario.builder()
                    .id(UUID.randomUUID())
                    .nome("  Novo Nome  ")
                    .email("  NOVO@STEP.COM  ")
                    .senha("  novaSenha  ")
                    .role(Role.ALUNO)
                    .ativo(true)
                    .aluno(aluno)
                    .build();

            when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(existente));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Usuario resultado = usuarioService.editarUsuario(usuarioId, atualizado);

            assertNotNull(resultado);
            assertEquals(usuarioId, existente.getId());
            assertEquals("Novo Nome", existente.getNome());
            assertEquals("novo@step.com", existente.getEmail());
            assertEquals("novaSenha", existente.getSenha());
            assertEquals(Role.ALUNO, existente.getRole());
            assertTrue(existente.isAtivo());
            assertEquals(aluno, existente.getAluno());
            verify(usuarioRepository, times(1)).findById(usuarioId);
            verify(usuarioRepository, times(1)).save(existente);
        }

        @Test
        @DisplayName("Deve limpar aluno quando perfil atualizado não é aluno")
        void deveLimparAlunoQuandoPerfilNaoAluno() {
            Usuario existente = Usuario.builder()
                    .id(usuarioId)
                    .nome("Nome Antigo")
                    .email("old@step.com")
                    .senha("old")
                    .role(Role.ALUNO)
                    .ativo(true)
                    .aluno(aluno)
                    .build();

            Usuario atualizado = Usuario.builder()
                    .nome("Gestor")
                    .email("gestor@step.com")
                    .senha("123")
                    .role(Role.ADMIN)
                    .ativo(true)
                    .aluno(aluno)
                    .build();

            when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(existente));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Usuario resultado = usuarioService.editarUsuario(usuarioId, atualizado);

            assertNotNull(resultado);
            assertEquals(Role.ADMIN, existente.getRole());
            assertNull(existente.getAluno());
            verify(usuarioRepository, times(1)).save(existente);
        }

        @Test
        @DisplayName("Deve lançar exceção quando ID é nulo")
        void deveLancarExcecaoQuandoIdNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> usuarioService.editarUsuario(null, usuarioValidoAdmin));

            assertTrue(ex.getMessage().contains("ID do Usuário"));
            verify(usuarioRepository, never()).findById(any());
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário atualizado é nulo")
        void deveLancarExcecaoQuandoUsuarioAtualizadoNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> usuarioService.editarUsuario(usuarioId, null));

            assertTrue(ex.getMessage().contains("Usuário"));
            verify(usuarioRepository, never()).findById(any());
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário não encontrado")
        void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
            when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> usuarioService.editarUsuario(usuarioId, usuarioValidoAdmin));

            assertTrue(ex.getMessage().contains("Usuário não encontrado"));
            verify(usuarioRepository, times(1)).findById(usuarioId);
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando perfil aluno sem vínculo")
        void deveLancarExcecaoQuandoPerfilAlunoSemVinculo() {
            usuarioValidoAluno.setAluno(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> usuarioService.editarUsuario(usuarioId, usuarioValidoAluno));

            assertTrue(ex.getMessage().contains("Aluno do Usuário"));
            verify(usuarioRepository, never()).findById(any());
            verify(usuarioRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("excluirUsuario")
    class ExcluirUsuario {

        @Test
        @DisplayName("Deve excluir usuário com sucesso")
        void deveExcluirUsuarioComSucesso() {
            when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioValidoAdmin));

            assertDoesNotThrow(() -> usuarioService.excluirUsuario(usuarioId));

            verify(usuarioRepository, times(1)).findById(usuarioId);
            verify(usuarioRepository, times(1)).delete(usuarioValidoAdmin);
        }

        @Test
        @DisplayName("Deve lançar exceção quando ID é nulo")
        void deveLancarExcecaoQuandoIdNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> usuarioService.excluirUsuario(null));

            assertTrue(ex.getMessage().contains("ID do Usuário"));
            verify(usuarioRepository, never()).findById(any());
            verify(usuarioRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário não existe")
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> usuarioService.excluirUsuario(usuarioId));

            assertTrue(ex.getMessage().contains("Usuário não encontrado"));
            verify(usuarioRepository, times(1)).findById(usuarioId);
            verify(usuarioRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("listarUsuarios")
    class ListarUsuarios {

        @Test
        @DisplayName("Deve retornar lista de usuários")
        void deveRetornarListaDeUsuarios() {
            when(usuarioRepository.findAll()).thenReturn(List.of(usuarioValidoAdmin, usuarioValidoAluno));

            List<Usuario> resultado = usuarioService.listarUsuarios();

            assertEquals(2, resultado.size());
            verify(usuarioRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há usuários")
        void deveRetornarListaVazia() {
            when(usuarioRepository.findAll()).thenReturn(List.of());

            List<Usuario> resultado = usuarioService.listarUsuarios();

            assertTrue(resultado.isEmpty());
            verify(usuarioRepository, times(1)).findAll();
        }
    }
}
