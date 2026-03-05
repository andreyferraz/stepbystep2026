package com.stepbystep.school.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "material_estudo")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MaterialEstudo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String titulo;
    private String descricao;
    private String urlArquivo;
    private LocalDateTime dataUpload;

    @ManyToOne
    private Turma turma;

}
