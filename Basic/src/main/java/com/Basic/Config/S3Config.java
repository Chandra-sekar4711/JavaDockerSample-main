package com.Basic.Config;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {

    private static final String REGION = System.getenv("AWS_REGION");

    private static final String ACCESS_KEY = System.getenv("AWS_ACCESS_KEY");
    private static final String SECRET_KEY = System.getenv("AWS_SECRET_KEY");

    @Bean
    public S3Client s3Client() {
        // Uses EC2 IAM role automatically when deployed
        // For local testing, uses ~/.aws/credentials

        AwsBasicCredentials credentials = AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY);
        return S3Client.builder()
                .region(Region.of(REGION))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}