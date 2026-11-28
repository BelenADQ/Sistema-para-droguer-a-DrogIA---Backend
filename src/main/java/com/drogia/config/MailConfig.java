package com.drogia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/** Configuración SMTP reutilizando la cuenta YAML ya existente de Gmail. */
@Configuration
public class MailConfig {

    @Value("${gmail.user}")
    private String usuario;

    @Value("${gmail.app-password}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("smtp.gmail.com");
        sender.setPort(587);
        sender.setProtocol("smtp");
        sender.setUsername(usuario);
        sender.setPassword(password);
        sender.setJavaMailProperties(propiedades());
        return sender;
    }

    private Properties propiedades() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        return props;
    }
}
