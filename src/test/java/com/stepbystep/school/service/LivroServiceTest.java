package com.stepbystep.school.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.stepbystep.school.model.Livro;
import com.stepbystep.school.repository.LivroRepository;

@ExtendWith(MockitoExtension.class)
class LivroServiceTest {

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private LivroRepository livroRepository;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private LivroService livroService;

    private Livro livroValido;
    private UUID livroId;

    @BeforeEach
    void setUp() {
        livroId = UUID.randomUUID();
        livroValido = Livro.builder()
                .id(livroId)
                .titulo("Clean Code")
                .sinopse("Livro sobre boas práticas de programação")
                .urlCapa("capa-antiga.webp")
                .anoLancamento(2008)
                .linkCompra(null)
                .build();
    }

    @Nested
    @DisplayName("salvarLivro")
    class SalvarLivro {

        @Test
        @DisplayName("Deve salvar livro com upload de capa")
        void deveSalvarLivroComUploadDeCapa() {
            Livro livro = Livro.builder()
                    .titulo("Domain-Driven Design")
                    .sinopse("Livro sobre DDD")
                    .anoLancamento(2003)
                    .build();

            when(file.isEmpty()).thenReturn(false);
            when(fileUploadService.salvarImagem(file)).thenReturn("nova-capa.webp");

            assertDoesNotThrow(() -> livroService.salvarLivro(livro, file));

            assertEquals("nova-capa.webp", livro.getUrlCapa());
            verify(fileUploadService, times(1)).salvarImagem(file);
            verify(livroRepository, times(1)).save(livro);
        }

        @Test
        @DisplayName("Deve salvar livro sem upload quando capa já existe")
        void deveSalvarLivroSemUploadQuandoCapaJaExiste() {
            Livro livro = Livro.builder()
                    .titulo("Refactoring")
                    .sinopse("Melhoria de código")
                    .anoLancamento(1999)
                    .urlCapa("capa-existente.webp")
                    .build();

            assertDoesNotThrow(() -> livroService.salvarLivro(livro, null));

            verify(fileUploadService, never()).salvarImagem(any());
            verify(livroRepository, times(1)).save(livro);
        }

        @Test
        @DisplayName("Deve lançar exceção quando livro é nulo")
        void deveLancarExcecaoQuandoLivroNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> livroService.salvarLivro(null, file));

