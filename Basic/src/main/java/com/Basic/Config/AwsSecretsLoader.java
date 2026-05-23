package com.Basic.Config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class AwsSecretsLoader {

    private static final String REGION = System.getenv("AWS_REGION");

    private static final String DB_SECRET_NAME = System.getenv("DB_SECRET_NAME");
    private static final String S3_SECRET_NAME = System.getenv("S3_SECRET_NAME");

    private static final String ACCESS_KEY = System.getenv("AWS_ACCESS_KEY_ID");
    private static final String SECRET_KEY = System.getenv("AWS_SECRET_ACCESS_KEY");

    public static void loadSecrets() {
        Region secretsRegion = resolveRegion();
        System.out.println("REGION = " + secretsRegion.id());
        try {

            SecretsManagerClient client = SecretsManagerClient.builder()
                    .region(secretsRegion)
                    .credentialsProvider(credentialsProvider())
                    .build();

            ObjectMapper mapper = new ObjectMapper();

            // =========================
            // 1. DB SECRET (DB-Secret)
            // =========================
            GetSecretValueResponse dbResponse = client.getSecretValue(
                    GetSecretValueRequest.builder()
                            .secretId(DB_SECRET_NAME)
                            .build()
            );

            String dbSecretString = dbResponse.secretString();

            if (dbSecretString == null || dbSecretString.isEmpty()) {
                throw new RuntimeException("DB secret is empty");
            }

            JsonNode dbJson = mapper.readTree(dbSecretString);

            String username = dbJson.path("username").asText(null);
            String password = dbJson.path("password").asText(null);
            String host = dbJson.path("host").asText(null);
            int port = dbJson.path("port").asInt(3306);
            String db = dbJson.path("dbInstanceIdentifier").asText(null);

            if (username == null || password == null || host == null) {
                throw new RuntimeException("Missing required DB fields in secret JSON");
            }

            String dbUrl = "jdbc:mysql://" + host + ":" + port + "/" + db;
            System.out.println("dbUrl----->"+" "+dbUrl);
            System.setProperty("DB_URL", dbUrl);
            System.setProperty("DB_USERNAME", username);
            System.setProperty("DB_PASSWORD", password);

            // =========================
            // 2. S3 SECRET (EC2-S3-Secrets)
            // =========================
            GetSecretValueResponse s3Response = client.getSecretValue(
                    GetSecretValueRequest.builder()
                            .secretId(S3_SECRET_NAME)
                            .build()
            );

            String s3SecretString = s3Response.secretString();

            if (s3SecretString == null || s3SecretString.isEmpty()) {
                throw new RuntimeException("S3 secret is empty");
            }

            JsonNode s3Json = mapper.readTree(s3SecretString);

            String bucketName = s3Json.path("AWS_S3_BUCKET_NAME").asText(null);
            String region = s3Json.path("AWS_REGION").asText(null);

            if (bucketName == null || region == null) {
                throw new RuntimeException("Missing required S3 fields in secret JSON");
            }

            System.setProperty("AWS_S3_BUCKET_NAME", bucketName);
            System.setProperty("AWS_REGION", region);

            // =========================
            // DONE
            // =========================
            System.out.println("✅ DB Secret Loaded");
            System.out.println("✅ S3 Secret Loaded");
            System.out.println("🚀 AWS Secrets loaded successfully");

            client.close();

        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to load AWS Secrets", e);
        }
    }

    private static AwsCredentialsProvider credentialsProvider() {
        String accessKey = firstNonBlank(
                System.getenv("AWS_ACCESS_KEY_ID"),
                ACCESS_KEY,
                System.getProperty("aws.accessKeyId")
        );
        String secretKey = firstNonBlank(
                System.getenv("AWS_SECRET_ACCESS_KEY"),
                SECRET_KEY,
                System.getProperty("aws.secretAccessKey")
        );

        if (accessKey != null && secretKey != null) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            return StaticCredentialsProvider.create(credentials);
        }

        return DefaultCredentialsProvider.create();
    }

    private static Region resolveRegion() {
        String region = firstNonBlank(
                REGION,
                System.getenv("AWS_DEFAULT_REGION"),
                System.getProperty("aws.region")
        );

        if (region != null) {
            return Region.of(region);
        }

        return DefaultAwsRegionProviderChain.builder().build().getRegion();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
