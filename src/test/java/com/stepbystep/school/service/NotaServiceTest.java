package com.stepbystep.school.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stepbystep.school.model.Aluno;
import com.stepbystep.school.model.Nota;
import com.stepbystep.school.repository.NotaRepository;

@ExtendWith(MockitoExtension.class)
class NotaServiceTest {

    @Mock
    private AlunoService alunoService;

    @Mock
    private NotaRepository notaRepository;

    @InjectMocks
    private NotaService notaService;

    private UUID alunoId;
    private UUID notaId;
    private Aluno aluno;
    private Nota notaValida;

    @BeforeEach
    void setUp() {
        alunoId = UUID.randomUUID();
        notaId = UUID.randomUUID();
        aluno = Aluno.builder().id(alunoId).nome("Aluno Teste").build();
        notaValida = Nota.builder()
            .valor(8.5)
            .bimestre(2)
            .atividade("Prova 1")
            .dataReferencia(LocalDate.now())
            .descricao("Prova 1")
            .build();
    }

    @Nested
    @DisplayName("cadastrarNota")
    class CadastrarNota {

        @Test
        @DisplayName("Deve cadastrar nota associada ao aluno")
        void deveCadastrarNotaAssociadaAoAluno() {
            when(alunoService.obterAlunoPorId(alunoId)).thenReturn(aluno);
            when(notaRepository.save(any(Nota.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Nota retorno = notaService.cadastrarNota(alunoId, notaValida);

            assertNotNull(retorno);
            assertEquals(alunoId, retorno.getAluno().getId());
            assertEquals(8.5, retorno.getValor());
            verify(notaRepository, times(1)).save(notaValida);
        }

        @Test
        @DisplayName("Deve lançar exceção quando id do aluno é nulo")
        void deveLancarExcecaoQuandoIdAlunoNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> notaService.cadastrarNota(null, notaValida));

            assertTrue(ex.getMessage().contains("ID do aluno"));
            verify(notaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando nota é nula")
        void deveLancarExcecaoQuandoNotaNula() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> notaService.cadastrarNota(alunoId, null));

            assertTrue(ex.getMessage().contains("Nota"));
            verify(notaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando nota e presença não forem informadas")
        void deveLancarExcecaoQuandoNotaEPresencaNaoForemInformadas() {
            notaValida.setValor(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> notaService.cadastrarNota(alunoId, notaValida));

            assertTrue(ex.getMessage().contains("Informe ao menos a nota ou a presença"));
            verify(notaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando bimestre é inválido")
        void deveLancarExcecaoQuandoBimestreInvalido() {
            notaValida.setBimestre(5);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> notaService.cadastrarNota(alunoId, notaValida));

            assertTrue(ex.getMessage().contains("Bimestre"));
            verify(notaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("editarNota")
    class EditarNota {

        @Test
        @DisplayName("Deve editar nota existente sem alterar aluno")
        void deveEditarNotaExistenteSemAlterarAluno() {
            Nota notaExistente = Nota.builder()
                    .id(notaId)
                    .valor(6.0)
                    .bimestre(1)
                    .atividade("Atividade antiga")
                    .dataReferencia(LocalDate.now().minusDays(2))
                    .descricao("Antiga")
                    .aluno(aluno)
                    .build();
            Nota notaAtualizada = Nota.builder()
                    .valor(9.0)
                    .bimestre(3)
                    .atividade("Atividade atualizada")
                    .dataReferencia(LocalDate.now())
                    .descricao("Atualizada")
                    .aluno(Aluno.builder().id(UUID.randomUUID()).build())
                    .build();

            when(notaRepository.findById(notaId)).thenReturn(Optional.of(notaExistente));
            when(notaRepository.save(any(Nota.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Nota retorno = notaService.editarNota(notaId, notaAtualizada);

            assertEquals(9.0, retorno.getValor());
            assertEquals(3, retorno.getBimestre());
            assertEquals("Atualizada", retorno.getDescricao());
            assertEquals(aluno.getId(), retorno.getAluno().getId());
            verify(notaRepository, times(1)).save(notaExistente);
        }

        @Test
        @DisplayName("Deve lançar exceção quando id da nota é nulo")
        void deveLancarExcecaoQuandoIdNotaNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> notaService.editarNota(null, notaValida));

            assertTrue(ex.getMessage().contains("ID da nota"));
            verify(notaRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando nota de entrada é nula")
        void deveLancarExcecaoQuandoNotaEntradaNula() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> notaService.editarNota(notaId, null));

            assertTrue(ex.getMessage().contains("Nota"));
            verify(notaRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando nota e presença não forem informadas")
        void deveLancarExcecaoQuandoNotaEPresencaNaoForemInformadas() {
            notaValida.setValor(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> notaService.editarNota(notaId, notaValida));

            assertTrue(ex.getMessage().contains("Informe ao menos a nota ou a presença"));
            verify(notaRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando bimestre inválido")
        void deveLancarExcecaoQuandoBimestreInvalido() {
            notaValida.setBimestre(0);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> notaService.editarNota(notaId, notaValida));

            assertTrue(ex.getMessage().contains("Bimestre"));
            verify(notaRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando nota não encontrada")
        void deveLancarExcecaoQuandoNotaNaoEncontrada() {
            when(notaRepository.findById(notaId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> notaService.editarNota(notaId, notaValida));

            assertTrue(ex.getMessage().contains("Nota não encontrada com ID"));
            verify(notaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("excluirNota")
    class ExcluirNota {

        @Test
        @DisplayName("Deve excluir nota quando ela existe")
        void deveExcluirNotaQuandoElaExiste() {
            when(notaRepository.existsById(notaId)).thenReturn(true);

            notaService.excluirNota(notaId);

            verify(notaRepository, times(1)).existsById(notaId);
            verify(notaRepository, times(1)).deleteById(notaId);
        }

        @Test
        @DisplayName("Deve lançar exceção quando id da nota é nulo")
        void deveLancarExcecaoQuandoIdDaNotaNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> notaService.excluirNota(null));

            assertTrue(ex.getMessage().contains("ID da nota"));
            verify(notaRepository, never()).existsById(any());
            verify(notaRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando nota não existe")
        void deveLancarExcecaoQuandoNotaNaoExiste() {
            when(notaRepository.existsById(notaId)).thenReturn(false);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> notaService.excluirNota(notaId));

            assertTrue(ex.getMessage().contains("Nota não encontrada com ID"));
            verify(notaRepository, times(1)).existsById(notaId);
            verify(notaRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("listarNotasPorAluno")
    class ListarNotasPorAluno {

        @Test
        @DisplayName("Deve listar notas de um aluno específico")
        void deveListarNotasDeUmAlunoEspecifico() {
            Nota nota = Nota.builder().id(notaId).aluno(aluno).build();
            when(notaRepository.findByAlunoIdOrderByDataReferenciaDescIdDesc(alunoId)).thenReturn(List.of(nota));

            List<Nota> retorno = notaService.listarNotasPorAluno(alunoId);

            assertEquals(1, retorno.size());
            assertEquals(notaId, retorno.get(0).getId());
            verify(notaRepository, times(1)).findByAlunoIdOrderByDataReferenciaDescIdDesc(alunoId);
        }

        @Test
        @DisplayName("Deve lançar exceção quando ID do aluno for nulo")
        void deveLancarExcecaoQuandoIdDoAlunoForNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> notaService.listarNotasPorAluno(null));

            assertTrue(ex.getMessage().contains("ID do aluno"));
            verify(notaRepository, never()).findByAlunoIdOrderByDataReferenciaDescIdDesc(any());
        }
    }
}
