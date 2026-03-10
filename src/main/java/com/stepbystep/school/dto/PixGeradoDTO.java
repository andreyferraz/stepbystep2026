package com.stepbystep.school.dto;

import java.util.UUID;

import com.stepbystep.school.enums.StatusMensalidade;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PixGeradoDTO {

    private UUID mensalidadeId;
    private String pixCopiaECola;
    private String qrCodeBase64;
    private StatusMensalidade status;
}
