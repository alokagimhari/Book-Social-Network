package com.tech.BookStore.auth;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine springTemplateEngine;


    @Async
    public void sendEmail(
            String to,
            String username,
            EmailTemplate emailTemplate,
            String confirmationUrl,
            String activationCode,
            String subject
    ) throws MessagingException {
        String templateName;
        if(emailTemplate ==null)
        {
            templateName = "confirm-email";
        }else {
            templateName = emailTemplate.name();
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED,
                UTF_8.name()

        );

        Map<String,Object> properties = new HashMap<>();
        properties.put("username",username);
        properties.put("confirmationUrl",confirmationUrl);
        properties.put("activation_code",activationCode);

        Context context = new Context();
        context.setVariables(properties);

        helper.setFrom("contact@aloka.com");
        helper.setTo(to);
        helper.setSubject(subject);

        String template = springTemplateEngine.process(templateName,context);

        helper.setText(template,true);
        mailSender.send(message);
    }
}
