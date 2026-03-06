package com.stepbystep.school.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.stepbystep.school.model.GaleriaFoto;
import com.stepbystep.school.repository.GaleriaFotoRepository;

@ExtendWith(MockitoExtension.class)
class GaleriaFotoServiceTest {

    @Mock
    private GaleriaFotoRepository galeriaFotoRepository;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private GaleriaFotoService galeriaFotoService;

    @Nested
    @DisplayName("salvarImagem")
    class SalvarImagem {

        @Test
        @DisplayName("Deve salvar imagem quando arquivo é válido")
        void deveSalvarImagemQuandoArquivoValido() {
            GaleriaFoto galeriaFoto = GaleriaFoto.builder().legenda("Evento").dataUpload(LocalDate.now()).build();
            when(multipartFile.isEmpty()).thenReturn(false);
            when(fileUploadService.salvarImagem(multipartFile)).thenReturn("foto.webp");

            galeriaFotoService.salvarImagem(galeriaFoto, multipartFile);

            assertEquals("foto.webp", galeriaFoto.getUrlImagem());
            verify(fileUploadService, times(1)).salvarImagem(multipartFile);
            verify(galeriaFotoRepository, times(1)).save(galeriaFoto);
        }

        @Test
        @DisplayName("Deve lançar exceção quando arquivo é nulo")
        void deveLancarExcecaoQuandoArquivoNulo() {
            GaleriaFoto galeriaFoto = GaleriaFoto.builder().build();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> galeriaFotoService.salvarImagem(galeriaFoto, null));

            assertTrue(ex.getMessage().contains("Imagem é obrigatória"));
            verify(fileUploadService, never()).salvarImagem(any());
            verify(galeriaFotoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve salvar sem upload quando arquivo vazio e URL já preenchida")
        void deveSalvarSemUploadQuandoArquivoVazioEUrlPreenchida() {
            GaleriaFoto galeriaFoto = GaleriaFoto.builder().urlImagem("ja-existente.webp").build();
            when(multipartFile.isEmpty()).thenReturn(true);

            galeriaFotoService.salvarImagem(galeriaFoto, multipartFile);

            verify(fileUploadService, never()).salvarImagem(any());
            verify(galeriaFotoRepository, times(1)).save(galeriaFoto);
        }

        @Test
        @DisplayName("Deve lançar exceção quando arquivo vazio e URL da imagem ausente")
        void deveLancarExcecaoQuandoArquivoVazioEUrlAusente() {
            GaleriaFoto galeriaFoto = GaleriaFoto.builder().urlImagem(null).build();
            when(multipartFile.isEmpty()).thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> galeriaFotoService.salvarImagem(galeriaFoto, multipartFile));

            assertTrue(ex.getMessage().contains("URL da imagem é obrigatória"));
            verify(fileUploadService, never()).salvarImagem(any());
            verify(galeriaFotoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando upload retorna URL nula")
        void deveLancarExcecaoQuandoUploadRetornaUrlNula() {
            GaleriaFoto galeriaFoto = GaleriaFoto.builder().build();
            when(multipartFile.isEmpty()).thenReturn(false);
            when(fileUploadService.salvarImagem(multipartFile)).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> galeriaFotoService.salvarImagem(galeriaFoto, multipartFile));

            assertTrue(ex.getMessage().contains("URL da imagem é obrigatória"));
            verify(galeriaFotoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve propagar NullPointerException quando galeriaFoto é nula")
        void devePropagarNullPointerQuandoGaleriaFotoNula() {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(fileUploadService.salvarImagem(multipartFile)).thenReturn("foto.webp");

            assertThrows(NullPointerException.class,
                    () -> galeriaFotoService.salvarImagem(null, multipartFile));

            verify(galeriaFotoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listarTodas")
    class ListarTodas {

        @Test
        @DisplayName("Deve retornar lista de imagens")
        void deveRetornarListaDeImagens() {
            List<GaleriaFoto> fotos = List.of(
                    GaleriaFoto.builder().id(UUID.randomUUID()).urlImagem("a.webp").build(),
                    GaleriaFoto.builder().id(UUID.randomUUID()).urlImagem("b.webp").build());
            when(galeriaFotoRepository.findAll()).thenReturn(fotos);

            List<GaleriaFoto> resultado = galeriaFotoService.listarTodas();

            assertEquals(2, resultado.size());
            assertEquals("a.webp", resultado.get(0).getUrlImagem());
            verify(galeriaFotoRepository, times(1)).findAll();
        }
    }

    @Nested
    @DisplayName("excluirImagem")
    class ExcluirImagem {

        @Test
        @DisplayName("Deve excluir imagem pelo ID")
        void deveExcluirImagemPeloId() {
            UUID id = UUID.randomUUID();

            galeriaFotoService.excluirImagem(id);

            verify(galeriaFotoRepository, times(1)).deleteById(id);
        }
    }
}
