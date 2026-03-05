package com.stepbystep.school.util;

import java.time.LocalDate;

public class ValidationUtils {

    private ValidationUtils() {
    }

    /**
     * Valida se um campo string não é nulo nem vazio (incluindo espaços)
     */
    public static void validarCampoObrigatorio(String campo, String nomeCampo) {
        if (campo == null || campo.trim().isEmpty()) {
            throw new IllegalArgumentException(nomeCampo + " é obrigatório.");
        }
    }

    /**
     * Valida se um campo numérico do tipo double não é nulo
     */
    public static void validarCampoObrigatorio(Double campo, String nomeCampo) {
        if (campo == null) {
            throw new IllegalArgumentException(nomeCampo + " é obrigatório.");
        }
    }

    /**
     * Valida se um campo numérico do tipo integer não é nulo
     */
    public static void validarCampoObrigatorio(Integer campo, String nomeCampo) {
        if (campo == null) {
            throw new IllegalArgumentException(nomeCampo + " é obrigatório.");
        }
    }

    /**
     * Valida se um campo booleano não é nulo
     */
    public static void validarCampoObrigatorio(Boolean valor, String nomeCampo) {
        if (valor == null) {
            throw new IllegalArgumentException(nomeCampo + " não pode ser nulo");
        }
    }

    /**
     * Valida se um campo do tipo LocalDate não é nulo
     */
    public static void validarCampoObrigatorio(LocalDate valor, String nomeCampo) {
        if (valor == null) {
            throw new IllegalArgumentException(nomeCampo + " não pode ser nulo");
        }
    }

    /**
     * Valida se um objeto não é nulo
     */
    public static void validarObjetoNaoNulo(Object objeto, String nomeObjeto) {
        if (objeto == null) {
            throw new IllegalArgumentException(nomeObjeto + " não pode ser nulo");
        }
    }

}
