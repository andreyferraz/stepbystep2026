package com.stepbystep.school.model;

import java.time.LocalDate;
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
@Table(name = "nota")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Nota {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private Double valor;
    private Integer bimestre;
    private String atividade;
    private String presenca;
    private LocalDate dataReferencia;
    private String descricao;

    @ManyToOne
    private Aluno aluno;

}
