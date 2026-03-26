package com.stepbystep.school.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stepbystep.school.model.Mensalidade;

@Service
public class MercadoPagoPixApiService {

    private static final DateTimeFormatter DATA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${mercadopago.api.base-url:https://api.mercadopago.com}")
    private String baseUrl;

    @Value("${mercadopago.api.token:}")
    private String accessToken;

    @Value("${mercadopago.api.timeout-seconds:20}")
    private int timeoutSeconds;

    @Value("${mercadopago.api.payer.email:pagador@stepbystep.local}")
    private String payerEmail;

    @Value("${mercadopago.api.statement-descriptor:STEPBYSTEP}")
    private String statementDescriptor;

    @Value("${mercadopago.api.notification-url:}")
    private String notificationUrl;

    public DynamicPixPayload criarCobrancaPix(Mensalidade mensalidade, String txid) {
        validarConfiguracao();

        String idempotencyKey = UUID.randomUUID().toString();
        String body = construirBodyPagamento(mensalidade, txid);

        JsonNode response = executarRequest(
            "POST",
            "/v1/payments",
            body,
            idempotencyKey
        );

        String paymentId = texto(response, "id");
        JsonNode poi = response.path("point_of_interaction");
        JsonNode transactionData = poi.path("transaction_data");

        String qrCode = texto(transactionData, "qr_code");
        if (qrCode == null || qrCode.isBlank()) {
            throw new IllegalStateException("Mercado Pago não retornou o código PIX copia e cola.");
        }

        String txidFinal = paymentId == null || paymentId.isBlank()
            ? txid.trim().toUpperCase(Locale.ROOT)
            : paymentId.trim().toUpperCase(Locale.ROOT);

        return new DynamicPixPayload(txidFinal, qrCode);
    }

    private void validarConfiguracao() {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("Configure mercadopago.api.token para gerar cobrança PIX dinâmica.");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("Configure mercadopago.api.base-url para gerar cobrança PIX dinâmica.");
        }
    }

    private String construirBodyPagamento(Mensalidade mensalidade, String txid) {
        BigDecimal valor = mensalidade.getValor() == null ? BigDecimal.ZERO : mensalidade.getValor();
        LocalDate vencimento = mensalidade.getDataVencimento();

        ObjectNode root = objectMapper.createObjectNode();
        root.put("transaction_amount", valor.setScale(2, RoundingMode.HALF_UP).doubleValue());
        root.put("payment_method_id", "pix");
        root.put("description", descricaoCobranca(vencimento));
        root.put("external_reference", mensalidade.getId() == null ? txid : mensalidade.getId().toString());
        root.put("statement_descriptor", limitar(statementDescriptor, 22));

        if (notificationUrl != null && !notificationUrl.isBlank()) {
            root.put("notification_url", notificationUrl.trim());
        }

        ObjectNode payer = root.putObject("payer");
        payer.put("email", payerEmail == null || payerEmail.isBlank() ? "pagador@stepbystep.local" : payerEmail.trim());

        try {
            return objectMapper.writeValueAsString(root);
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível serializar cobrança Pix do Mercado Pago.", ex);
        }
    }

    private String descricaoCobranca(LocalDate vencimento) {
        String base = "Mensalidade StepByStep";
        if (vencimento == null) {
            return base;
        }
        return base + " venc. " + vencimento.format(DATA_FMT);
    }

    private JsonNode executarRequest(String metodo, String path, String body, String idempotencyKey) {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Authorization", "Bearer " + accessToken.trim())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");

            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                builder.header("X-Idempotency-Key", idempotencyKey);
            }

            if ("POST".equalsIgnoreCase(metodo) || "PUT".equalsIgnoreCase(metodo)) {
                builder.method(metodo.toUpperCase(Locale.ROOT), HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
            } else {
                builder.method(metodo.toUpperCase(Locale.ROOT), HttpRequest.BodyPublishers.noBody());
            }

            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String responseBody = response.body() == null ? "" : response.body();

            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Erro Mercado Pago API (HTTP " + status + "): " + responseBody);
            }

            return objectMapper.readTree(responseBody);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Falha ao criar cobrança PIX no Mercado Pago.", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao criar cobrança PIX no Mercado Pago.", ex);
        }
    }

    private String texto(JsonNode root, String campo) {
        if (root == null || campo == null || campo.isBlank()) {
            return null;
        }
        JsonNode node = root.get(campo);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    private String limitar(String texto, int limite) {
        if (texto == null) {
            return "";
        }
        String valor = texto.trim();
        if (valor.length() <= limite) {
            return valor;
        }
        return valor.substring(0, limite);
    }

    public record DynamicPixPayload(String txid, String pixCopiaECola) {
    }
}
