package com.stepbystep.school.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stepbystep.school.dto.MensalidadeFinanceiroDTO;
import com.stepbystep.school.dto.PixGeradoDTO;
import com.stepbystep.school.enums.StatusMensalidade;
import com.stepbystep.school.model.Mensalidade;
import com.stepbystep.school.service.MensalidadeService;

@RestController
@RequestMapping("/api/alunos/{alunoId}/mensalidades")
public class MensalidadeController {

    private final MensalidadeService mensalidadeService;

    public MensalidadeController(MensalidadeService mensalidadeService) {
        this.mensalidadeService = mensalidadeService;
    }

    @GetMapping
    public List<MensalidadeFinanceiroDTO> listarMensalidades(@PathVariable UUID alunoId) {
        return mensalidadeService.listarMensalidadesPorAluno(alunoId)
                .stream()
                .map(this::toFinanceiroDto)
                .toList();
    }

    @PostMapping("/{mensalidadeId}/gerar-pix")
    public PixGeradoDTO gerarPix(@PathVariable UUID alunoId, @PathVariable UUID mensalidadeId) {
        Mensalidade mensalidade = mensalidadeService.gerarPix(alunoId, mensalidadeId);
        return PixGeradoDTO.builder()
                .mensalidadeId(mensalidade.getId())
                .status(mensalidade.getStatus())
                .pixCopiaECola(mensalidade.getPixCopiaECola())
                .qrCodeBase64(mensalidadeService.gerarQrCodeBase64(mensalidade.getPixCopiaECola()))
                .build();
    }

    @PostMapping("/{mensalidadeId}/confirmar-pagamento")
    public MensalidadeFinanceiroDTO confirmarPagamento(@PathVariable UUID alunoId, @PathVariable UUID mensalidadeId) {
        Mensalidade mensalidadePaga = mensalidadeService.confirmarPagamento(alunoId, mensalidadeId);
        return toFinanceiroDto(mensalidadePaga);
    }

    private MensalidadeFinanceiroDTO toFinanceiroDto(Mensalidade mensalidade) {
        return MensalidadeFinanceiroDTO.builder()
                .id(mensalidade.getId())
                .valor(mensalidade.getValor())
                .dataVencimento(mensalidade.getDataVencimento())
                .status(mensalidade.getStatus())
                .paga(mensalidade.getStatus() == StatusMensalidade.PAGO)
                .dataPagamento(mensalidade.getDataPagamento())
                .build();
    }
}
