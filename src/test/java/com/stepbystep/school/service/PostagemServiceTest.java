package com.stepbystep.school.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stepbystep.school.model.Postagem;
import com.stepbystep.school.repository.PostagemRepository;

@ExtendWith(MockitoExtension.class)
class PostagemServiceTest {

    @Mock
    private PostagemRepository postagemRepository;

    @InjectMocks
    private PostagemService postagemService;

    @Nested
    @DisplayName("criarPostagem")
    class CriarPostagem {

        @Test
        @DisplayName("Deve criar postagem com data automática quando não informada")
        void deveCriarPostagemComDataAutomaticaQuandoNaoInformada() {
            Postagem postagem = Postagem.builder()
                    .titulo("Nova postagem")
                    .conteudo("Conteúdo da postagem")
                    .urlImagemCapa("capa.webp")
                    .build();

            when(postagemRepository.save(any(Postagem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Postagem retorno = postagemService.criarPostagem(postagem);

            assertNotNull(retorno);
            assertNotNull(retorno.getDataPublicacao());
            assertEquals("Nova postagem", retorno.getTitulo());
            verify(postagemRepository, times(1)).save(postagem);
        }

        @Test
        @DisplayName("Deve manter data de publicação quando já informada")
        void deveManterDataPublicacaoQuandoJaInformada() {
            LocalDateTime dataInformada = LocalDateTime.now().minusDays(1);
            Postagem postagem = Postagem.builder()
                    .titulo("Postagem")
                    .conteudo("Conteúdo")
                    .dataPublicacao(dataInformada)
                    .build();

            when(postagemRepository.save(any(Postagem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Postagem retorno = postagemService.criarPostagem(postagem);

            assertEquals(dataInformada, retorno.getDataPublicacao());
            verify(postagemRepository, times(1)).save(postagem);
        }

        @Test
        @DisplayName("Deve lançar exceção quando postagem é nula")
        void deveLancarExcecaoQuandoPostagemNula() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postagemService.criarPostagem(null));

            assertTrue(ex.getMessage().contains("Postagem"));
            verify(postagemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando título é inválido")
        void deveLancarExcecaoQuandoTituloInvalido() {
            Postagem postagem = Postagem.builder()
                    .titulo("  ")
                    .conteudo("Conteúdo válido")
                    .build();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postagemService.criarPostagem(postagem));

            assertTrue(ex.getMessage().contains("Título da postagem"));
            verify(postagemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando conteúdo é inválido")
        void deveLancarExcecaoQuandoConteudoInvalido() {
            Postagem postagem = Postagem.builder()
                    .titulo("Título válido")
                    .conteudo("")
                    .build();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postagemService.criarPostagem(postagem));

            assertTrue(ex.getMessage().contains("Conteúdo da postagem"));
            verify(postagemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("editarPostagem")
    class EditarPostagem {

        @Test
        @DisplayName("Deve editar postagem mantendo data original e atualizando capa quando informada")
        void deveEditarPostagemMantendoDataOriginalEAtualizandoCapa() {
            UUID id = UUID.randomUUID();
            LocalDateTime dataOriginal = LocalDateTime.now().minusDays(5);
            Postagem existente = Postagem.builder()
                    .id(id)
                    .titulo("Titulo antigo")
                    .conteudo("Conteudo antigo")
                    .dataPublicacao(dataOriginal)
                    .urlImagemCapa("capa-antiga.webp")
                    .build();
            Postagem entrada = Postagem.builder()
                    .titulo("Titulo novo")
                    .conteudo("Conteudo novo")
                    .urlImagemCapa("capa-nova.webp")
                    .build();

            when(postagemRepository.findById(id)).thenReturn(Optional.of(existente));
            when(postagemRepository.save(any(Postagem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Postagem retorno = postagemService.editarPostagem(id, entrada);

            assertEquals("Titulo novo", retorno.getTitulo());
            assertEquals("Conteudo novo", retorno.getConteudo());
            assertEquals("capa-nova.webp", retorno.getUrlImagemCapa());
            assertEquals(dataOriginal, retorno.getDataPublicacao());
            verify(postagemRepository, times(1)).save(existente);
        }

        @Test
        @DisplayName("Deve editar postagem sem alterar capa quando nova capa não informada")
        void deveEditarPostagemSemAlterarCapaQuandoNaoInformada() {
            UUID id = UUID.randomUUID();
            LocalDateTime dataOriginal = LocalDateTime.now().minusDays(5);
            Postagem existente = Postagem.builder()
                    .id(id)
                    .titulo("Titulo antigo")
                    .conteudo("Conteudo antigo")
                    .dataPublicacao(dataOriginal)
                    .urlImagemCapa("capa-antiga.webp")
                    .build();
            Postagem entrada = Postagem.builder()
                    .titulo("Titulo novo")
                    .conteudo("Conteudo novo")
                    .urlImagemCapa("   ")
                    .build();

            when(postagemRepository.findById(id)).thenReturn(Optional.of(existente));
            when(postagemRepository.save(any(Postagem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Postagem retorno = postagemService.editarPostagem(id, entrada);

            assertEquals("capa-antiga.webp", retorno.getUrlImagemCapa());
            assertEquals(dataOriginal, retorno.getDataPublicacao());
        }

        @Test
        @DisplayName("Deve lançar exceção quando postagem não encontrada na edição")
        void deveLancarExcecaoQuandoPostagemNaoEncontradaNaEdicao() {
            UUID id = UUID.randomUUID();
            Postagem entrada = Postagem.builder().titulo("Titulo").conteudo("Conteudo").build();

            when(postagemRepository.findById(id)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> postagemService.editarPostagem(id, entrada));

            assertTrue(ex.getMessage().contains("Postagem não encontrada com ID"));
            verify(postagemRepository, never()).save(any());
        }
    }

        @Nested
        @DisplayName("excluirPostagem")
        class ExcluirPostagem {

                @Test
                @DisplayName("Deve excluir postagem quando ela existe")
                void deveExcluirPostagemQuandoElaExiste() {
                        UUID id = UUID.randomUUID();
                        when(postagemRepository.existsById(id)).thenReturn(true);

                        postagemService.excluirPostagem(id);

                        verify(postagemRepository, times(1)).existsById(id);
                        verify(postagemRepository, times(1)).deleteById(id);
                }

                @Test
                @DisplayName("Deve lançar exceção quando ID da postagem é nulo")
                void deveLancarExcecaoQuandoIdNulo() {
                        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                        () -> postagemService.excluirPostagem(null));

                        assertTrue(ex.getMessage().contains("ID da postagem"));
                        verify(postagemRepository, never()).existsById(any());
                        verify(postagemRepository, never()).deleteById(any());
                }

                @Test
                @DisplayName("Deve lançar exceção quando postagem não existe")
                void deveLancarExcecaoQuandoPostagemNaoExiste() {
                        UUID id = UUID.randomUUID();
                        when(postagemRepository.existsById(id)).thenReturn(false);

                        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                        () -> postagemService.excluirPostagem(id));

                        assertTrue(ex.getMessage().contains("Postagem não encontrada com ID"));
                        verify(postagemRepository, times(1)).existsById(id);
                        verify(postagemRepository, never()).deleteById(any());
                }
        }
}
