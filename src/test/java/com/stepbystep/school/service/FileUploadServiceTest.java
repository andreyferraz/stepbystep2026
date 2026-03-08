package com.stepbystep.school.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.stepbystep.school.exception.FileUploadException;

class FileUploadServiceTest {

    @TempDir
    Path tempDir;

    private FileUploadService fileUploadService;
    private ImageWriterSpi fakeWebpWriterSpi;

    @BeforeEach
    void setUp() {
        fileUploadService = new FileUploadService();
        setPrivateField(fileUploadService, "uploadDir", tempDir.toString());
        setPrivateField(fileUploadService, "webpQuality", 0.75f);

        // Usa writer fake para não depender da biblioteca nativa WebP no ambiente de teste.
        fakeWebpWriterSpi = new FakeWebpImageWriterSpi();
        IIORegistry registry = IIORegistry.getDefaultInstance();
        unregisterNativeWebpWriters(registry);
        registry.registerServiceProvider(fakeWebpWriterSpi);
    }

    @AfterEach
    void tearDown() {
        IIORegistry.getDefaultInstance().deregisterServiceProvider(fakeWebpWriterSpi);
    }

    @Test
    @DisplayName("Deve salvar imagem válida em WebP")
    void deveSalvarImagemValidaEmWebp() throws IOException {
        MultipartFile imagem = new MockMultipartFile(
                "imagem",
                "foto.png",
                "image/png",
                criarPngValido());

        String nomeArquivo = fileUploadService.salvarImagem(imagem);

        assertNotNull(nomeArquivo);
        assertTrue(nomeArquivo.endsWith(".webp"));
        assertTrue(fileUploadService.arquivoExiste(nomeArquivo));
    }

