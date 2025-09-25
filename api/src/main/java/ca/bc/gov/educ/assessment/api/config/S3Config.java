package ca.bc.gov.educ.assessment.api.config;

import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class S3Config {

    private final ApplicationProperties applicationProperties;

    @Bean
    public S3Client s3Client() {
        String endpoint = applicationProperties.getS3EndpointUrl();
        String accessKeyId = applicationProperties.getS3AccessKeyId();
        String secretKey = applicationProperties.getS3AccessSecretKey();
        String bucketName = applicationProperties.getS3BucketName();

        log.info("Configuring S3 client for BCBox:");
        log.info("  - Endpoint: {}", endpoint);
        log.info("  - Bucket: {}", bucketName);
        log.info("  - Access Key ID: {}", accessKeyId != null && !accessKeyId.isEmpty() ? "***configured***" : "NOT CONFIGURED");
        log.info("  - Secret Key: {}", secretKey != null && !secretKey.isEmpty() ? "***configured***" : "NOT CONFIGURED");

        if (endpoint == null || endpoint.isEmpty()) {
            throw new IllegalStateException("S3 endpoint URL is not configured");
        }
        if (bucketName == null || bucketName.isEmpty()) {
            throw new IllegalStateException("S3 bucket name is not configured");
        }
        if (accessKeyId == null || accessKeyId.isEmpty()) {
            throw new IllegalStateException("S3 access key ID is not configured");
        }
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalStateException("S3 secret key is not configured");
        }

        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKeyId, secretKey);

        return S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
            .region(Region.CA_WEST_1) // todo where do we want these to land????
            .forcePathStyle(true) // required for BCBox S3 compatibility
            .build();
    }
}