            assertTrue(ex.getMessage().contains("Livro"));
            verify(livroRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando título é inválido")
        void deveLancarExcecaoQuandoTituloInvalido() {
            livroValido.setTitulo("   ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> livroService.salvarLivro(livroValido, file));

            assertTrue(ex.getMessage().contains("título"));
            verify(livroRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando sinopse é inválida")
        void deveLancarExcecaoQuandoSinopseInvalida() {
            livroValido.setSinopse("");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> livroService.salvarLivro(livroValido, file));

            assertTrue(ex.getMessage().contains("sinopse"));
            verify(livroRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando ano de lançamento é inválido")
        void deveLancarExcecaoQuandoAnoLancamentoInvalido() {
            livroValido.setAnoLancamento(0);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> livroService.salvarLivro(livroValido, file));

            assertTrue(ex.getMessage().contains("ano de lançamento"));
            verify(livroRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando capa não é informada")
        void deveLancarExcecaoQuandoCapaNaoInformada() {
            Livro livro = Livro.builder()
                    .titulo("Effective Java")
                    .sinopse("Boas práticas em Java")
                    .anoLancamento(2018)
                    .urlCapa(null)
                    .build();

            when(file.isEmpty()).thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> livroService.salvarLivro(livro, file));

            assertTrue(ex.getMessage().contains("imagem"));
            verify(fileUploadService, never()).salvarImagem(any());
            verify(livroRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("editarLivro")
    class EditarLivro {

        @Test
        @DisplayName("Deve editar livro e atualizar capa quando há novo upload")
        void deveEditarLivroEAtualizarCapaQuandoNovoUpload() {
            Livro entrada = Livro.builder()
                    .id(livroId)
                    .titulo("Novo titulo")
                    .sinopse("Nova sinopse")
                    .anoLancamento(2020)
                    .linkCompra("https://exemplo.com/livro")
                    .build();

            Livro livroExistente = Livro.builder()
                    .id(livroId)
                    .titulo("Antigo")
                    .sinopse("Antiga")
                    .anoLancamento(2010)
                    .urlCapa("capa-antiga.webp")
                    .linkCompra(null)
                    .build();

            when(livroRepository.findById(livroId)).thenReturn(Optional.of(livroExistente));
            when(file.isEmpty()).thenReturn(false);
            when(fileUploadService.salvarImagem(file)).thenReturn("capa-nova.webp");
            when(livroRepository.save(livroExistente)).thenReturn(livroExistente);

            Livro retorno = livroService.editarLivro(entrada, file);

            assertNotNull(retorno);
            assertEquals("Novo titulo", livroExistente.getTitulo());
            assertEquals("Nova sinopse", livroExistente.getSinopse());
            assertEquals(2020, livroExistente.getAnoLancamento());
            assertEquals("https://exemplo.com/livro", livroExistente.getLinkCompra());
            assertEquals("capa-nova.webp", livroExistente.getUrlCapa());
            verify(fileUploadService, times(1)).salvarImagem(file);
            verify(livroRepository, times(1)).save(livroExistente);
        }

        @Test
        @DisplayName("Deve editar livro sem alterar capa quando não há upload")
        void deveEditarLivroSemAlterarCapaQuandoSemUpload() {
            Livro entrada = Livro.builder()
                    .id(livroId)
                    .titulo("Titulo atualizado")
                    .sinopse("Sinopse atualizada")
                    .anoLancamento(2021)
                    .linkCompra(null)
                    .build();

            Livro livroExistente = Livro.builder()
                    .id(livroId)
                    .titulo("Antigo")
                    .sinopse("Antiga")
                    .anoLancamento(2010)
                    .urlCapa("capa-antiga.webp")
                    .linkCompra("https://antigo")
                    .build();

            when(livroRepository.findById(livroId)).thenReturn(Optional.of(livroExistente));
            when(livroRepository.save(livroExistente)).thenReturn(livroExistente);

            Livro retorno = livroService.editarLivro(entrada, null);

            assertNotNull(retorno);
            assertEquals("capa-antiga.webp", livroExistente.getUrlCapa());
            assertNull(livroExistente.getLinkCompra());
            verify(fileUploadService, never()).salvarImagem(any());
            verify(livroRepository, times(1)).save(livroExistente);
        }

        @Test
        @DisplayName("Deve lançar exceção quando livro de entrada é nulo")
        void deveLancarExcecaoQuandoLivroEntradaNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> livroService.editarLivro(null, file));

            assertTrue(ex.getMessage().contains("Livro"));
            verify(livroRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando ID é nulo")
        void deveLancarExcecaoQuandoIdNulo() {
            livroValido.setId(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> livroService.editarLivro(livroValido, file));

            assertTrue(ex.getMessage().contains("ID do livro"));
            verify(livroRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando livro não existe")
        void deveLancarExcecaoQuandoLivroNaoExiste() {
            when(livroRepository.findById(livroId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> livroService.editarLivro(livroValido, file));

            assertTrue(ex.getMessage().contains("Livro não encontrado com ID"));
            verify(livroRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando título é inválido na edição")
        void deveLancarExcecaoQuandoTituloInvalidoNaEdicao() {
            livroValido.setTitulo(" ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> livroService.editarLivro(livroValido, file));

            assertTrue(ex.getMessage().contains("título"));
            verify(livroRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando sinopse é inválida na edição")
        void deveLancarExcecaoQuandoSinopseInvalidaNaEdicao() {
            livroValido.setSinopse(" ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> livroService.editarLivro(livroValido, file));

            assertTrue(ex.getMessage().contains("sinopse"));
            verify(livroRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando ano é inválido na edição")
        void deveLancarExcecaoQuandoAnoInvalidoNaEdicao() {
            livroValido.setAnoLancamento(-1);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> livroService.editarLivro(livroValido, file));

            assertTrue(ex.getMessage().contains("ano de lançamento"));
            verify(livroRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando livro existente não possui capa e não há upload")
        void deveLancarExcecaoQuandoLivroExistenteSemCapaESemUpload() {
            Livro entrada = Livro.builder()
                    .id(livroId)
                    .titulo("Titulo")
                    .sinopse("Sinopse")
                    .anoLancamento(2022)
                    .build();

            Livro livroExistente = Livro.builder()
                    .id(livroId)
                    .titulo("Antigo")
                    .sinopse("Antiga")
                    .anoLancamento(2010)
                    .urlCapa(null)
                    .build();

            when(livroRepository.findById(livroId)).thenReturn(Optional.of(livroExistente));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> livroService.editarLivro(entrada, null));

            assertTrue(ex.getMessage().contains("imagem"));
            verify(livroRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("excluirLivro")
    class ExcluirLivro {

        @Test
        @DisplayName("Deve excluir livro com sucesso")
        void deveExcluirLivroComSucesso() {
            when(livroRepository.existsById(livroId)).thenReturn(true);

            assertDoesNotThrow(() -> livroService.excluirLivro(livroId));

            verify(livroRepository, times(1)).deleteById(livroId);
        }

        @Test
        @DisplayName("Deve lançar exceção quando ID é nulo")
        void deveLancarExcecaoQuandoIdNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> livroService.excluirLivro(null));

            assertTrue(ex.getMessage().contains("ID do livro"));
            verify(livroRepository, never()).existsById(any());
            verify(livroRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando livro não existe")
        void deveLancarExcecaoQuandoLivroNaoExiste() {
            when(livroRepository.existsById(livroId)).thenReturn(false);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> livroService.excluirLivro(livroId));

            assertTrue(ex.getMessage().contains("Livro não encontrado com ID"));
            verify(livroRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("listarLivroPorId")
    class ListarLivroPorId {

        @Test
        @DisplayName("Deve retornar livro quando encontrado")
        void deveRetornarLivroQuandoEncontrado() {
            when(livroRepository.findById(livroId)).thenReturn(Optional.of(livroValido));

            Livro retorno = livroService.listarLivroPorId(livroId);

            assertNotNull(retorno);
            assertEquals(livroId, retorno.getId());
            verify(livroRepository, times(1)).findById(livroId);
        }

        @Test
        @DisplayName("Deve lançar exceção quando ID é nulo")
        void deveLancarExcecaoQuandoIdNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> livroService.listarLivroPorId(null));

            assertTrue(ex.getMessage().contains("ID do livro"));
            verify(livroRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando livro não encontrado")
        void deveLancarExcecaoQuandoLivroNaoEncontrado() {
            when(livroRepository.findById(livroId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> livroService.listarLivroPorId(livroId));

            assertTrue(ex.getMessage().contains("Livro não encontrado com ID"));
        }
    }

    @Nested
    @DisplayName("listarTodosLivros")
    class ListarTodosLivros {

        @Test
        @DisplayName("Deve retornar todos os livros")
        void deveRetornarTodosOsLivros() {
            List<Livro> livros = List.of(
                    Livro.builder().id(UUID.randomUUID()).titulo("Livro A").sinopse("S1").anoLancamento(2001).urlCapa("a.webp").build(),
                    Livro.builder().id(UUID.randomUUID()).titulo("Livro B").sinopse("S2").anoLancamento(2002).urlCapa("b.webp").build());
            when(livroRepository.findAll()).thenReturn(livros);

            Iterable<Livro> retorno = livroService.listarTodosLivros();

            assertIterableEquals(livros, retorno);
            verify(livroRepository, times(1)).findAll();
        }
    }
}
