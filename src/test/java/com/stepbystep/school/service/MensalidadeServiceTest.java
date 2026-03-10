package com.stepbystep.school.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
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

        setPrivateField(mensalidadeService, "pixChave", "12345678900");
        setPrivateField(mensalidadeService, "pixRecebedor", "STEP BY STEP SCHOOL");
        setPrivateField(mensalidadeService, "pixCidade", "SAO PAULO");
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
    @DisplayName("gerarPix")
    class GerarPix {

        @Test
        @DisplayName("Deve gerar payload PIX quando mensalidade está pendente")
        void deveGerarPixQuandoMensalidadePendente() {
            when(alunoService.obterAlunoPorId(alunoId)).thenReturn(Aluno.builder().id(alunoId).build());
            when(mensalidadeRepository.findByIdAndAlunoId(mensalidadeId, alunoId)).thenReturn(Optional.of(mensalidade));
            when(mensalidadeRepository.save(any(Mensalidade.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Mensalidade retorno = mensalidadeService.gerarPix(alunoId, mensalidadeId);

            assertNotNull(retorno.getPixCopiaECola());
            assertTrue(retorno.getPixCopiaECola().startsWith("000201"));
            verify(mensalidadeRepository, times(1)).save(mensalidade);
        }

        @Test
        @DisplayName("Não deve regenerar PIX quando já existe payload")
        void naoDeveRegenerarPixQuandoJaExistePayload() {
            mensalidade.setPixCopiaECola("000201260014br.gov.bcb.pix");
            when(alunoService.obterAlunoPorId(alunoId)).thenReturn(Aluno.builder().id(alunoId).build());
            when(mensalidadeRepository.findByIdAndAlunoId(mensalidadeId, alunoId)).thenReturn(Optional.of(mensalidade));

            Mensalidade retorno = mensalidadeService.gerarPix(alunoId, mensalidadeId);

            assertEquals("000201260014br.gov.bcb.pix", retorno.getPixCopiaECola());
            verify(mensalidadeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando mensalidade está paga")
        void deveLancarExcecaoQuandoMensalidadePaga() {
            mensalidade.setStatus(StatusMensalidade.PAGO);
            when(alunoService.obterAlunoPorId(alunoId)).thenReturn(Aluno.builder().id(alunoId).build());
            when(mensalidadeRepository.findByIdAndAlunoId(mensalidadeId, alunoId)).thenReturn(Optional.of(mensalidade));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> mensalidadeService.gerarPix(alunoId, mensalidadeId));

            assertTrue(ex.getMessage().contains("já paga"));
            verify(mensalidadeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando mensalidade não encontrada")
        void deveLancarExcecaoQuandoMensalidadeNaoEncontrada() {
            when(alunoService.obterAlunoPorId(alunoId)).thenReturn(Aluno.builder().id(alunoId).build());
            when(mensalidadeRepository.findByIdAndAlunoId(mensalidadeId, alunoId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> mensalidadeService.gerarPix(alunoId, mensalidadeId));

            assertTrue(ex.getMessage().contains("Mensalidade não encontrada"));
        }

        @Test
        @DisplayName("Deve lançar exceção quando valor da mensalidade é inválido")
        void deveLancarExcecaoQuandoValorMensalidadeInvalido() {
            mensalidade.setValor(BigDecimal.ZERO);
            when(alunoService.obterAlunoPorId(alunoId)).thenReturn(Aluno.builder().id(alunoId).build());
            when(mensalidadeRepository.findByIdAndAlunoId(mensalidadeId, alunoId)).thenReturn(Optional.of(mensalidade));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> mensalidadeService.gerarPix(alunoId, mensalidadeId));

            assertTrue(ex.getMessage().contains("maior que zero"));
        }
    }

    @Nested
    @DisplayName("gerarQrCodeBase64")
    class GerarQrCodeBase64 {

        @Test
        @DisplayName("Deve gerar QR Code em Base64")
        void deveGerarQrCodeEmBase64() {
            String base64 = mensalidadeService.gerarQrCodeBase64("000201010212");

            assertNotNull(base64);
            assertFalse(base64.isBlank());
        }

        @Test
        @DisplayName("Deve lançar exceção quando payload PIX é inválido")
        void deveLancarExcecaoQuandoPayloadPixInvalido() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> mensalidadeService.gerarQrCodeBase64("  "));

            assertTrue(ex.getMessage().contains("Payload PIX"));
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

    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Não foi possível configurar campo privado para teste", e);
        }
    }
}
