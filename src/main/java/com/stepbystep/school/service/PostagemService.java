package com.stepbystep.school.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.stepbystep.school.model.Postagem;
import com.stepbystep.school.repository.PostagemRepository;
import com.stepbystep.school.util.ValidationUtils;

@Service
public class PostagemService {

    private final PostagemRepository postagemRepository;

    public PostagemService(PostagemRepository postagemRepository) {
        this.postagemRepository = postagemRepository;
    }

    public Postagem criarPostagem(Postagem postagem) {
        ValidationUtils.validarCampoObrigatorio(postagem, "Postagem");
        ValidationUtils.validarCampoStringObrigatorio(postagem.getTitulo(), "Título da postagem");
        ValidationUtils.validarCampoStringObrigatorio(postagem.getConteudo(), "Conteúdo da postagem");

        if (postagem.getDataPublicacao() == null) {
            postagem.setDataPublicacao(LocalDateTime.now());
        }

        return postagemRepository.save(postagem);
    }

    public Postagem editarPostagem(UUID id, Postagem postagem) {
        ValidationUtils.validarCampoObrigatorio(id, "ID da postagem");
        ValidationUtils.validarCampoObrigatorio(postagem, "Postagem");
        ValidationUtils.validarCampoStringObrigatorio(postagem.getTitulo(), "Título da postagem");
        ValidationUtils.validarCampoStringObrigatorio(postagem.getConteudo(), "Conteúdo da postagem");

        Postagem postagemExistente = postagemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Postagem não encontrada com ID: " + id));

        postagemExistente.setTitulo(postagem.getTitulo());
        postagemExistente.setConteudo(postagem.getConteudo());
        if (postagem.getUrlImagemCapa() != null && !postagem.getUrlImagemCapa().isBlank()) {
            postagemExistente.setUrlImagemCapa(postagem.getUrlImagemCapa());
        }

        return postagemRepository.save(postagemExistente);
    }

    public void excluirPostagem(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, "ID da postagem");
        if (!postagemRepository.existsById(id)) {
            throw new IllegalArgumentException("Postagem não encontrada com ID: " + id);
        }
        postagemRepository.deleteById(id);
    }

}
