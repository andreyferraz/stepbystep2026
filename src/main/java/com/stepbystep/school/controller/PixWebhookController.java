package com.stepbystep.school.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stepbystep.school.service.MensalidadeService;

@RestController
@RequestMapping("/api/webhooks/mercadopago")
public class PixWebhookController {

    private final MensalidadeService mensalidadeService;

    @Value("${mercadopago.webhook.token:}")
    private String webhookTokenConfigurado;

    public PixWebhookController(MensalidadeService mensalidadeService) {
        this.mensalidadeService = mensalidadeService;
    }

    @PostMapping("/pix")
    public ResponseEntity<Map<String, Object>> receberWebhookPix(
        @RequestBody Map<String, Object> payload,
        @RequestHeader(name = "X-Webhook-Token", required = false) String webhookToken
    ) {
        if (tokenConfigurado() && !tokenValido(webhookToken)) {
            return ResponseEntity.status(401).body(Map.of("message", "Webhook token inválido."));
        }

        List<String> txids = extrairIdentificadoresPagamento(payload);
        int confirmados = 0;
        List<String> naoEncontrados = new ArrayList<>();

        for (String txid : txids) {
            try {
                mensalidadeService.confirmarPagamentoPorTxid(txid, LocalDateTime.now());
                confirmados++;
            } catch (IllegalArgumentException ex) {
                naoEncontrados.add(txid);
            }
        }

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("receivedIds", txids.size());
        resposta.put("confirmedPayments", confirmados);
        resposta.put("notFoundIds", naoEncontrados);
        resposta.put("status", "processed");

        return ResponseEntity.ok(resposta);
    }

    private boolean tokenConfigurado() {
        return webhookTokenConfigurado != null && !webhookTokenConfigurado.isBlank();
    }

    private boolean tokenValido(String tokenRecebido) {
        if (tokenRecebido == null) {
            return false;
        }

        return webhookTokenConfigurado.trim().equals(tokenRecebido.trim());
    }

    private List<String> extrairIdentificadoresPagamento(Map<String, Object> payload) {
        List<String> txids = new ArrayList<>();
        if (payload == null || payload.isEmpty()) {
            return txids;
        }

        adicionarIdMercadoPago(payload, txids);
        adicionarTxidDireto(payload, txids);
        adicionarTxidsListaPix(payload, txids);

        return txids.stream().distinct().toList();
    }

    @SuppressWarnings("unchecked")
    private void adicionarIdMercadoPago(Map<String, Object> payload, List<String> ids) {
        Object data = payload.get("data");
        if (!(data instanceof Map<?, ?> dataMap)) {
            return;
        }

        Object id = ((Map<String, Object>) dataMap).get("id");
        adicionarSeValido(id == null ? null : String.valueOf(id), ids);
    }

    private void adicionarTxidDireto(Map<String, Object> payload, List<String> ids) {
        Object txidDireto = payload.get("txid");
        if (txidDireto instanceof String txidStr) {
            adicionarSeValido(txidStr, ids);
        }
    }

    @SuppressWarnings("unchecked")
    private void adicionarTxidsListaPix(Map<String, Object> payload, List<String> ids) {
        Object pix = payload.get("pix");
        if (!(pix instanceof List<?> pixList)) {
            return;
        }

        for (Object item : pixList) {
            if (!(item instanceof Map<?, ?> itemMap)) {
                continue;
            }
            Object txid = ((Map<String, Object>) itemMap).get("txid");
            if (txid instanceof String txidStr) {
                adicionarSeValido(txidStr, ids);
            }
        }
    }

    private void adicionarSeValido(String valor, List<String> ids) {
        if (valor == null) {
            return;
        }

        String normalizado = valor.trim();
        if (!normalizado.isBlank()) {
            ids.add(normalizado.toUpperCase(Locale.ROOT));
        }
    }
}
