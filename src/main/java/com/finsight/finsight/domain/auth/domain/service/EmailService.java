package com.finsight.finsight.domain.auth.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
     * 학습 알림 이메일 발송
     */
    public void sendNotificationEmail(String email, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }
}