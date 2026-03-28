# Guia Completo: GitHub Secrets para Pre-Inscricao por E-mail

## Objetivo
Este documento mostra como armazenar credenciais de e-mail com seguranca no GitHub e como o projeto recupera esses valores em execucao, sem versionar senha no repositório.

## Resumo da Arquitetura
1. O formulario de pre-inscricao envia dados para o endpoint `/pre-inscricao`.
2. O backend salva a pre-inscricao no banco.
3. O servico de e-mail envia notificacao para o destinatario configurado.
4. Credenciais SMTP nao ficam no codigo: entram por variaveis de ambiente.
5. No GitHub, as variaveis sensiveis sao armazenadas em Actions Secrets.

## Por que nao salvar senha no GitHub
- Repositorio e historico de commits podem expor credenciais.
- Mesmo em repositorio privado, o risco operacional continua alto.
- A rotacao de segredo fica mais dificil quando valor esta hardcoded.

A pratica recomendada e:
- Versionar apenas configuracoes publicas (host, porta, flags).
- Manter usuario e senha SMTP em Secrets.

## Variaveis usadas pelo projeto
O projeto le as seguintes variaveis:

- `SPRING_MAIL_HOST` (padrao: `smtp.office365.com`)
- `SPRING_MAIL_PORT` (padrao: `587`)
- `SPRING_MAIL_USERNAME` (obrigatoria para envio real)
- `SPRING_MAIL_PASSWORD` (obrigatoria para envio real)
- `PREINSCRICAO_EMAIL_DESTINATARIO` (padrao: `revistadeathmetal@hotmail.com`)
- `PREINSCRICAO_EMAIL_REMETENTE` (opcional, default usa `SPRING_MAIL_USERNAME`)
- `PREINSCRICAO_EMAIL_ASSUNTO` (opcional)

## Como configurar no GitHub
### Passo 1: abrir Secrets
1. Abra o repositorio no GitHub.
2. Entre em `Settings`.
3. Clique em `Secrets and variables`.
4. Clique em `Actions`.
5. Clique em `New repository secret`.

### Passo 2: criar os secrets
Crie os secrets abaixo:

1. Nome: `SPRING_MAIL_USERNAME`
   Valor: seu login de e-mail (ex.: conta Outlook/Hotmail)
2. Nome: `SPRING_MAIL_PASSWORD`
   Valor: senha da conta (ou app password, se aplicavel)
3. Nome: `PREINSCRICAO_EMAIL_DESTINATARIO`
   Valor: `revistadeathmetal@hotmail.com`
4. Nome: `PREINSCRICAO_EMAIL_REMETENTE` (opcional)
   Valor: remetente desejado

## Workflow criado
Arquivo: `.github/workflows/ci.yml`

O workflow faz:
1. Checkout do codigo.
2. Setup do Java 17.
3. Cache Maven.
4. Injeta secrets como variaveis de ambiente no job.
5. Em branch `main`, valida se os secrets obrigatorios existem.
6. Executa compilacao Maven.

## Como o backend recupera os valores
- O Spring Boot resolve variaveis `${NOME_DA_VARIAVEL}` em runtime.
- Em `application.properties`, os campos de e-mail estao parametrizados.
- O servico `PreInscricaoEmailService` usa `@Value` para ler configuracoes.

Fluxo prático:
1. GitHub injeta secrets no ambiente do workflow.
2. Spring le environment variables.
3. `JavaMailSender` usa as propriedades SMTP.
4. O e-mail de pre-inscricao e enviado ao destinatario configurado.

## Teste local (sem GitHub)
No macOS/Linux, exporte antes de subir a app:

```bash
export SPRING_MAIL_USERNAME="seu_email@hotmail.com"
export SPRING_MAIL_PASSWORD="sua_senha"
export PREINSCRICAO_EMAIL_DESTINATARIO="revistadeathmetal@hotmail.com"
./mvnw spring-boot:run
```

Depois:
1. Abra a pagina inicial.
2. Envie o formulario de pre-inscricao.
3. Verifique caixa de entrada do destinatario.

## Teste no GitHub Actions
1. Faça push na branch principal ou execute manualmente (`Run workflow`).
2. Abra a aba `Actions`.
3. Confira o workflow `CI`.
4. Se faltar segredo em `main`, o passo de validacao falha com mensagem clara.

## Boas praticas de seguranca
1. Nunca commitar senha em arquivos versionados.
2. Rotacionar secrets periodicamente.
3. Revogar credenciais imediatamente em caso de vazamento.
4. Usar conta de e-mail dedicada para automacao (recomendado).
5. Restringir permissao de quem pode alterar secrets no repositorio.

## Troubleshooting
### Erro de autenticacao SMTP
- Verifique `SPRING_MAIL_USERNAME` e `SPRING_MAIL_PASSWORD`.
- Confirme se o provedor aceita SMTP externo.
- Se houver MFA, avalie uso de senha de aplicativo.

### Timeout de envio
- Verifique rede/firewall para `smtp.office365.com:587`.
- Ajuste timeouts SMTP se necessario.

### Workflow falha na validacao de secrets
- Revise nomes exatos dos secrets no GitHub.
- Confirme que foram criados no repositorio correto.

### Formulario salva mas nao envia e-mail
- O backend registra pre-inscricao e informa erro de envio em flash message.
- Verifique logs da aplicacao para excecao de mail.

## Checklist final
1. Secrets criados no GitHub.
2. Workflow `CI` executando com sucesso.
3. Formulario enviando para `/pre-inscricao`.
4. Pre-inscricao persistida no banco.
5. E-mail recebido em `revistadeathmetal@hotmail.com`.
