package com.stepbystep.school.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.stepbystep.school.exception.FileUploadException;

@Service
public class FileUploadService {
     @Value("${upload.dir}")
    private String uploadDir;

    /**
     * Salva uma imagem no diretório configurado e retorna o nome do arquivo gerado.
     * 
     * @param imagemFile O arquivo MultipartFile a ser salvo
     * @return O nome do arquivo gerado (UUID + extensão)
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
            
            String extensao = "";
            int ultimoPonto = nomeOriginal.lastIndexOf(".");
            if (ultimoPonto > 0) {
                extensao = nomeOriginal.substring(ultimoPonto);
            }
            
            String nomeArquivo = UUID.randomUUID().toString() + extensao;

            // Define o caminho completo do arquivo e salva
            Path caminhoArquivo = uploadPath.resolve(nomeArquivo);
            Files.copy(imagemFile.getInputStream(), caminhoArquivo);

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
}
