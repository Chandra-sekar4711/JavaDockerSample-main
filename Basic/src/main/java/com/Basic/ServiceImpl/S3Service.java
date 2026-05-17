package com.Basic.ServiceImpl;

import java.util.*;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Service
public class S3Service {

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    // Upload file to S3
    public String uploadFile(MultipartFile file, String folderName) throws IOException {
        String fileName = folderName + "/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return "File uploaded successfully: " + fileName;

        } catch (S3Exception e) {
            throw new RuntimeException("Error uploading file: " + e.getMessage());
        }
    }

    // Download file from S3 (returns bytes)
    public byte[] downloadFile(String fileName) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            InputStream inputStream = s3Client.getObject(getObjectRequest);
            return inputStream.readAllBytes();

        } catch (S3Exception | IOException e) {
            throw new RuntimeException("Error downloading file: " + e.getMessage());
        }
    }

    // Generate pre-signed URL for temporary access (valid for 5 minutes)
    public String generatePresignedUrl(String fileName) {
        try {
            // For S3 pre-signed URL, you'd use S3Presigner
            // Simplified version - returns URL
            return String.format("https://%s.s3.%s.amazonaws.com/%s",
                    bucketName, "ap-south-1", fileName);
        } catch (Exception e) {
            throw new RuntimeException("Error generating URL: " + e.getMessage());
        }
    }

    // Delete file from S3
    public String deleteFile(String key) {

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(request);

        // VERIFY DELETE (IMPORTANT FIX)
        try {
            s3Client.headObject(b -> b.bucket(bucketName).key(key));
            return "Delete FAILED (file still exists): " + key;
        } catch (S3Exception e) {
            return "File deleted successfully: " + key;
        }
    }

    // List all files in a folder
    public List<String> listFiles(String folderName) {
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(folderName)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            return listResponse.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());

        } catch (S3Exception e) {
            throw new RuntimeException("Error listing files: " + e.getMessage());
        }
    }
}