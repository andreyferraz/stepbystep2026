package com.stepbystep.school.controller;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import com.stepbystep.school.service.FileUploadService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

@Controller
@RequiredArgsConstructor
public class UploadController {

    private final FileUploadService fileUploadService;

    @GetMapping("/uploads/{nomeArquivo:.+}")
    public ResponseEntity<Resource> servirUpload(@PathVariable("nomeArquivo") String nomeArquivo) {
        try {
            Path caminhoArquivo = fileUploadService.getCaminhoCompleto(nomeArquivo);
            if (!Files.exists(caminhoArquivo)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado.");
            }

            Resource recurso = new UrlResource(caminhoArquivo.toUri());
            MediaType mediaType = resolverMediaType(caminhoArquivo);

            return ResponseEntity.ok().contentType(mediaType).body(recurso);
        } catch (MalformedURLException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de arquivo inválido.");
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível carregar o arquivo.");
        }
    }

    private MediaType resolverMediaType(Path caminhoArquivo) {
        try {
            String contentType = Files.probeContentType(caminhoArquivo);
            if (contentType != null && !contentType.isBlank()) {
                return MediaType.parseMediaType(contentType);
            }
        } catch (Exception ex) {
            // Fallback por extensão para ambientes onde probeContentType não reconhece webp.
        }

        String nome = caminhoArquivo.getFileName() == null ? "" : caminhoArquivo.getFileName().toString().toLowerCase();
        if (nome.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (nome.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (nome.endsWith(".jpg") || nome.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (nome.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }

        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
