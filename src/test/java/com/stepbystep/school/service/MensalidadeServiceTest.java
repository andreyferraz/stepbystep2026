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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stepbystep.school.enums.StatusMensalidade;
import com.stepbystep.school.model.Aluno;
import com.stepbystep.school.model.Mensalidade;
import com.stepbystep.school.repository.MensalidadeRepository;

@ExtendWith(MockitoExtension.class)
class MensalidadeServiceTest {

    @Mock
    private AlunoService alunoService;

    @Mock
    private MensalidadeRepository mensalidadeRepository;

    @InjectMocks
    private MensalidadeService mensalidadeService;

    private UUID alunoId;
    private UUID mensalidadeId;
    private Mensalidade mensalidade;

    @BeforeEach
    void setUp() {
        alunoId = UUID.randomUUID();
        mensalidadeId = UUID.randomUUID();
        mensalidade = Mensalidade.builder()
                .id(mensalidadeId)
                .valor(new BigDecimal("350.00"))
                .dataVencimento(LocalDate.now().plusDays(10))
                .status(StatusMensalidade.PENDENTE)
                .aluno(Aluno.builder().id(alunoId).nome("Aluno Teste").build())
                .build();
    }

    @Nested
    @DisplayName("listarMensalidadesPorAluno")
    class ListarMensalidadesPorAluno {

        @Test
        @DisplayName("Deve listar mensalidades do aluno")
        void deveListarMensalidadesDoAluno() {
            when(mensalidadeRepository.findByAlunoIdOrderByDataVencimentoAsc(alunoId)).thenReturn(List.of(mensalidade));
            when(alunoService.obterAlunoPorId(alunoId)).thenReturn(Aluno.builder().id(alunoId).build());

            List<Mensalidade> retorno = mensalidadeService.listarMensalidadesPorAluno(alunoId);

            assertEquals(1, retorno.size());
            assertEquals(mensalidadeId, retorno.get(0).getId());
            verify(mensalidadeRepository, times(1)).findByAlunoIdOrderByDataVencimentoAsc(alunoId);
        }

        @Test
        @DisplayName("Deve lançar exceção quando ID do aluno é nulo")
        void deveLancarExcecaoQuandoIdAlunoNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> mensalidadeService.listarMensalidadesPorAluno(null));

            assertTrue(ex.getMessage().contains("ID do aluno"));
            verify(mensalidadeRepository, never()).findByAlunoIdOrderByDataVencimentoAsc(any());
        }
    }

    @Nested
    @DisplayName("confirmarPagamento")
    class ConfirmarPagamento {

        @Test
        @DisplayName("Deve confirmar pagamento da mensalidade")
        void deveConfirmarPagamentoDaMensalidade() {
            when(alunoService.obterAlunoPorId(alunoId)).thenReturn(Aluno.builder().id(alunoId).build());
            when(mensalidadeRepository.findByIdAndAlunoId(mensalidadeId, alunoId)).thenReturn(Optional.of(mensalidade));
            when(mensalidadeRepository.save(any(Mensalidade.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Mensalidade retorno = mensalidadeService.confirmarPagamento(alunoId, mensalidadeId);

            assertEquals(StatusMensalidade.PAGO, retorno.getStatus());
            assertNotNull(retorno.getDataPagamento());
            verify(mensalidadeRepository, times(1)).save(mensalidade);
        }

        @Test
        @DisplayName("Deve lançar exceção quando mensalidade não encontrada")
        void deveLancarExcecaoQuandoMensalidadeNaoEncontrada() {
            when(alunoService.obterAlunoPorId(alunoId)).thenReturn(Aluno.builder().id(alunoId).build());
            when(mensalidadeRepository.findByIdAndAlunoId(mensalidadeId, alunoId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> mensalidadeService.confirmarPagamento(alunoId, mensalidadeId));

            assertTrue(ex.getMessage().contains("Mensalidade não encontrada"));
            verify(mensalidadeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando ID do aluno é nulo")
        void deveLancarExcecaoQuandoIdAlunoNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> mensalidadeService.confirmarPagamento(null, mensalidadeId));

            assertTrue(ex.getMessage().contains("ID do aluno"));
            verify(mensalidadeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("excluirMensalidade")
    class ExcluirMensalidade {

        @Test
        @DisplayName("Deve excluir cobrança pendente")
        void deveExcluirCobrancaPendente() {
            when(alunoService.obterAlunoPorId(alunoId)).thenReturn(Aluno.builder().id(alunoId).build());
            when(mensalidadeRepository.findByIdAndAlunoId(mensalidadeId, alunoId)).thenReturn(Optional.of(mensalidade));

            mensalidadeService.excluirMensalidade(alunoId, mensalidadeId);

            verify(mensalidadeRepository, times(1)).delete(mensalidade);
        }

        @Test
        @DisplayName("Deve bloquear exclusão quando mensalidade já está paga")
        void deveBloquearExclusaoQuandoMensalidadePaga() {
            mensalidade.setStatus(StatusMensalidade.PAGO);
            when(alunoService.obterAlunoPorId(alunoId)).thenReturn(Aluno.builder().id(alunoId).build());
            when(mensalidadeRepository.findByIdAndAlunoId(mensalidadeId, alunoId)).thenReturn(Optional.of(mensalidade));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> mensalidadeService.excluirMensalidade(alunoId, mensalidadeId));

            assertTrue(ex.getMessage().contains("já paga"));
            verify(mensalidadeRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando mensalidade não encontrada")
        void deveLancarExcecaoQuandoMensalidadeNaoEncontrada() {
            when(alunoService.obterAlunoPorId(alunoId)).thenReturn(Aluno.builder().id(alunoId).build());
            when(mensalidadeRepository.findByIdAndAlunoId(mensalidadeId, alunoId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> mensalidadeService.excluirMensalidade(alunoId, mensalidadeId));

            assertTrue(ex.getMessage().contains("Mensalidade não encontrada"));
            verify(mensalidadeRepository, never()).delete(any());
        }
    }

}
