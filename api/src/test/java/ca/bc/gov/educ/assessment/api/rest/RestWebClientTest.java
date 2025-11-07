package ca.bc.gov.educ.assessment.api.rest;

import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestWebClientTest {

    @Mock
    private ApplicationProperties applicationProperties;

    private RestWebClient restWebClient;

    @BeforeEach
    void setUp() {
        // Setup mock properties
        when(applicationProperties.getClientID()).thenReturn("test-client-id");
        when(applicationProperties.getClientSecret()).thenReturn("test-client-secret");
        when(applicationProperties.getTokenURL()).thenReturn("https://auth.test/token");
        when(applicationProperties.getChesClientID()).thenReturn("ches-client-id");
        when(applicationProperties.getChesClientSecret()).thenReturn("ches-secret");
        when(applicationProperties.getChesTokenURL()).thenReturn("https://ches.auth.test/token");
        when(applicationProperties.getS3AccessKeyId()).thenReturn("s3-access-key");
        when(applicationProperties.getS3AccessSecretKey()).thenReturn("s3-secret-key");
        when(applicationProperties.getS3BucketName()).thenReturn("test-bucket");
        when(applicationProperties.getS3EndpointUrl()).thenReturn("https://s3.test.com");

        restWebClient = new RestWebClient(applicationProperties);
    }

    @Test
    void testAllWebClientTypesCanBeCreated() {
        // Ensure all three types of WebClient can be created from the same instance
        WebClient.Builder builder = WebClient.builder();

        WebClient mainClient = restWebClient.webClient(builder);
        WebClient chesClient = restWebClient.chesWebClient();
        WebClient comsClient = restWebClient.comsWebClient();

        assertNotNull(mainClient);
        assertNotNull(chesClient);
        assertNotNull(comsClient);
    }
}

