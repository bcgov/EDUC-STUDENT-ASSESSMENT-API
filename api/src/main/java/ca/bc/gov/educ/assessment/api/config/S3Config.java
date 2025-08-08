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
        log.debug("Configuring S3 client for BCBox with endpoint: {}", applicationProperties.getS3EndpointUrl());

        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
            applicationProperties.getS3AccessKeyId(),
            applicationProperties.getS3AccessSecretKey()
        );

        return S3Client.builder()
            .endpointOverride(URI.create(applicationProperties.getS3EndpointUrl()))
            .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
            .region(Region.CA_WEST_1) // todo BCBox typically uses us-east-1 - what region are we using?
            .forcePathStyle(true) // required for BCBox S3 compatibility
            .build();
    }
}
