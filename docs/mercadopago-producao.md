# Mercado Pago em Produção

Este documento descreve o passo a passo para promover a integração de cobrança Pix do Mercado Pago de ambiente de teste para produção no projeto Step By Step.

## 1. Pré-requisitos

1. Conta Mercado Pago com acesso a credenciais de produção.
2. Aplicação publicada com HTTPS válido.
3. Acesso ao ambiente de deploy para configurar variáveis de ambiente seguras.
4. Logs da aplicação habilitados para diagnóstico.

## 2. Segurança antes do go-live

1. Nunca versionar Access Token no repositório.
2. Armazenar segredos apenas no gerenciador de segredo da plataforma de deploy.
3. Rotacionar imediatamente qualquer token já exposto em conversas, logs, prints ou commits.
4. Confirmar que somente o backend conhece o Access Token.

## 3. Configuração de produção (variáveis de ambiente)

Defina as variáveis abaixo no ambiente de produção:

- MERCADOPAGO_API_ENABLED=true
- MERCADOPAGO_API_TOKEN=SEU_TOKEN_DE_PRODUCAO
- MERCADOPAGO_API_BASE_URL=https://api.mercadopago.com
- MERCADOPAGO_API_TIMEOUT_SECONDS=20
- MERCADOPAGO_API_PAYER_EMAIL=financeiro@suaempresa.com.br
- MERCADOPAGO_API_STATEMENT_DESCRIPTOR=STEPBYSTEP
- MERCADOPAGO_API_NOTIFICATION_URL=https://seu-dominio.com/api/webhooks/mercadopago/pix
- MERCADOPAGO_WEBHOOK_TOKEN=UM_TOKEN_FORTE_GERADO_POR_VOCE
- MERCADOPAGO_API_ALLOW_LIVE_TOKEN=true

Observações:

1. O projeto bloqueia token live por padrão, por isso em produção é necessário MERCADOPAGO_API_ALLOW_LIVE_TOKEN=true.
2. Em homologação e desenvolvimento, mantenha MERCADOPAGO_API_ALLOW_LIVE_TOKEN=false.

## 4. Ajustes de propriedades relevantes no projeto

As propriedades estão mapeadas em [src/main/resources/application.properties](src/main/resources/application.properties).

A lógica de integração com Mercado Pago está em [src/main/java/com/stepbystep/school/service/MercadoPagoPixApiService.java](src/main/java/com/stepbystep/school/service/MercadoPagoPixApiService.java).

O endpoint de webhook está em [src/main/java/com/stepbystep/school/controller/PixWebhookController.java](src/main/java/com/stepbystep/school/controller/PixWebhookController.java).

As exceções de segurança para webhook estão em [src/main/java/com/stepbystep/school/config/SecurityConfig.java](src/main/java/com/stepbystep/school/config/SecurityConfig.java).

## 5. Configurar webhook no Mercado Pago

1. No painel do Mercado Pago, configure a URL de notificação:
   https://seu-dominio.com/api/webhooks/mercadopago/pix
2. Configure o mesmo token de proteção usado na variável MERCADOPAGO_WEBHOOK_TOKEN.
3. Garanta que a URL esteja publicamente acessível e com TLS válido.
4. Valide que o endpoint responde 200 para payloads válidos.

## 6. Publicação e validação funcional

1. Fazer deploy em produção com as variáveis definidas.
2. Reiniciar a aplicação.
3. Gerar uma cobrança Pix no painel admin.
4. Confirmar:
   - QR Code gerado
   - Chave copia e cola retornada
   - Registro de txid salvo
5. Efetuar pagamento real controlado e validar se o webhook marca a mensalidade como PAGO.

## 7. Checklist de go-live

1. Token de produção armazenado em segredo (não em arquivo).
2. MERCADOPAGO_API_ENABLED=true em produção.
3. MERCADOPAGO_API_ALLOW_LIVE_TOKEN=true em produção.
4. URL de webhook apontando para domínio final com HTTPS.
5. MERCADOPAGO_WEBHOOK_TOKEN configurado e validado.
6. Teste ponta a ponta concluído com sucesso.
7. Monitoramento de erros HTTP 4xx e 5xx habilitado.

## 8. Troubleshooting rápido

### Erro 401 Unauthorized use of live credentials

Possíveis causas:

1. Token inválido ou expirado.
2. Mistura de credencial de teste com contexto de produção.
3. Token live usado com MERCADOPAGO_API_ALLOW_LIVE_TOKEN=false.

Ações:

1. Validar token no painel do Mercado Pago.
2. Confirmar variável de ambiente ativa no servidor.
3. Conferir logs da aplicação para mensagem detalhada de validação.

### Webhook não confirma pagamento

Possíveis causas:

1. URL incorreta no painel.
2. Token de webhook divergente.
3. Firewall ou proxy bloqueando chamada.

Ações:

1. Verificar se a rota está pública e com HTTPS.
2. Confirmar token recebido no header X-Webhook-Token.
3. Revisar logs do endpoint de webhook.

## 9. Boas práticas contínuas

1. Rotacionar Access Token periodicamente.
2. Criar alerta para picos de falhas na criação de cobrança.
3. Registrar auditoria de geração de cobrança e confirmação de pagamento.
4. Manter ambiente de homologação separado de produção com segredos distintos.
