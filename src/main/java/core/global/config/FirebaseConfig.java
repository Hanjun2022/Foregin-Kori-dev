package core.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials.json-string}")
    private String firebaseCredentialsJsonString;

    /**
     * 1. FirebaseApp 객체를 스프링 빈으로 등록하는 부분
     */
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        List<FirebaseApp> firebaseApps = FirebaseApp.getApps();
        if (firebaseApps != null && !firebaseApps.isEmpty()) {
            return firebaseApps.get(0);
        }

        InputStream serviceAccount = new ByteArrayInputStream(firebaseCredentialsJsonString.getBytes());

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
        return FirebaseApp.initializeApp(options);
    }

    /**
     * 2. FirebaseMessaging 객체를 스프링 빈으로 등록하는 부분
     */
    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}