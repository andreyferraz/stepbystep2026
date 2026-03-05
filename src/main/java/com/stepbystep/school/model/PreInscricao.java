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
@Table(name = "pre_inscricao")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PreInscricao {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String nomeInteressado;
    private String whatsapp;

    @Column(columnDefinition = "TEXT")
    private String mensagem;

    private LocalDateTime dataLead;
    private boolean respondido;

}
