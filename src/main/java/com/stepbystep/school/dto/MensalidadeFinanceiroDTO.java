package com.stepbystep.school.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.stepbystep.school.enums.StatusMensalidade;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MensalidadeFinanceiroDTO {

    private UUID id;
    private BigDecimal valor;
    private LocalDate dataVencimento;
    private StatusMensalidade status;
    private boolean paga;
    private LocalDateTime dataPagamento;
}
