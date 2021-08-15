package com.ariseontech.joindesk.event.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {
    @Autowired
    public JavaMailSender emailSender;
    @Value("${spring.mail.username}")
    private String fromAddress;
    @Value("${spring.application.name}")
    private String appName;
    @Value("${app.email.enabled}")
    private boolean emailEnabled;

    public void sendTextMail(String to, String subject, String text) {
        if (!emailEnabled) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(appName + "<" + fromAddress + " > ");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        send(message);
    }

    public void sendTextMail(String to, String subject, String text, List<String> cc, List<String> bcc) {
        if (!emailEnabled) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(appName + "<" + fromAddress + " > ");
        message.setTo(to);
        message.setCc(String.valueOf(cc));
        message.setBcc(String.valueOf(bcc));
        message.setSubject(subject);
        message.setText(text);
        send(message);
    }

    private void send(SimpleMailMessage message) {
        if (!emailEnabled) return;
        emailSender.send(message);
    }
}

