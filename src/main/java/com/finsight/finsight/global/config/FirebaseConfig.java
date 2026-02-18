package com.finsight.finsight.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.adminsdk.account.path}")
    private String firebaseAccountPath;

    @PostConstruct
    public void initialize() {
        try (InputStream serviceAccount = new FileInputStream(firebaseAccountPath)) {

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // 이미 초기화된 경우 중복 방지
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Firebase 초기화 실패 - 경로 확인: " + firebaseAccountPath, e
            );
        }
    }
}
