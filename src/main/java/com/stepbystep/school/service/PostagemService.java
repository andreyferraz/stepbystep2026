package com.stepbystep.school.service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.stepbystep.school.enums.StatusPostagem;
import com.stepbystep.school.model.Postagem;
import com.stepbystep.school.repository.PostagemRepository;
import com.stepbystep.school.util.ValidationUtils;

@Service
public class PostagemService {

    private static final Pattern HTML_TAGS_PATTERN = Pattern.compile("<[^>]*>");
    private static final int TAMANHO_MAXIMO_RESUMO = 220;

    private final PostagemRepository postagemRepository;
    private final FileUploadService fileUploadService;

    public PostagemService(PostagemRepository postagemRepository, FileUploadService fileUploadService) {
        this.postagemRepository = postagemRepository;
        this.fileUploadService = fileUploadService;
    }

    public Postagem criarPostagem(Postagem postagem, MultipartFile imagemCapa) {
        ValidationUtils.validarCampoObrigatorio(postagem, "Postagem");
        ValidationUtils.validarCampoStringObrigatorio(postagem.getTitulo(), "Título da postagem");
        ValidationUtils.validarCampoStringObrigatorio(postagem.getConteudo(), "Conteúdo da postagem");

        if (imagemCapa != null && !imagemCapa.isEmpty()) {
            postagem.setUrlImagemCapa(montarUrlPublicaUpload(fileUploadService.salvarImagem(imagemCapa)));
        }

        postagem.setTitulo(postagem.getTitulo().trim());
        postagem.setConteudo(postagem.getConteudo().trim());
        postagem.setCategoria(normalizarTextoOpcional(postagem.getCategoria()));
        postagem.setAutor(normalizarTextoOpcional(postagem.getAutor()));

        if (postagem.getDataPublicacao() == null) {
            postagem.setDataPublicacao(LocalDateTime.now());
        }

        if (postagem.getStatus() == null) {
            postagem.setStatus(StatusPostagem.RASCUNHO);
        }

        if (postagem.getSlug() == null || postagem.getSlug().isBlank()) {
            postagem.setSlug(gerarSlugUnico(postagem.getTitulo(), null));
        } else {
            postagem.setSlug(garantirSlugUnico(postagem.getSlug(), null));
        }

        postagem.setResumo(resolverResumo(postagem.getResumo(), postagem.getConteudo()));

        return postagemRepository.save(postagem);
    }

    public Postagem editarPostagem(UUID id, Postagem postagem, MultipartFile imagemCapa) {
        ValidationUtils.validarCampoObrigatorio(id, "ID da postagem");
        ValidationUtils.validarCampoObrigatorio(postagem, "Postagem");
        ValidationUtils.validarCampoStringObrigatorio(postagem.getTitulo(), "Título da postagem");
        ValidationUtils.validarCampoStringObrigatorio(postagem.getConteudo(), "Conteúdo da postagem");

        Postagem postagemExistente = postagemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Postagem não encontrada com ID: " + id));

        postagemExistente.setTitulo(postagem.getTitulo().trim());
        postagemExistente.setConteudo(postagem.getConteudo().trim());
        postagemExistente.setCategoria(normalizarTextoOpcional(postagem.getCategoria()));
        postagemExistente.setAutor(normalizarTextoOpcional(postagem.getAutor()));
        postagemExistente.setStatus(postagem.getStatus() == null ? postagemExistente.getStatus() : postagem.getStatus());

        if (postagem.getDataPublicacao() != null) {
            postagemExistente.setDataPublicacao(postagem.getDataPublicacao());
        }

        postagemExistente.setSlug(gerarSlugUnico(postagem.getTitulo(), postagemExistente.getId()));
        postagemExistente.setResumo(resolverResumo(postagem.getResumo(), postagem.getConteudo()));

        if (imagemCapa != null && !imagemCapa.isEmpty()) {
            postagemExistente.setUrlImagemCapa(montarUrlPublicaUpload(fileUploadService.salvarImagem(imagemCapa)));
        }

        return postagemRepository.save(postagemExistente);
    }

