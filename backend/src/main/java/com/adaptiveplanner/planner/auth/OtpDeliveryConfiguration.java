package com.adaptiveplanner.planner.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
@EnableConfigurationProperties(OtpEmailProperties.class)
public class OtpDeliveryConfiguration {
    @Bean
    @ConditionalOnProperty(name = "app.email.enabled", havingValue = "true")
    OtpDeliveryService smtpOtpDeliveryService(JavaMailSender mailSender, OtpEmailProperties properties) {
        return (recipient, code) -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.getFrom());
            message.setTo(recipient);
            message.setSubject("Your Adaptive Teaching Planner sign-in code");
            message.setText("Your sign-in code is " + code + ". It expires in 5 minutes. If you did not request it, you can ignore this email.");
            mailSender.send(message);
        };
    }

    @Bean
    @ConditionalOnProperty(name = "app.email.enabled", havingValue = "false", matchIfMissing = true)
    OtpDeliveryService disabledOtpDeliveryService() {
        return (recipient, code) -> { throw new OtpDeliveryUnavailableException(); };
    }
}
