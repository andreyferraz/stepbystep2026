package com.stepbystep.school.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.stepbystep.school.enums.StatusMensalidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "mensalidade")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Mensalidade {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private BigDecimal valor;
    private LocalDate dataVencimento;

    @Enumerated(EnumType.STRING)
    private StatusMensalidade status;

    private LocalDateTime dataPagamento;

    @Column(columnDefinition = "TEXT")
    private String pixCopiaECola;

    @ManyToOne
    private Aluno aluno;

}
