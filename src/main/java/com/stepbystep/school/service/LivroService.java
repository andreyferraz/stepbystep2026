package com.stepbystep.school.service;

import java.util.UUID;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.stepbystep.school.model.Livro;
import com.stepbystep.school.repository.LivroRepository;
import com.stepbystep.school.util.ValidationUtils;

@Service
public class LivroService {

    private static final String CAMPO_ID_LIVRO = "ID do livro";
    private static final String MSG_LIVRO_NAO_ENCONTRADO = "Livro não encontrado com ID: ";

    private final FileUploadService fileUploadService;
    private final LivroRepository livroRepository;

    public LivroService(FileUploadService fileUploadService, LivroRepository livroRepository) {
        this.fileUploadService = fileUploadService;
        this.livroRepository = livroRepository;
    }

    public void salvarLivro(Livro livro, MultipartFile file) {

        ValidationUtils.validarCampoObrigatorio(livro, "Livro");
        ValidationUtils.validarCampoStringObrigatorio(livro.getTitulo(), "O título é obrigatorio");
        ValidationUtils.validarCampoStringObrigatorio(livro.getSinopse(), "A sinopse é obrigatoria");
        if (livro.getAnoLancamento() <= 0) {
            throw new IllegalArgumentException("O ano de lançamento é obrigatório.");
        }

        if (file != null && !file.isEmpty()) {
            String filePath = fileUploadService.salvarImagem(file);
            livro.setUrlCapa(montarUrlPublicaUpload(filePath));
        }

        livro.setUrlCapa(normalizarUrlCapa(livro.getUrlCapa()));
        ValidationUtils.validarCampoObrigatorio(livro.getUrlCapa(), "A imagem é obrigatória");
        livroRepository.save(livro);

    }

    public Livro editarLivro(Livro livro, MultipartFile file) {

        ValidationUtils.validarCampoObrigatorio(livro, "Livro");
        ValidationUtils.validarCampoObrigatorio(livro.getId(), CAMPO_ID_LIVRO);
        ValidationUtils.validarCampoStringObrigatorio(livro.getTitulo(), "O título é obrigatorio");
        ValidationUtils.validarCampoStringObrigatorio(livro.getSinopse(), "A sinopse é obrigatoria");
        if (livro.getAnoLancamento() <= 0) {
            throw new IllegalArgumentException("O ano de lançamento é obrigatório.");
        }

        Livro livroExistente = livroRepository.findById(livro.getId())
                .orElseThrow(() -> new IllegalArgumentException(MSG_LIVRO_NAO_ENCONTRADO + livro.getId()));

        if (file != null && !file.isEmpty()) {
            String filePath = fileUploadService.salvarImagem(file);
            livroExistente.setUrlCapa(montarUrlPublicaUpload(filePath));
        }

        livroExistente.setTitulo(livro.getTitulo());
        livroExistente.setSinopse(livro.getSinopse());
        livroExistente.setAnoLancamento(livro.getAnoLancamento());
        livroExistente.setLinkCompra(livro.getLinkCompra());
        livroExistente.setUrlCapa(normalizarUrlCapa(livroExistente.getUrlCapa()));

        ValidationUtils.validarCampoObrigatorio(livroExistente.getUrlCapa(), "A imagem é obrigatória");

        return livroRepository.save(livroExistente);
    }

    public void excluirLivro(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, CAMPO_ID_LIVRO);
        if (!livroRepository.existsById(id)) {
            throw new IllegalArgumentException(MSG_LIVRO_NAO_ENCONTRADO + id);
        }
        livroRepository.deleteById(id);
    }

    public Livro listarLivroPorId(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, CAMPO_ID_LIVRO);
        Livro livro = livroRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MSG_LIVRO_NAO_ENCONTRADO + id));

        return copiarLivroComUrlNormalizada(livro);
    }

    public Iterable<Livro> listarTodosLivros() {
        return StreamSupport.stream(livroRepository.findAll().spliterator(), false)
                .map(this::copiarLivroComUrlNormalizada)
                .toList();
    }

    private String montarUrlPublicaUpload(String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            return "";
        }

        return "/uploads/" + nomeArquivo;
    }

    private String normalizarUrlCapa(String urlCapa) {
        if (urlCapa == null || urlCapa.isBlank()) {
            return "";
        }

        String valorNormalizado = urlCapa.trim();

        if (valorNormalizado.startsWith("http://") || valorNormalizado.startsWith("https://")) {
            return valorNormalizado;
        }

        if (valorNormalizado.startsWith("/uploads/")) {
            return valorNormalizado;
        }

        if (valorNormalizado.startsWith("uploads/")) {
            return "/" + valorNormalizado;
        }

        return montarUrlPublicaUpload(valorNormalizado);
    }

    private Livro copiarLivroComUrlNormalizada(Livro livro) {
        return Livro.builder()
                .id(livro.getId())
                .titulo(livro.getTitulo())
                .sinopse(livro.getSinopse())
                .urlCapa(normalizarUrlCapa(livro.getUrlCapa()))
                .anoLancamento(livro.getAnoLancamento())
                .linkCompra(livro.getLinkCompra())
                .build();
    }

}
