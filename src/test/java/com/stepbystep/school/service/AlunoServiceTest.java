package com.stepbystep.school.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stepbystep.school.enums.NivelAtual;
import com.stepbystep.school.model.Aluno;
import com.stepbystep.school.model.Mensalidade;
import com.stepbystep.school.model.Nota;
import com.stepbystep.school.model.Turma;
import com.stepbystep.school.repository.AlunoRepository;

@ExtendWith(MockitoExtension.class)
class AlunoServiceTest {

    @Mock
    private AlunoRepository alunoRepository;

    @InjectMocks
    private AlunoService alunoService;

    private Aluno alunoValido;
    private Turma turma;
    private UUID alunoId;

    @BeforeEach
    void setUp() {
        alunoId = UUID.randomUUID();
        turma = Turma.builder().id(UUID.randomUUID()).nome("Turma A").build();

        alunoValido = Aluno.builder()
                .id(alunoId)
                .nome("João Silva")
                .dataNascimento(LocalDate.of(2010, 5, 15))
                .telefone("11999999999")
                .nivelAtual(NivelAtual.INICIANTE)
                .turma(turma)
                .notas(List.of(Nota.builder().valor(8.0).bimestre(1).build()))
                .mensalidades(List.of(Mensalidade.builder().build()))
                .build();
    }

    // ==================== cadastrarAluno ====================

    @Nested
    @DisplayName("cadastrarAluno")
    class CadastrarAluno {

        @Test
        @DisplayName("Deve cadastrar aluno com dados válidos")
        void deveCadastrarAlunoComSucesso() {
            when(alunoRepository.save(any(Aluno.class))).thenReturn(alunoValido);

            assertDoesNotThrow(() -> alunoService.cadastrarAluno(alunoValido));
            verify(alunoRepository, times(1)).save(alunoValido);
        }

