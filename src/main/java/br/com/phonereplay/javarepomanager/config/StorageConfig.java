package br.com.phonereplay.javarepomanager.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class StorageConfig {

    @Bean
    public Storage storage() throws IOException {
        FileInputStream serviceAccountStream = new FileInputStream("path/to/credentials.json");
        return StorageOptions.newBuilder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                .build()
                .getService();
    }
}
