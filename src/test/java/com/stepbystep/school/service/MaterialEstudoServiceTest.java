package com.stepbystep.school.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.stepbystep.school.model.MaterialEstudo;
import com.stepbystep.school.model.Turma;
import com.stepbystep.school.repository.MaterialEstudoRepository;

@ExtendWith(MockitoExtension.class)
class MaterialEstudoServiceTest {

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private MaterialEstudoRepository materialEstudoRepository;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private MaterialEstudoService materialEstudoService;

    private Turma turma;
    private UUID materialId;
    private UUID turmaId;
    private MaterialEstudo materialValido;

    @BeforeEach
    void setUp() {
        turmaId = UUID.randomUUID();
        materialId = UUID.randomUUID();
        turma = Turma.builder().id(turmaId).nome("Turma A").build();

        materialValido = MaterialEstudo.builder()
                .id(materialId)
                .titulo("Simple Present")
                .descricao("Lista de exercicios")
                .dataUpload(LocalDateTime.now())
                .urlArquivo("material-antigo.pdf")
                .turma(turma)
                .build();
    }

    @Nested
    @DisplayName("salvarMaterialEstudo")
    class SalvarMaterialEstudo {

        @Test
        @DisplayName("Deve salvar material com arquivo valido")
        void deveSalvarMaterialComArquivoValido() {
            MaterialEstudo material = MaterialEstudo.builder()
                    .titulo("Past Tense")
                    .descricao("Apostila")
                    .dataUpload(LocalDateTime.now())
                    .turma(turma)
                    .build();

            when(file.isEmpty()).thenReturn(false);
            when(fileUploadService.salvarDocumento(file)).thenReturn("material.pdf");

            assertDoesNotThrow(() -> materialEstudoService.salvarMaterialEstudo(material, file));

            assertEquals("material.pdf", material.getUrlArquivo());
            verify(fileUploadService, times(1)).salvarDocumento(file);
            verify(materialEstudoRepository, times(1)).save(material);
        }

        @Test
        @DisplayName("Deve lancar excecao quando material eh nulo")
        void deveLancarExcecaoQuandoMaterialNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> materialEstudoService.salvarMaterialEstudo(null, file));

