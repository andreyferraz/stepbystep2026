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

import com.stepbystep.school.model.Turma;
import com.stepbystep.school.repository.TurmaRepository;

@ExtendWith(MockitoExtension.class)
class TurmaServiceTest {

    @Mock
    private TurmaRepository turmaRepository;

    @InjectMocks
    private TurmaService turmaService;

    private UUID turmaId;
    private Turma turmaValida;

    @BeforeEach
    void setUp() {
        turmaId = UUID.randomUUID();
        turmaValida = Turma.builder()
                .id(turmaId)
                .nome("Turma Kids")
                .horario("14:00")
                .diasSemana("Segunda e Quarta")
                .build();
    }

    @Nested
    @DisplayName("criarTurma")
    class CriarTurma {

        @Test
        @DisplayName("Deve criar turma com sucesso e normalizar campos")
        void deveCriarTurmaComSucessoENormalizarCampos() {
            Turma novaTurma = Turma.builder()
                    .id(UUID.randomUUID())
                    .nome("  Turma Teens  ")
                    .horario("  19:00  ")
                    .diasSemana("  Terca e Quinta  ")
                    .build();

            when(turmaRepository.save(any(Turma.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Turma resultado = turmaService.criarTurma(novaTurma);

            ArgumentCaptor<Turma> captor = ArgumentCaptor.forClass(Turma.class);
            verify(turmaRepository, times(1)).save(captor.capture());

            Turma enviadoParaSalvar = captor.getValue();
            assertNull(enviadoParaSalvar.getId());
            assertEquals("Turma Teens", enviadoParaSalvar.getNome());
            assertEquals("19:00", enviadoParaSalvar.getHorario());
            assertEquals("Terca e Quinta", enviadoParaSalvar.getDiasSemana());

            assertNotNull(resultado);
            assertNull(resultado.getId());
        }

        @Test
        @DisplayName("Deve lançar exceção quando turma é nula")
        void deveLancarExcecaoQuandoTurmaNula() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> turmaService.criarTurma(null));

            assertTrue(ex.getMessage().contains("Turma"));
            verify(turmaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando nome é nulo")
        void deveLancarExcecaoQuandoNomeNulo() {
            turmaValida.setNome(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> turmaService.criarTurma(turmaValida));

            assertTrue(ex.getMessage().contains("Nome da Turma"));
            verify(turmaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando horário é vazio")
        void deveLancarExcecaoQuandoHorarioVazio() {
            turmaValida.setHorario("   ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> turmaService.criarTurma(turmaValida));

            assertTrue(ex.getMessage().contains("Horário da Turma"));
            verify(turmaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando dias da semana é nulo")
        void deveLancarExcecaoQuandoDiasSemanaNulo() {
            turmaValida.setDiasSemana(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> turmaService.criarTurma(turmaValida));

            assertTrue(ex.getMessage().contains("Dias da Semana da Turma"));
            verify(turmaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("editarTurma")
    class EditarTurma {

        @Test
        @DisplayName("Deve editar turma existente com sucesso")
        void deveEditarTurmaExistenteComSucesso() {
            Turma turmaExistente = Turma.builder()
                    .id(turmaId)
                    .nome("Turma Antiga")
                    .horario("08:00")
                    .diasSemana("Segunda")
                    .build();

            Turma dadosAtualizados = Turma.builder()
                    .id(UUID.randomUUID())
                    .nome("  Turma Atualizada  ")
                    .horario("  10:00  ")
                    .diasSemana("  Terca e Quinta  ")
                    .build();

            when(turmaRepository.findById(turmaId)).thenReturn(Optional.of(turmaExistente));
            when(turmaRepository.save(any(Turma.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Turma resultado = turmaService.editarTurma(turmaId, dadosAtualizados);

            assertNotNull(resultado);
            assertEquals(turmaId, turmaExistente.getId());
            assertEquals("Turma Atualizada", turmaExistente.getNome());
            assertEquals("10:00", turmaExistente.getHorario());
            assertEquals("Terca e Quinta", turmaExistente.getDiasSemana());

            verify(turmaRepository, times(1)).findById(turmaId);
            verify(turmaRepository, times(1)).save(turmaExistente);
        }

        @Test
        @DisplayName("Deve lançar exceção quando ID é nulo")
        void deveLancarExcecaoQuandoIdNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> turmaService.editarTurma(null, turmaValida));

            assertTrue(ex.getMessage().contains("ID da Turma"));
            verify(turmaRepository, never()).findById(any());
            verify(turmaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando turma atualizada é nula")
        void deveLancarExcecaoQuandoTurmaAtualizadaNula() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> turmaService.editarTurma(turmaId, null));

            assertTrue(ex.getMessage().contains("Turma"));
            verify(turmaRepository, never()).findById(any());
            verify(turmaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando turma não existe")
        void deveLancarExcecaoQuandoTurmaNaoExiste() {
            when(turmaRepository.findById(turmaId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> turmaService.editarTurma(turmaId, turmaValida));

            assertTrue(ex.getMessage().contains("Turma não encontrada"));
            verify(turmaRepository, times(1)).findById(turmaId);
            verify(turmaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando nome é vazio")
        void deveLancarExcecaoQuandoNomeVazio() {
            turmaValida.setNome("   ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> turmaService.editarTurma(turmaId, turmaValida));

            assertTrue(ex.getMessage().contains("Nome da Turma"));
            verify(turmaRepository, never()).findById(any());
            verify(turmaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listarTurmas")
    class ListarTurmas {

        @Test
        @DisplayName("Deve retornar lista de turmas")
        void deveRetornarListaDeTurmas() {
            when(turmaRepository.findAll()).thenReturn(List.of(turmaValida));

            List<Turma> resultado = turmaService.listarTurmas();

            assertEquals(1, resultado.size());
            assertEquals(turmaValida, resultado.get(0));
            verify(turmaRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há turmas")
        void deveRetornarListaVaziaQuandoNaoHaTurmas() {
            when(turmaRepository.findAll()).thenReturn(List.of());

            List<Turma> resultado = turmaService.listarTurmas();

            assertTrue(resultado.isEmpty());
            verify(turmaRepository, times(1)).findAll();
        }
    }

    @Nested
    @DisplayName("excluirTurma")
    class ExcluirTurma {

        @Test
        @DisplayName("Deve excluir turma com sucesso")
        void deveExcluirTurmaComSucesso() {
            when(turmaRepository.findById(turmaId)).thenReturn(Optional.of(turmaValida));

            assertDoesNotThrow(() -> turmaService.excluirTurma(turmaId));

            verify(turmaRepository, times(1)).findById(turmaId);
            verify(turmaRepository, times(1)).delete(turmaValida);
        }

        @Test
        @DisplayName("Deve lançar exceção quando ID é nulo")
        void deveLancarExcecaoQuandoIdNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> turmaService.excluirTurma(null));

            assertTrue(ex.getMessage().contains("ID da Turma"));
            verify(turmaRepository, never()).findById(any());
            verify(turmaRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando turma não encontrada")
        void deveLancarExcecaoQuandoTurmaNaoEncontrada() {
            when(turmaRepository.findById(turmaId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> turmaService.excluirTurma(turmaId));

            assertTrue(ex.getMessage().contains("Turma não encontrada"));
            verify(turmaRepository, times(1)).findById(turmaId);
            verify(turmaRepository, never()).delete(any());
        }
    }
}
