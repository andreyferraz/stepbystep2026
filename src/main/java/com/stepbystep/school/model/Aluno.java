package com.stepbystep.school.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.stepbystep.school.enums.NivelAtual;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "aluno")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Aluno {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String nome;
    private LocalDate dataNascimento;
    private String telefone;
    @Enumerated(EnumType.STRING)
    private NivelAtual nivelAtual;
    @ManyToOne
    private Turma turma;

    @OneToMany(mappedBy = "aluno")
    private List<Nota> notas;

    @OneToMany(mappedBy = "aluno")
    private List<Mensalidade> mensalidades;

}
