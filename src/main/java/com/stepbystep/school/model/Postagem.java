package com.stepbystep.school.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.stepbystep.school.enums.StatusPostagem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "blog_postagem")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Postagem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String titulo;

    @Column(unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String resumo;

    private String categoria;
    private String autor;

    @Enumerated(EnumType.STRING)
    private StatusPostagem status;

    @Column(columnDefinition = "TEXT")
    private String conteudo;

    private LocalDateTime dataPublicacao;
    private String urlImagemCapa;

}