            assertTrue(ex.getMessage().contains("Material de estudo"));
            verify(materialEstudoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lancar excecao quando titulo eh invalido")
        void deveLancarExcecaoQuandoTituloInvalido() {
            materialValido.setTitulo("   ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> materialEstudoService.salvarMaterialEstudo(materialValido, file));

            assertTrue(ex.getMessage().contains("Título"));
            verify(materialEstudoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lancar excecao quando descricao eh invalida")
        void deveLancarExcecaoQuandoDescricaoInvalida() {
            materialValido.setDescricao("");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> materialEstudoService.salvarMaterialEstudo(materialValido, file));

            assertTrue(ex.getMessage().contains("Descrição"));
            verify(materialEstudoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lancar excecao quando dataUpload eh nula")
        void deveLancarExcecaoQuandoDataUploadNula() {
            materialValido.setDataUpload(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> materialEstudoService.salvarMaterialEstudo(materialValido, file));

            assertTrue(ex.getMessage().contains("Data de upload"));
            verify(materialEstudoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lancar excecao quando turma eh nula")
        void deveLancarExcecaoQuandoTurmaNula() {
            materialValido.setTurma(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> materialEstudoService.salvarMaterialEstudo(materialValido, file));

            assertTrue(ex.getMessage().contains("Turma"));
            verify(materialEstudoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lancar excecao quando arquivo eh nulo")
        void deveLancarExcecaoQuandoArquivoNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> materialEstudoService.salvarMaterialEstudo(materialValido, null));

            assertTrue(ex.getMessage().contains("Arquivo"));
            verify(fileUploadService, never()).salvarDocumento(any());
            verify(materialEstudoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lancar excecao quando arquivo esta vazio")
        void deveLancarExcecaoQuandoArquivoVazio() {
            when(file.isEmpty()).thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> materialEstudoService.salvarMaterialEstudo(materialValido, file));

            assertTrue(ex.getMessage().contains("Arquivo é obrigatório"));
            verify(fileUploadService, never()).salvarDocumento(any());
            verify(materialEstudoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lancar excecao quando upload retorna URL nula")
        void deveLancarExcecaoQuandoUploadRetornaUrlNula() {
            when(file.isEmpty()).thenReturn(false);
            when(fileUploadService.salvarDocumento(file)).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> materialEstudoService.salvarMaterialEstudo(materialValido, file));

            assertTrue(ex.getMessage().contains("URL do arquivo"));
            verify(materialEstudoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("editarMaterialEstudo")
    class EditarMaterialEstudo {

        @Test
        @DisplayName("Deve editar material e atualizar arquivo quando novo upload")
        void deveEditarMaterialEAtualizarArquivoQuandoNovoUpload() {
            MaterialEstudo entrada = MaterialEstudo.builder()
                    .id(materialId)
                    .titulo("Titulo novo")
                    .descricao("Descricao nova")
                    .dataUpload(LocalDateTime.now().plusDays(1))
                    .turma(turma)
                    .build();

            MaterialEstudo existente = MaterialEstudo.builder()
                    .id(materialId)
                    .titulo("Titulo antigo")
                    .descricao("Descricao antiga")
                    .dataUpload(LocalDateTime.now())
                    .urlArquivo("material-antigo.pdf")
                    .turma(turma)
                    .build();

            when(materialEstudoRepository.findById(materialId)).thenReturn(Optional.of(existente));
            when(file.isEmpty()).thenReturn(false);
            when(fileUploadService.salvarDocumento(file)).thenReturn("material-novo.pdf");
            when(materialEstudoRepository.save(existente)).thenReturn(existente);

            MaterialEstudo retorno = materialEstudoService.editarMaterialEstudo(entrada, file);

            assertNotNull(retorno);
            assertEquals("Titulo novo", existente.getTitulo());
            assertEquals("Descricao nova", existente.getDescricao());
            assertEquals("material-novo.pdf", existente.getUrlArquivo());
            verify(fileUploadService, times(1)).salvarDocumento(file);
            verify(materialEstudoRepository, times(1)).save(existente);
        }

        @Test
        @DisplayName("Deve editar material sem alterar arquivo quando file nulo")
        void deveEditarMaterialSemAlterarArquivoQuandoFileNulo() {
            MaterialEstudo entrada = MaterialEstudo.builder()
                    .id(materialId)
                    .titulo("Titulo atualizado")
                    .descricao("Descricao atualizada")
                    .dataUpload(null)
                    .turma(turma)
                    .build();

            MaterialEstudo existente = MaterialEstudo.builder()
                    .id(materialId)
                    .titulo("Antigo")
                    .descricao("Antiga")
                    .dataUpload(LocalDateTime.now())
                    .urlArquivo("material-antigo.pdf")
                    .turma(turma)
                    .build();

            when(materialEstudoRepository.findById(materialId)).thenReturn(Optional.of(existente));
            when(materialEstudoRepository.save(existente)).thenReturn(existente);

            MaterialEstudo retorno = materialEstudoService.editarMaterialEstudo(entrada, null);

            assertNotNull(retorno);
            assertEquals("material-antigo.pdf", existente.getUrlArquivo());
            verify(fileUploadService, never()).salvarDocumento(any());
            verify(materialEstudoRepository, times(1)).save(existente);
        }

        @Test
        @DisplayName("Deve manter dataUpload anterior quando entrada nao informar data")
        void deveManterDataUploadAnteriorQuandoEntradaNaoInformarData() {
            LocalDateTime dataOriginal = LocalDateTime.now();
            MaterialEstudo entrada = MaterialEstudo.builder()
                    .id(materialId)
                    .titulo("Titulo atualizado")
                    .descricao("Descricao atualizada")
                    .dataUpload(null)
                    .turma(turma)
                    .build();

            MaterialEstudo existente = MaterialEstudo.builder()
                    .id(materialId)
                    .titulo("Antigo")
                    .descricao("Antiga")
                    .dataUpload(dataOriginal)
                    .urlArquivo("material-antigo.pdf")
                    .turma(turma)
                    .build();

            when(materialEstudoRepository.findById(materialId)).thenReturn(Optional.of(existente));
            when(materialEstudoRepository.save(existente)).thenReturn(existente);

            materialEstudoService.editarMaterialEstudo(entrada, null);

            assertEquals(dataOriginal, existente.getDataUpload());
        }

        @Test
        @DisplayName("Deve lancar excecao quando material de entrada eh nulo")
        void deveLancarExcecaoQuandoMaterialEntradaNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> materialEstudoService.editarMaterialEstudo(null, file));

            assertTrue(ex.getMessage().contains("Material de estudo"));
            verify(materialEstudoRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Deve lancar excecao quando ID do material eh nulo")
        void deveLancarExcecaoQuandoIdMaterialNulo() {
            materialValido.setId(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> materialEstudoService.editarMaterialEstudo(materialValido, file));

            assertTrue(ex.getMessage().contains("ID do material de estudo"));
            verify(materialEstudoRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Deve lancar excecao quando titulo eh invalido na edicao")
        void deveLancarExcecaoQuandoTituloInvalidoNaEdicao() {
            materialValido.setTitulo(" ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> materialEstudoService.editarMaterialEstudo(materialValido, file));

            assertTrue(ex.getMessage().contains("Título"));
            verify(materialEstudoRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Deve lancar excecao quando descricao eh invalida na edicao")
        void deveLancarExcecaoQuandoDescricaoInvalidaNaEdicao() {
            materialValido.setDescricao(" ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> materialEstudoService.editarMaterialEstudo(materialValido, file));

            assertTrue(ex.getMessage().contains("Descrição"));
            verify(materialEstudoRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Deve lancar excecao quando turma eh nula na edicao")
        void deveLancarExcecaoQuandoTurmaNulaNaEdicao() {
            materialValido.setTurma(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> materialEstudoService.editarMaterialEstudo(materialValido, file));

            assertTrue(ex.getMessage().contains("Turma"));
            verify(materialEstudoRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Deve lancar excecao quando material nao encontrado")
        void deveLancarExcecaoQuandoMaterialNaoEncontrado() {
            when(materialEstudoRepository.findById(materialId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> materialEstudoService.editarMaterialEstudo(materialValido, file));

            assertTrue(ex.getMessage().contains("Material de estudo não encontrado com ID"));
            verify(materialEstudoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lancar excecao quando sem upload e material existente sem URL")
        void deveLancarExcecaoQuandoSemUploadEMaterialSemUrl() {
            MaterialEstudo entrada = MaterialEstudo.builder()
                    .id(materialId)
                    .titulo("Titulo")
                    .descricao("Descricao")
                    .dataUpload(LocalDateTime.now())
                    .turma(turma)
                    .build();

            MaterialEstudo existente = MaterialEstudo.builder()
                    .id(materialId)
                    .titulo("Antigo")
                    .descricao("Antiga")
                    .dataUpload(LocalDateTime.now())
                    .urlArquivo(null)
                    .turma(turma)
                    .build();

            when(materialEstudoRepository.findById(materialId)).thenReturn(Optional.of(existente));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> materialEstudoService.editarMaterialEstudo(entrada, null));

            assertTrue(ex.getMessage().contains("URL do arquivo"));
            verify(materialEstudoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listarPorTurma")
    class ListarPorTurma {

        @Test
        @DisplayName("Deve listar materiais por turma")
        void deveListarMateriaisPorTurma() {
            List<MaterialEstudo> materiais = List.of(materialValido);
            when(materialEstudoRepository.findByTurmaId(turmaId)).thenReturn(materiais);

            List<MaterialEstudo> retorno = materialEstudoService.listarPorTurma(turmaId);

            assertEquals(1, retorno.size());
            assertEquals(materialValido, retorno.get(0));
            verify(materialEstudoRepository, times(1)).findByTurmaId(turmaId);
        }

        @Test
        @DisplayName("Deve lancar excecao quando ID da turma eh nulo")
        void deveLancarExcecaoQuandoIdTurmaNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> materialEstudoService.listarPorTurma(null));

            assertTrue(ex.getMessage().contains("ID da turma"));
            verify(materialEstudoRepository, never()).findByTurmaId(any());
        }
    }

    @Nested
    @DisplayName("excluirMaterialEstudo")
    class ExcluirMaterialEstudo {

        @Test
        @DisplayName("Deve excluir material com sucesso")
        void deveExcluirMaterialComSucesso() {
            when(materialEstudoRepository.findById(materialId)).thenReturn(Optional.of(materialValido));

            assertDoesNotThrow(() -> materialEstudoService.excluirMaterialEstudo(materialId));

            verify(materialEstudoRepository, times(1)).delete(materialValido);
        }

        @Test
        @DisplayName("Deve lancar excecao quando ID do material eh nulo")
        void deveLancarExcecaoQuandoIdMaterialNulo() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> materialEstudoService.excluirMaterialEstudo(null));

            assertTrue(ex.getMessage().contains("ID do material de estudo"));
            verify(materialEstudoRepository, never()).deleteById(any());
        }
    }
}