    public List<Postagem> listarPostagensPublicadas(String busca, String categoria) {
        List<Postagem> publicadas = postagemRepository.findByStatusOrderByDataPublicacaoDesc(StatusPostagem.PUBLICADO);
        String buscaNormalizada = normalizarTextoOpcional(busca).toLowerCase(Locale.ROOT);
        String categoriaNormalizada = normalizarTextoOpcional(categoria).toLowerCase(Locale.ROOT);

        return publicadas.stream()
            .filter(postagem -> buscaNormalizada.isEmpty() || correspondeBusca(postagem, buscaNormalizada))
            .filter(postagem -> categoriaNormalizada.isEmpty() || categoriaPostagem(postagem).equals(categoriaNormalizada))
            .toList();
    }

    public List<Postagem> listarPostagensAdmin() {
        return postagemRepository.findAllByOrderByDataPublicacaoDesc();
    }

    public Optional<Postagem> buscarPostagemPublicadaPorSlug(String slug) {
        ValidationUtils.validarCampoStringObrigatorio(slug, "Slug da postagem");
        return postagemRepository.findBySlugAndStatus(slug.trim(), StatusPostagem.PUBLICADO);
    }

    public String salvarImagemConteudo(MultipartFile imagem) {
        ValidationUtils.validarCampoObrigatorio(imagem, "Imagem");
        if (imagem.isEmpty()) {
            throw new IllegalArgumentException("A imagem enviada está vazia.");
        }

        String nomeArquivo = fileUploadService.salvarImagem(imagem);
        return montarUrlPublicaUpload(nomeArquivo);
    }

    public void excluirPostagem(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, "ID da postagem");
        if (!postagemRepository.existsById(id)) {
            throw new IllegalArgumentException("Postagem não encontrada com ID: " + id);
        }
        postagemRepository.deleteById(id);
    }

    private boolean correspondeBusca(Postagem postagem, String buscaNormalizada) {
        return textoSeguro(postagem.getTitulo()).toLowerCase(Locale.ROOT).contains(buscaNormalizada)
            || textoSeguro(postagem.getResumo()).toLowerCase(Locale.ROOT).contains(buscaNormalizada)
            || textoSeguro(postagem.getConteudo()).toLowerCase(Locale.ROOT).contains(buscaNormalizada);
    }

    private String categoriaPostagem(Postagem postagem) {
        return normalizarTextoOpcional(postagem.getCategoria()).toLowerCase(Locale.ROOT);
    }

    private String resolverResumo(String resumoInformado, String conteudoHtml) {
        String resumoNormalizado = normalizarTextoOpcional(resumoInformado);
        if (!resumoNormalizado.isEmpty()) {
            return limitarTexto(resumoNormalizado, TAMANHO_MAXIMO_RESUMO);
        }

        String conteudoSemHtml = removerTagsHtml(conteudoHtml);
        return limitarTexto(conteudoSemHtml, TAMANHO_MAXIMO_RESUMO);
    }

    private String removerTagsHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return HTML_TAGS_PATTERN.matcher(html).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }

    private String limitarTexto(String texto, int tamanhoMaximo) {
        String textoNormalizado = normalizarTextoOpcional(texto);
        if (textoNormalizado.length() <= tamanhoMaximo) {
            return textoNormalizado;
        }
        return textoNormalizado.substring(0, tamanhoMaximo).trim() + "...";
    }

    private String montarUrlPublicaUpload(String nomeArquivo) {
        return "/uploads/" + nomeArquivo;
    }

    private String gerarSlugUnico(String titulo, UUID idAtual) {
        String base = slugify(titulo);
        String candidato = base;
        int sufixo = 2;

        while (slugJaExiste(candidato, idAtual)) {
            candidato = base + "-" + sufixo;
            sufixo++;
        }

        return candidato;
    }

    private String garantirSlugUnico(String slug, UUID idAtual) {
        return gerarSlugUnico(slug.replace('-', ' '), idAtual);
    }

    private boolean slugJaExiste(String slug, UUID idAtual) {
        if (idAtual == null) {
            return postagemRepository.existsBySlug(slug);
        }
        return postagemRepository.existsBySlugAndIdNot(slug, idAtual);
    }

    private String slugify(String texto) {
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9\\s-]", "")
            .trim()
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-");

        if (normalizado.isBlank()) {
            return "postagem";
        }

        return normalizado;
    }

    private String normalizarTextoOpcional(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private String textoSeguro(String texto) {
        return texto == null ? "" : texto;
    }

}
