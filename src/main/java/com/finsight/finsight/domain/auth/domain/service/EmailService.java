package com.finsight.finsight.domain.auth.domain.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public String generateVerificationCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    public void sendVerificationEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[FinSight] 이메일 인증번호");
        message.setText("인증번호: " + code + "\n\n3분 내에 입력해주세요.");
        mailSender.send(message);
    }

    /**
     * 학습 알림 이메일 발송 (평문)
     */
    public void sendNotificationEmail(String email, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }

    /**
     * HTML 이메일 발송
     */
    public void sendHtmlEmail(String email, String subject, String htmlContent) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);  // true = HTML 모드
            
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }
}
