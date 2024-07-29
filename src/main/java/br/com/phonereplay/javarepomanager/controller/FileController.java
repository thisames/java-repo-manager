package br.com.phonereplay.javarepomanager.controller;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@RestController
@RequestMapping("/api")
public class FileController {

    @Value("${gcs.bucket.name}")
    private String bucketName;

    @Value("${encryption.key}")
    private String encryptionKey;

    private final Storage storage;

    public FileController(Storage storage) {
        this.storage = storage;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("aar") MultipartFile file,
                                             @RequestParam("groupID") String groupId,
                                             @RequestParam("artifactID") String artifactId,
                                             @RequestParam("version") String version) throws IOException {
        String objectName = String.format("%s/%s/%s/%s-%s.aar",
                groupId.replace('.', '/'), artifactId, version, artifactId, version);

        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/octet-stream").build();
        storage.create(blobInfo, file.getBytes());

        return ResponseEntity.ok("File uploaded successfully!");
    }

    @GetMapping("/download/{groupId}/{artifactId}/{version}/{fileName}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String groupId,
                                               @PathVariable String artifactId,
                                               @PathVariable String version,
                                               @PathVariable String fileName) {
        String objectName = String.format("%s/%s/%s/%s",
                groupId.replace('.', '/'), artifactId, version, fileName);

        Blob blob = storage.get(BlobId.of(bucketName, objectName));
        if (blob == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        byte[] content = blob.getContent();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(content);
    }

    private static byte[] decrypt(byte[] encryptedData, String key) throws GeneralSecurityException {
        SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(key), "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, encryptedData, 0, 12);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        return cipher.doFinal(encryptedData, 12, encryptedData.length - 12);
    }
}
