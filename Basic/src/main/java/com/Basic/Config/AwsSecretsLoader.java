package com.Basic.Config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
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

        try {

            AwsBasicCredentials awsCreds =
                    AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY);

            SecretsManagerClient client = SecretsManagerClient.builder()
                    .region(Region.of(REGION))
                    .credentialsProvider(
                            StaticCredentialsProvider.create(awsCreds)
                    )
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
            String access_key = s3Json.path("ACCESS_KEY").asText(null);
            String secret_key = s3Json.path("SECRET_KEY").asText(null);

            if (bucketName == null || region == null) {
                throw new RuntimeException("Missing required S3 fields in secret JSON");
            }

            System.setProperty("AWS_S3_BUCKET_NAME", bucketName);
            System.setProperty("AWS_REGION", region);
            System.setProperty("AWS_ACCESS_KEY_ID", access_key);
            System.setProperty("AWS_SECRET_ACCESS_KEY", secret_key);

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
}
