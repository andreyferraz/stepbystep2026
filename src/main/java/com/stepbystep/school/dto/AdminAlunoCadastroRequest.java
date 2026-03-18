package com.stepbystep.school.dto;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class AdminAlunoCadastroRequest {

    private String nome;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataNascimento;

    private UUID turmaId;

    private String nivel;

    private String telefone;

    private String email;

    private String responsavel;

    private String observacoes;

    private String usuarioSenha;

    private String usuarioSenhaConfirmacao;

    private String status;
}
