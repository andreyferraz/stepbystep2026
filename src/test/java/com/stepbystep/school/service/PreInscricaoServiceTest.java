package com.stepbystep.school.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

import java.time.LocalDateTime;
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

import com.stepbystep.school.model.PreInscricao;
import com.stepbystep.school.repository.PreInscricaoRepository;

@ExtendWith(MockitoExtension.class)
class PreInscricaoServiceTest {

    @Mock
    private PreInscricaoRepository preInscricaoRepository;

    @InjectMocks
    private PreInscricaoService preInscricaoService;

    private UUID leadId;
    private PreInscricao lead;

    @BeforeEach
    void setUp() {
        leadId = UUID.randomUUID();
        lead = PreInscricao.builder()
                .id(leadId)
                .nomeInteressado("Ana Souza")
                .whatsapp("11999999999")
                .mensagem("Tenho interesse nas turmas de conversacao")
                .dataLead(LocalDateTime.now().minusHours(1))
                .respondido(false)
                .build();
    }

    @Nested
    @DisplayName("criarPreInscricao")
    class CriarPreInscricao {

        @Test
        @DisplayName("Deve criar pre-inscricao com dados validos")
        void deveCriarPreInscricaoComDadosValidos() {
            when(preInscricaoRepository.save(any(PreInscricao.class))).thenAnswer(invocation -> {
                PreInscricao salvo = invocation.getArgument(0);
                salvo.setId(leadId);
                return salvo;
            });

            PreInscricao resultado = preInscricaoService.criarPreInscricao(
                    "  Ana Souza  ",
                    " 11999999999 ",
                    "  Tenho interesse nas turmas de conversacao  ");

            ArgumentCaptor<PreInscricao> captor = ArgumentCaptor.forClass(PreInscricao.class);
            verify(preInscricaoRepository, times(1)).save(captor.capture());

            PreInscricao enviadoParaSalvar = captor.getValue();
            assertEquals("Ana Souza", enviadoParaSalvar.getNomeInteressado());
            assertEquals("11999999999", enviadoParaSalvar.getWhatsapp());
            assertEquals("Tenho interesse nas turmas de conversacao", enviadoParaSalvar.getMensagem());
            assertFalse(enviadoParaSalvar.isRespondido());
            assertNotNull(enviadoParaSalvar.getDataLead());

            assertNotNull(resultado);
            assertEquals(leadId, resultado.getId());
        }

        @Test
        @DisplayName("Deve aceitar mensagem nula")
        void deveAceitarMensagemNula() {
            when(preInscricaoRepository.save(any(PreInscricao.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PreInscricao resultado = preInscricaoService.criarPreInscricao("Ana", "11999999999", null);

            assertNotNull(resultado);
            assertEquals("Ana", resultado.getNomeInteressado());
            assertEquals("11999999999", resultado.getWhatsapp());
            assertEquals(null, resultado.getMensagem());
            verify(preInscricaoRepository, times(1)).save(any(PreInscricao.class));
        }

        @Test
        @DisplayName("Deve lancar excecao quando nome e nulo")
        void deveLancarExcecaoQuandoNomeNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> preInscricaoService.criarPreInscricao(null, "11999999999", "msg"));

            assertTrue(ex.getMessage().contains("Nome"));
            verify(preInscricaoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lancar excecao quando nome e vazio")
        void deveLancarExcecaoQuandoNomeVazio() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> preInscricaoService.criarPreInscricao("   ", "11999999999", "msg"));

            assertTrue(ex.getMessage().contains("Nome"));
            verify(preInscricaoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lancar excecao quando whatsapp e nulo")
        void deveLancarExcecaoQuandoWhatsappNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> preInscricaoService.criarPreInscricao("Ana", null, "msg"));

            assertTrue(ex.getMessage().contains("WhatsApp"));
            verify(preInscricaoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lancar excecao quando whatsapp e vazio")
        void deveLancarExcecaoQuandoWhatsappVazio() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> preInscricaoService.criarPreInscricao("Ana", "   ", "msg"));

            assertTrue(ex.getMessage().contains("WhatsApp"));
            verify(preInscricaoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listarPreInscricoes")
    class ListarPreInscricoes {

        @Test
        @DisplayName("Deve retornar lista ordenada pelo repositorio")
        void deveRetornarListaOrdenada() {
            when(preInscricaoRepository.findAllByOrderByDataLeadDesc()).thenReturn(List.of(lead));

            List<PreInscricao> resultado = preInscricaoService.listarPreInscricoes();

            assertEquals(1, resultado.size());
            assertEquals(lead, resultado.get(0));
            verify(preInscricaoRepository, times(1)).findAllByOrderByDataLeadDesc();
        }
    }

    @Nested
    @DisplayName("marcarComoRespondido")
    class MarcarComoRespondido {

        @Test
        @DisplayName("Deve marcar lead como respondido")
        void deveMarcarLeadComoRespondido() {
            when(preInscricaoRepository.findById(leadId)).thenReturn(Optional.of(lead));
            when(preInscricaoRepository.save(any(PreInscricao.class))).thenAnswer(invocation -> invocation.getArgument(0));

            assertDoesNotThrow(() -> preInscricaoService.marcarComoRespondido(leadId));

            assertTrue(lead.isRespondido());
            verify(preInscricaoRepository, times(1)).findById(leadId);
            verify(preInscricaoRepository, times(1)).save(lead);
        }

        @Test
        @DisplayName("Deve lancar excecao quando id e nulo")
        void deveLancarExcecaoQuandoIdNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> preInscricaoService.marcarComoRespondido(null));

            assertTrue(ex.getMessage().contains("ID da pré-inscrição"));
            verify(preInscricaoRepository, never()).findById(any());
            verify(preInscricaoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lancar excecao quando lead nao encontrado")
        void deveLancarExcecaoQuandoLeadNaoEncontrado() {
            UUID idInexistente = UUID.randomUUID();
            when(preInscricaoRepository.findById(idInexistente)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> preInscricaoService.marcarComoRespondido(idInexistente));

            assertTrue(ex.getMessage().contains("Pré-inscrição não encontrada"));
            verify(preInscricaoRepository, times(1)).findById(idInexistente);
            verify(preInscricaoRepository, never()).save(any());
        }
    }
}
