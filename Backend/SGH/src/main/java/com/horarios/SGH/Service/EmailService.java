package com.horarios.SGH.Service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final HtmlTemplateService templateService;

    public EmailService(JavaMailSender mailSender, HtmlTemplateService templateService) {
        this.mailSender = mailSender;
        this.templateService = templateService;
    }

    public void sendVerificationEmail(String toEmail, String code) throws Exception {
        String html = templateService.verificationTemplate(code);
        sendHtmlEmail(toEmail, "Código de Verificación - SGH", html);
    }

    public void sendPasswordResetEmail(String toEmail, String code, String userName) throws Exception {
        String html = templateService.passwordResetTemplate(code, userName);
        sendHtmlEmail(toEmail, "Código de Verificación - Restablecimiento de Contraseña - SGH", html);
    }

    public void sendApprovalEmail(String toEmail, String userName) throws Exception {
        String html = templateService.approvalTemplate(userName);
        sendHtmlEmail(toEmail, "¡Registro Aprobado! - Sistema de Gestión de Horarios", html);
    }

    public void sendRejectionEmail(String toEmail, String userName, String reason) throws Exception {
        String html = templateService.rejectionTemplate(userName, reason);
        sendHtmlEmail(toEmail, "Registro No Aprobado - Sistema de Gestión de Horarios", html);
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }
}
