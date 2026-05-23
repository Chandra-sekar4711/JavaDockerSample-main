package com.Basic.Config;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {

    @Value("${aws.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .build();
    }
    private AwsCredentialsProvider credentialsProvider() {
        String accessKey = firstNonBlank(
                System.getenv("AWS_ACCESS_KEY_ID"),
                System.getProperty("aws.accessKeyId")
        );
        String secretKey = firstNonBlank(
                System.getenv("AWS_SECRET_ACCESS_KEY"),
                System.getProperty("aws.secretAccessKey")
        );

        if (accessKey != null && secretKey != null) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            return StaticCredentialsProvider.create(credentials);
        }

        return DefaultCredentialsProvider.create();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
