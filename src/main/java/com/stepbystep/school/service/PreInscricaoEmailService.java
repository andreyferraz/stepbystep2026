package com.stepbystep.school.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.stepbystep.school.model.PreInscricao;
import com.stepbystep.school.util.ValidationUtils;

@Service
public class PreInscricaoEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.preinscricao.email.destinatario:}")
    private String destinatarioEmail;

    @Value("${app.preinscricao.email.remetente:}")
    private String remetenteEmail;

    @Value("${app.preinscricao.email.assunto:Nova pre-inscricao recebida - Step By Step}")
    private String assuntoEmail;

    public PreInscricaoEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarNotificacaoNovaPreInscricao(PreInscricao preInscricao, String nivel, String duvidas) {
        ValidationUtils.validarCampoObrigatorio(preInscricao, "Pré-inscrição");
        ValidationUtils.validarCampoStringObrigatorio(destinatarioEmail, "E-mail destinatário de pré-inscrição");

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(destinatarioEmail.trim());
        mensagem.setSubject(assuntoEmail == null || assuntoEmail.isBlank()
            ? "Nova pre-inscricao recebida - Step By Step"
            : assuntoEmail.trim());

        if (remetenteEmail != null && !remetenteEmail.isBlank()) {
            mensagem.setFrom(remetenteEmail.trim());
        }

        mensagem.setText(montarCorpoEmail(preInscricao, nivel, duvidas));
        mailSender.send(mensagem);
    }

    private String montarCorpoEmail(PreInscricao preInscricao, String nivel, String duvidas) {
        String nivelNormalizado = nivel == null || nivel.isBlank() ? "Nao informado" : nivel.trim();
        String duvidasNormalizadas = duvidas == null || duvidas.isBlank() ? "Sem observacoes" : duvidas.trim();

        StringBuilder corpo = new StringBuilder();
        corpo.append("Nova pre-inscricao recebida pelo portal.\n\n");
        corpo.append("Nome: ").append(preInscricao.getNomeInteressado()).append("\n");
        corpo.append("WhatsApp: ").append(preInscricao.getWhatsapp()).append("\n");
        corpo.append("Nivel de ingles: ").append(nivelNormalizado).append("\n");
        corpo.append("Duvidas/Objetivos: ").append(duvidasNormalizadas).append("\n\n");
        corpo.append("Data de envio: ").append(preInscricao.getDataLead()).append("\n");
        corpo.append("ID da pre-inscricao: ").append(preInscricao.getId());
        return corpo.toString();
    }
}
