package com.stepbystep.school.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(columnDefinition = "TEXT")
    private String conteudo;

    private LocalDateTime dataPublicacao;
    private String urlImagemCapa;

}