    @Test
    @DisplayName("Deve lançar exceção quando nenhum encoder WebP está disponível")
    void deveLancarExcecaoQuandoNaoExisteEncoderWebp() throws IOException {
        IIORegistry.getDefaultInstance().deregisterServiceProvider(fakeWebpWriterSpi);

        MultipartFile imagem = new MockMultipartFile(
                "imagem",
                "foto.png",
                "image/png",
                criarPngValido());

        FileUploadException ex = assertThrows(FileUploadException.class,
                () -> fileUploadService.salvarImagem(imagem));

        assertTrue(ex.getMessage().contains("Encoder WebP nao encontrado"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando nome do arquivo é nulo")
    void deveLancarExcecaoQuandoNomeArquivoNulo() {
        MultipartFile imagemSemNome = new MockMultipartFile(
                "imagem",
                null,
                "image/png",
                new byte[] {1, 2, 3});

        FileUploadException ex = assertThrows(FileUploadException.class,
                () -> fileUploadService.salvarImagem(imagemSemNome));

        assertTrue(ex.getMessage().contains("Nome do arquivo"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando nome do arquivo é vazio")
    void deveLancarExcecaoQuandoNomeArquivoVazio() {
        MultipartFile imagemSemNome = new MockMultipartFile(
                "imagem",
                "",
                "image/png",
                new byte[] {1, 2, 3});

        FileUploadException ex = assertThrows(FileUploadException.class,
                () -> fileUploadService.salvarImagem(imagemSemNome));

        assertTrue(ex.getMessage().contains("Nome do arquivo"));
    }

    @Test
    @DisplayName("Deve criar diretório de upload quando não existe")
    void deveCriarDiretorioUploadQuandoNaoExiste() throws IOException {
        Path novoDiretorio = tempDir.resolve("uploads-nao-existe");
        setPrivateField(fileUploadService, "uploadDir", novoDiretorio.toString());

        MultipartFile imagem = new MockMultipartFile(
                "imagem",
                "foto.png",
                "image/png",
                criarPngValido());

        String nomeArquivo = fileUploadService.salvarImagem(imagem);

        assertTrue(Files.exists(novoDiretorio));
        assertTrue(Files.exists(novoDiretorio.resolve(nomeArquivo)));
    }

    @Test
    @DisplayName("Deve lançar exceção quando conteúdo não é imagem")
    void deveLancarExcecaoQuandoConteudoNaoEhImagem() {
        MultipartFile arquivoInvalido = new MockMultipartFile(
                "imagem",
                "arquivo.txt",
                "text/plain",
                "nao eh imagem".getBytes());

        FileUploadException ex = assertThrows(FileUploadException.class,
                () -> fileUploadService.salvarImagem(arquivoInvalido));

        assertTrue(ex.getMessage().contains("imagem valida"));
    }

    @Test
    @DisplayName("Deve encapsular IOException ao salvar imagem")
    void deveEncapsularIOExceptionAoSalvarImagem() throws IOException {
        Path arquivoComoDiretorio = Files.createTempFile(tempDir, "upload", ".tmp");
        setPrivateField(fileUploadService, "uploadDir", arquivoComoDiretorio.toString());

        MultipartFile imagem = new MockMultipartFile(
                "imagem",
                "foto.png",
                "image/png",
                criarPngValido());

        FileUploadException ex = assertThrows(FileUploadException.class,
                () -> fileUploadService.salvarImagem(imagem));

        assertTrue(ex.getMessage().contains("Não foi possível salvar a imagem"));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof IOException);
    }

    @Test
    @DisplayName("Deve remover imagem quando arquivo existe")
    void deveRemoverImagemQuandoArquivoExiste() throws IOException {
        Path arquivo = Files.createFile(tempDir.resolve("arquivo.webp"));

        assertTrue(Files.exists(arquivo));

        assertDoesNotThrow(() -> fileUploadService.removerImagem("arquivo.webp"));

        assertFalse(Files.exists(arquivo));
    }

    @Test
    @DisplayName("Não deve falhar ao remover imagem com nome nulo ou vazio")
    void naoDeveFalharAoRemoverImagemComNomeNuloOuVazio() {
        assertDoesNotThrow(() -> fileUploadService.removerImagem(null));
        assertDoesNotThrow(() -> fileUploadService.removerImagem(""));
    }

    @Test
    @DisplayName("Deve encapsular IOException ao remover imagem")
    void deveEncapsularIOExceptionAoRemoverImagem() throws IOException {
        Path arquivoComoDiretorio = Files.createTempFile(tempDir, "upload", ".tmp");
        setPrivateField(fileUploadService, "uploadDir", arquivoComoDiretorio.toString());

        FileUploadException ex = assertThrows(FileUploadException.class,
                () -> fileUploadService.removerImagem("arquivo.webp"));

        assertTrue(ex.getMessage().contains("Não foi possível remover a imagem"));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof IOException);
    }

    @Test
    @DisplayName("Deve retornar false para nome nulo e vazio em arquivoExiste")
    void deveRetornarFalseParaNomeNuloEVazioEmArquivoExiste() {
        assertFalse(fileUploadService.arquivoExiste(null));
        assertFalse(fileUploadService.arquivoExiste(""));
    }

    @Test
    @DisplayName("Deve verificar existência de arquivo corretamente")
    void deveVerificarExistenciaDeArquivoCorretamente() throws IOException {
        Files.createFile(tempDir.resolve("existe.webp"));

        assertTrue(fileUploadService.arquivoExiste("existe.webp"));
        assertFalse(fileUploadService.arquivoExiste("nao-existe.webp"));
    }

    @Test
    @DisplayName("Deve retornar caminho completo corretamente")
    void deveRetornarCaminhoCompletoCorretamente() {
        Path esperado = tempDir.resolve("arquivo.webp");

        Path resultado = fileUploadService.getCaminhoCompleto("arquivo.webp");

        assertEquals(esperado, resultado);
    }

    @Test
    @DisplayName("Deve encapsular IOException de leitura do MultipartFile")
    void deveEncapsularIOExceptionDeLeituraMultipartFile() throws IOException {
        MultipartFile multipartFile = Mockito.mock(MultipartFile.class);
        Mockito.when(multipartFile.getOriginalFilename()).thenReturn("foto.png");
        Mockito.when(multipartFile.getInputStream()).thenThrow(new IOException("falha leitura"));

        FileUploadException ex = assertThrows(FileUploadException.class,
                () -> fileUploadService.salvarImagem(multipartFile));

        assertTrue(ex.getMessage().contains("Não foi possível salvar a imagem"));
        assertNotNull(ex.getCause());
        assertEquals("falha leitura", ex.getCause().getMessage());
    }

        @Test
        @DisplayName("Deve salvar documento PDF")
        void deveSalvarDocumentoPdf() {
        MultipartFile documento = new MockMultipartFile(
            "arquivo",
            "documento.pdf",
            "application/pdf",
            "conteudo pdf".getBytes());

        String nomeArquivo = fileUploadService.salvarDocumento(documento);

        assertNotNull(nomeArquivo);
        assertTrue(nomeArquivo.endsWith(".pdf"));
        assertTrue(fileUploadService.arquivoExiste(nomeArquivo));
        }

        @Test
        @DisplayName("Deve salvar documento DOCX")
        void deveSalvarDocumentoDocx() {
        MultipartFile documento = new MockMultipartFile(
            "arquivo",
            "documento.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "conteudo docx".getBytes());

        String nomeArquivo = fileUploadService.salvarDocumento(documento);

        assertNotNull(nomeArquivo);
        assertTrue(nomeArquivo.endsWith(".docx"));
        assertTrue(fileUploadService.arquivoExiste(nomeArquivo));
        }

        @Test
        @DisplayName("Deve aceitar extensão em maiúsculo no upload de documento")
        void deveAceitarExtensaoMaiusculaNoUploadDeDocumento() {
        MultipartFile documento = new MockMultipartFile(
            "arquivo",
            "DOCUMENTO.PDF",
            "application/pdf",
            "conteudo".getBytes());

        String nomeArquivo = fileUploadService.salvarDocumento(documento);

        assertTrue(nomeArquivo.endsWith(".pdf"));
        }

        @Test
        @DisplayName("Deve lançar exceção quando documento tem extensão inválida")
        void deveLancarExcecaoQuandoDocumentoTemExtensaoInvalida() {
        MultipartFile documento = new MockMultipartFile(
            "arquivo",
            "documento.txt",
            "text/plain",
            "conteudo".getBytes());

        FileUploadException ex = assertThrows(FileUploadException.class,
            () -> fileUploadService.salvarDocumento(documento));

        assertTrue(ex.getMessage().contains("Apenas PDF e DOCX"));
        }

        @Test
        @DisplayName("Deve lançar exceção quando documento não tem extensão")
        void deveLancarExcecaoQuandoDocumentoNaoTemExtensao() {
        MultipartFile documento = new MockMultipartFile(
            "arquivo",
            "documento",
            "application/octet-stream",
            "conteudo".getBytes());

        FileUploadException ex = assertThrows(FileUploadException.class,
            () -> fileUploadService.salvarDocumento(documento));

        assertTrue(ex.getMessage().contains("Apenas PDF e DOCX"));
        }

        @Test
        @DisplayName("Deve lançar exceção quando nome do documento é nulo")
        void deveLancarExcecaoQuandoNomeDocumentoNulo() {
        MultipartFile documento = new MockMultipartFile(
            "arquivo",
            null,
            "application/pdf",
            "conteudo".getBytes());

        FileUploadException ex = assertThrows(FileUploadException.class,
            () -> fileUploadService.salvarDocumento(documento));

        assertTrue(ex.getMessage().contains("Nome do arquivo"));
        }

        @Test
        @DisplayName("Deve lançar exceção quando nome do documento é vazio")
        void deveLancarExcecaoQuandoNomeDocumentoVazio() {
        MultipartFile documento = new MockMultipartFile(
            "arquivo",
            "   ",
            "application/pdf",
            "conteudo".getBytes());

        FileUploadException ex = assertThrows(FileUploadException.class,
            () -> fileUploadService.salvarDocumento(documento));

        assertTrue(ex.getMessage().contains("Nome do arquivo"));
        }

        @Test
        @DisplayName("Deve encapsular IOException ao salvar documento")
        void deveEncapsularIOExceptionAoSalvarDocumento() throws IOException {
        MultipartFile multipartFile = Mockito.mock(MultipartFile.class);
        Mockito.when(multipartFile.getOriginalFilename()).thenReturn("documento.pdf");
        Mockito.when(multipartFile.getInputStream()).thenThrow(new IOException("falha leitura documento"));

        FileUploadException ex = assertThrows(FileUploadException.class,
            () -> fileUploadService.salvarDocumento(multipartFile));

        assertTrue(ex.getMessage().contains("Não foi possível salvar o documento"));
        assertNotNull(ex.getCause());
        assertEquals("falha leitura documento", ex.getCause().getMessage());
        }

    private byte[] criarPngValido() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
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

    private void unregisterNativeWebpWriters(IIORegistry registry) {
        Iterator<ImageWriterSpi> providers = registry.getServiceProviders(ImageWriterSpi.class, true);
        while (providers.hasNext()) {
            ImageWriterSpi provider = providers.next();
            String className = provider.getClass().getName().toLowerCase(Locale.ROOT);
            if (className.contains("webp") && !provider.getClass().equals(FakeWebpImageWriterSpi.class)) {
                registry.deregisterServiceProvider(provider);
            }
        }
    }

    static class FakeWebpImageWriterSpi extends ImageWriterSpi {

        FakeWebpImageWriterSpi() {
            super(
                    "stepbystep-test",
                    "1.0",
                    new String[] {"webp"},
                    new String[] {"webp"},
                    new String[] {"image/webp"},
                    FakeWebpImageWriter.class.getName(),
                    new Class[] {ImageOutputStream.class},
                    null,
                    false,
                    null,
                    null,
                    null,
                    null,
                    false,
                    null,
                    null,
                    null,
                    null);
        }

        @Override
        public boolean canEncodeImage(ImageTypeSpecifier type) {
            return true;
        }

        @Override
        public ImageWriter createWriterInstance(Object extension) {
            return new FakeWebpImageWriter(this);
        }

        @Override
        public String getDescription(Locale locale) {
            return "Fake WebP writer for tests";
        }
    }

    static class FakeWebpImageWriter extends ImageWriter {

        FakeWebpImageWriter(ImageWriterSpi originatingProvider) {
            super(originatingProvider);
        }

        @Override
        public IIOMetadata getDefaultStreamMetadata(ImageWriteParam param) {
            return null;
        }

        @Override
        public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType, ImageWriteParam param) {
            return null;
        }

        @Override
        public IIOMetadata convertStreamMetadata(IIOMetadata inData, ImageWriteParam param) {
            return null;
        }

        @Override
        public IIOMetadata convertImageMetadata(IIOMetadata inData, ImageTypeSpecifier imageType, ImageWriteParam param) {
            return null;
        }

        @Override
        public ImageWriteParam getDefaultWriteParam() {
            return new FakeImageWriteParam();
        }

        @Override
        public void write(IIOMetadata streamMetadata, IIOImage image, ImageWriteParam param) throws IOException {
            ImageOutputStream output = (ImageOutputStream) getOutput();
            if (output != null) {
                output.write(new byte[] {1, 2, 3});
                output.flush();
            }
        }
    }

    static class FakeImageWriteParam extends ImageWriteParam {

        FakeImageWriteParam() {
            super(Locale.ROOT);
            this.canWriteCompressed = true;
            this.compressionTypes = new String[] {"fake"};
            this.compressionType = this.compressionTypes[0];
        }
    }
}
