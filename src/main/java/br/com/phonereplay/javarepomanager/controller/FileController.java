package br.com.phonereplay.javarepomanager.controller;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;

@RestController
@RequestMapping("/api")
public class FileController {

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.accessKey}")
    private String minioAccessKey;

    @Value("${minio.secretKey}")
    private String minioSecretKey;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Value("${encryption.key}")
    private String encryptionKey;

    private MinioClient minioClient;

    public FileController() {
        minioClient = MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(minioAccessKey, minioSecretKey)
                .build();
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("aar") MultipartFile file,
                                             @RequestParam("groupID") String groupId,
                                             @RequestParam("artifactID") String artifactId,
                                             @RequestParam("version") String version) {
        try {
            String objectName = String.format("%s/%s/%s/%s-%s.aar",
                    groupId.replace('.', '/'), artifactId, version, artifactId, version);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return ResponseEntity.ok("File uploaded successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error uploading file");
        }
    }

    @GetMapping("/download/{groupId}/{artifactId}/{version}/{fileName}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String groupId,
                                               @PathVariable String artifactId,
                                               @PathVariable String version,
                                               @PathVariable String fileName) {
        try {
            String objectName = String.format("%s/%s/%s/%s",
                    groupId.replace('.', '/'), artifactId, version, fileName);

            byte[] content = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            ).readAllBytes();

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(content);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    private static byte[] decrypt(byte[] encryptedData, String key) throws GeneralSecurityException {
        SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(key), "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, encryptedData, 0, 12);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        return cipher.doFinal(encryptedData, 12, encryptedData.length - 12);
    }
}