        @Test
        @DisplayName("Deve lançar exceção quando aluno é nulo")
        void deveLancarExcecaoQuandoAlunoNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.cadastrarAluno(null));
            assertTrue(ex.getMessage().contains("Aluno"));
            verify(alunoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando nome é nulo")
        void deveLancarExcecaoQuandoNomeNulo() {
            alunoValido.setNome(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.cadastrarAluno(alunoValido));
            assertTrue(ex.getMessage().contains("Nome do Aluno"));
            verify(alunoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando nome é vazio")
        void deveLancarExcecaoQuandoNomeVazio() {
            alunoValido.setNome("   ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.cadastrarAluno(alunoValido));
            assertTrue(ex.getMessage().contains("Nome do Aluno"));
            verify(alunoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando data de nascimento é nula")
        void deveLancarExcecaoQuandoDataNascimentoNula() {
            alunoValido.setDataNascimento(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.cadastrarAluno(alunoValido));
            assertTrue(ex.getMessage().contains("Data de Nascimento"));
            verify(alunoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando telefone é nulo")
        void deveLancarExcecaoQuandoTelefoneNulo() {
            alunoValido.setTelefone(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.cadastrarAluno(alunoValido));
            assertTrue(ex.getMessage().contains("Telefone"));
            verify(alunoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando telefone é vazio")
        void deveLancarExcecaoQuandoTelefoneVazio() {
            alunoValido.setTelefone("");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.cadastrarAluno(alunoValido));
            assertTrue(ex.getMessage().contains("Telefone"));
            verify(alunoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando nível atual é nulo")
        void deveLancarExcecaoQuandoNivelAtualNulo() {
            alunoValido.setNivelAtual(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.cadastrarAluno(alunoValido));
            assertTrue(ex.getMessage().contains("Nível Atual"));
            verify(alunoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando turma é nula")
        void deveLancarExcecaoQuandoTurmaNula() {
            alunoValido.setTurma(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.cadastrarAluno(alunoValido));
            assertTrue(ex.getMessage().contains("Turma"));
            verify(alunoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando mensalidades é nula")
        void deveLancarExcecaoQuandoMensalidadesNula() {
            alunoValido.setMensalidades(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.cadastrarAluno(alunoValido));
            assertTrue(ex.getMessage().contains("Mensalidades"));
            verify(alunoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando mensalidades é lista vazia")
        void deveLancarExcecaoQuandoMensalidadesVazia() {
            alunoValido.setMensalidades(Collections.emptyList());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.cadastrarAluno(alunoValido));
            assertTrue(ex.getMessage().contains("Mensalidades"));
            verify(alunoRepository, never()).save(any());
        }
    }

    // ==================== listarAlunos ====================

    @Nested
    @DisplayName("listarAlunos")
    class ListarAlunos {

        @Test
        @DisplayName("Deve retornar lista de alunos")
        void deveRetornarListaDeAlunos() {
            List<Aluno> alunos = List.of(alunoValido);
            when(alunoRepository.findAll()).thenReturn(alunos);

            List<Aluno> resultado = alunoService.listarAlunos();

            assertEquals(1, resultado.size());
            assertEquals(alunoValido, resultado.get(0));
            verify(alunoRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há alunos")
        void deveRetornarListaVazia() {
            when(alunoRepository.findAll()).thenReturn(Collections.emptyList());

            List<Aluno> resultado = alunoService.listarAlunos();

            assertTrue(resultado.isEmpty());
            verify(alunoRepository, times(1)).findAll();
        }
    }

    // ==================== obterAlunoPorId ====================

    @Nested
    @DisplayName("obterAlunoPorId")
    class ObterAlunoPorId {

        @Test
        @DisplayName("Deve retornar aluno quando encontrado")
        void deveRetornarAlunoQuandoEncontrado() {
            when(alunoRepository.findById(alunoId)).thenReturn(Optional.of(alunoValido));

            Aluno resultado = alunoService.obterAlunoPorId(alunoId);

            assertNotNull(resultado);
            assertEquals(alunoValido.getNome(), resultado.getNome());
            verify(alunoRepository, times(1)).findById(alunoId);
        }

        @Test
        @DisplayName("Deve lançar exceção quando aluno não encontrado")
        void deveLancarExcecaoQuandoAlunoNaoEncontrado() {
            UUID idInexistente = UUID.randomUUID();
            when(alunoRepository.findById(idInexistente)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.obterAlunoPorId(idInexistente));
            assertTrue(ex.getMessage().contains("Aluno não encontrado"));
            assertTrue(ex.getMessage().contains(idInexistente.toString()));
        }
    }

    // ==================== atualizarAluno ====================

    @Nested
    @DisplayName("atualizarAluno")
    class AtualizarAluno {

        @Test
        @DisplayName("Deve atualizar aluno com dados válidos")
        void deveAtualizarAlunoComSucesso() {
            Aluno alunoAtualizado = Aluno.builder()
                    .nome("João Atualizado")
                    .dataNascimento(LocalDate.of(2010, 6, 20))
                    .telefone("11888888888")
                    .nivelAtual(NivelAtual.INTERMEDIARIO)
                    .turma(turma)
                    .notas(List.of(Nota.builder().valor(9.0).bimestre(2).build()))
                    .mensalidades(List.of(Mensalidade.builder().build()))
                    .build();

            when(alunoRepository.findById(alunoId)).thenReturn(Optional.of(alunoValido));
            when(alunoRepository.save(any(Aluno.class))).thenReturn(alunoValido);

            assertDoesNotThrow(() -> alunoService.atualizarAluno(alunoId, alunoAtualizado));

            assertEquals("João Atualizado", alunoValido.getNome());
            assertEquals(NivelAtual.INTERMEDIARIO, alunoValido.getNivelAtual());
            assertEquals("11888888888", alunoValido.getTelefone());
            verify(alunoRepository, times(1)).save(alunoValido);
        }

        @Test
        @DisplayName("Deve lançar exceção quando aluno atualizado é nulo")
        void deveLancarExcecaoQuandoAlunoAtualizadoNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.atualizarAluno(alunoId, null));
            assertTrue(ex.getMessage().contains("Aluno"));
            verify(alunoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando nome do aluno atualizado é nulo")
        void deveLancarExcecaoQuandoNomeAtualizadoNulo() {
            alunoValido.setNome(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.atualizarAluno(alunoId, alunoValido));
            assertTrue(ex.getMessage().contains("Nome do Aluno"));
            verify(alunoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando data de nascimento do atualizado é nula")
        void deveLancarExcecaoQuandoDataNascimentoAtualizadaNula() {
            alunoValido.setDataNascimento(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.atualizarAluno(alunoId, alunoValido));
            assertTrue(ex.getMessage().contains("Data de Nascimento"));
            verify(alunoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando telefone do atualizado é vazio")
        void deveLancarExcecaoQuandoTelefoneAtualizadoVazio() {
            alunoValido.setTelefone("  ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.atualizarAluno(alunoId, alunoValido));
            assertTrue(ex.getMessage().contains("Telefone"));
            verify(alunoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando nível atual do atualizado é nulo")
        void deveLancarExcecaoQuandoNivelAtualAtualizadoNulo() {
            alunoValido.setNivelAtual(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.atualizarAluno(alunoId, alunoValido));
            assertTrue(ex.getMessage().contains("Nível Atual"));
            verify(alunoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando turma do atualizado é nula")
        void deveLancarExcecaoQuandoTurmaAtualizadaNula() {
            alunoValido.setTurma(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.atualizarAluno(alunoId, alunoValido));
            assertTrue(ex.getMessage().contains("Turma"));
            verify(alunoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando mensalidades do atualizado é vazia")
        void deveLancarExcecaoQuandoMensalidadesAtualizadaVazia() {
            alunoValido.setMensalidades(Collections.emptyList());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.atualizarAluno(alunoId, alunoValido));
            assertTrue(ex.getMessage().contains("Mensalidades"));
            verify(alunoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando notas do atualizado é nula")
        void deveLancarExcecaoQuandoNotasAtualizadaNula() {
            alunoValido.setNotas(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.atualizarAluno(alunoId, alunoValido));
            assertTrue(ex.getMessage().contains("Notas"));
            verify(alunoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando ID do aluno não existe no banco")
        void deveLancarExcecaoQuandoIdNaoExiste() {
            UUID idInexistente = UUID.randomUUID();
            Aluno alunoAtualizado = Aluno.builder()
                    .nome("Teste")
                    .dataNascimento(LocalDate.of(2010, 1, 1))
                    .telefone("11999999999")
                    .nivelAtual(NivelAtual.INICIANTE)
                    .turma(turma)
                    .notas(List.of(Nota.builder().valor(7.0).bimestre(1).build()))
                    .mensalidades(List.of(Mensalidade.builder().build()))
                    .build();

            when(alunoRepository.findById(idInexistente)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.atualizarAluno(idInexistente, alunoAtualizado));
            assertTrue(ex.getMessage().contains("Aluno não encontrado"));
            verify(alunoRepository, never()).save(any());
        }
    }

    // ==================== excluirAluno ====================

    @Nested
    @DisplayName("excluirAluno")
    class ExcluirAluno {

        @Test
        @DisplayName("Deve excluir aluno com sucesso")
        void deveExcluirAlunoComSucesso() {
            when(alunoRepository.findById(alunoId)).thenReturn(Optional.of(alunoValido));
            doNothing().when(alunoRepository).delete(alunoValido);

            assertDoesNotThrow(() -> alunoService.excluirAluno(alunoId));
            verify(alunoRepository, times(1)).delete(alunoValido);
        }

        @Test
        @DisplayName("Deve lançar exceção quando aluno a excluir não existe")
        void deveLancarExcecaoQuandoAlunoNaoExiste() {
            UUID idInexistente = UUID.randomUUID();
            when(alunoRepository.findById(idInexistente)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> alunoService.excluirAluno(idInexistente));
            assertTrue(ex.getMessage().contains("Aluno não encontrado"));
            verify(alunoRepository, never()).delete(any());
        }
    }
}
