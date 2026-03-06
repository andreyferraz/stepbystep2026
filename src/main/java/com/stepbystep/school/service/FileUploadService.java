package com.stepbystep.school.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.stepbystep.school.exception.FileUploadException;

@Service
public class FileUploadService {
     @Value("${upload.dir}")
    private String uploadDir;

    @Value("${upload.webp.quality:0.75}")
    private float webpQuality;

    /**
     * Salva uma imagem no diretório configurado e retorna o nome do arquivo gerado.
     * 
     * @param imagemFile O arquivo MultipartFile a ser salvo
     * @return O nome do arquivo gerado (UUID.webp)
     * @throws FileUploadException Se não conseguir salvar o arquivo
     */
    public String salvarImagem(MultipartFile imagemFile) {
        try {
            // Cria o diretório de upload se ele não existir
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Gera um nome de arquivo único para evitar colisões
            String nomeOriginal = imagemFile.getOriginalFilename();
            if (nomeOriginal == null || nomeOriginal.isEmpty()) {
                throw new FileUploadException("Nome do arquivo não pode ser nulo ou vazio");
            }

            String nomeArquivo = UUID.randomUUID().toString() + ".webp";

            // Define o caminho completo do arquivo e salva sempre em WebP
            Path caminhoArquivo = uploadPath.resolve(nomeArquivo);
            converterESalvarWebp(imagemFile, caminhoArquivo);

            return nomeArquivo;

        } catch (IOException e) {
            throw new FileUploadException("Não foi possível salvar a imagem. Erro: " + e.getMessage(), e);
        }
    }

    /**
     * Remove uma imagem do diretório de upload.
     * 
     * @param nomeArquivo O nome do arquivo a ser removido
     * @throws FileUploadException Se não conseguir remover o arquivo
     */
    public void removerImagem(String nomeArquivo) {
        try {
            if (nomeArquivo != null && !nomeArquivo.isEmpty()) {
                Path caminhoArquivo = Paths.get(uploadDir, nomeArquivo);
                Files.deleteIfExists(caminhoArquivo);
            }
        } catch (IOException e) {
            throw new FileUploadException("Não foi possível remover a imagem. Erro: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica se um arquivo existe no diretório de upload.
     * 
     * @param nomeArquivo O nome do arquivo a ser verificado
     * @return true se o arquivo existir, false caso contrário
     */
    public boolean arquivoExiste(String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.isEmpty()) {
            return false;
        }
        Path caminhoArquivo = Paths.get(uploadDir, nomeArquivo);
        return Files.exists(caminhoArquivo);
    }

    /**
     * Retorna o caminho completo para um arquivo no diretório de upload.
     * 
     * @param nomeArquivo O nome do arquivo
     * @return O caminho completo como Path
     */
    public Path getCaminhoCompleto(String nomeArquivo) {
        return Paths.get(uploadDir, nomeArquivo);
    }

    private void converterESalvarWebp(MultipartFile imagemFile, Path caminhoArquivo) throws IOException {
        BufferedImage bufferedImage;
        try (InputStream inputStream = imagemFile.getInputStream()) {
            bufferedImage = ImageIO.read(inputStream);
        }

        if (bufferedImage == null) {
            throw new FileUploadException("Arquivo enviado nao eh uma imagem valida");
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!writers.hasNext()) {
            throw new FileUploadException("Encoder WebP nao encontrado. Verifique a dependencia de WebP no projeto");
        }

        ImageWriter writer = writers.next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        if (writeParam.canWriteCompressed()) {
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            if (writeParam.getCompressionTypes() != null && writeParam.getCompressionTypes().length > 0) {
                writeParam.setCompressionType(writeParam.getCompressionTypes()[0]);
            }
            writeParam.setCompressionQuality(Math.max(0.0f, Math.min(1.0f, webpQuality)));
        }

        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(Files.newOutputStream(caminhoArquivo))) {
            writer.setOutput(imageOutputStream);
            writer.write(null, new IIOImage(bufferedImage, null, null), writeParam);
        } finally {
            writer.dispose();
        }
    }
}
