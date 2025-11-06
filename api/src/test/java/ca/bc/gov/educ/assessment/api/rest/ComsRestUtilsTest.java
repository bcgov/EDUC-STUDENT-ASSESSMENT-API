package ca.bc.gov.educ.assessment.api.rest;

import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.struct.external.coms.v1.Bucket;
import ca.bc.gov.educ.assessment.api.struct.external.coms.v1.ObjectMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComsRestUtilsTest {

    @Mock
    private WebClient comsWebClient;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ComsRestUtils comsRestUtils;

    @BeforeEach
    void setUp() {
        when(applicationProperties.getComsEndpointUrl()).thenReturn("https://coms.api.test");
        comsRestUtils = new ComsRestUtils(comsWebClient, applicationProperties);
    }

    @Test
    void testCreateBucket_Success() {
        // Arrange
        Bucket inputBucket = Bucket.builder()
                .bucket("test-bucket")
                .endpoint("https://s3.test")
                .build();

        Bucket expectedBucket = Bucket.builder()
                .bucketId("bucket-123")
                .bucket("test-bucket")
                .endpoint("https://s3.test")
                .build();

        when(comsWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Bucket.class)).thenReturn(Mono.just(expectedBucket));

        // Act
        Bucket result = comsRestUtils.createBucket(inputBucket);

        // Assert
        assertNotNull(result);
        assertEquals("bucket-123", result.getBucketId());
        assertEquals("test-bucket", result.getBucket());
        verify(comsWebClient).post();
        verify(requestBodyUriSpec).uri("https://coms.api.test/bucket");
    }

    @Test
    void testCreateBucket_ThrowsException() {
        // Arrange
        Bucket inputBucket = Bucket.builder().bucket("test-bucket").build();

        when(comsWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(new RuntimeException("Connection failed"));

        // Act & Assert
        StudentAssessmentAPIRuntimeException exception = assertThrows(
                StudentAssessmentAPIRuntimeException.class,
                () -> comsRestUtils.createBucket(inputBucket)
        );

        assertTrue(exception.getMessage().contains("Failed to create bucket in COMS"));
    }

    @Test
    void testGetBuckets_Success() {
        // Arrange
        Bucket bucket1 = Bucket.builder().bucketId("1").bucket("bucket1").build();
        Bucket bucket2 = Bucket.builder().bucketId("2").bucket("bucket2").build();

        when(comsWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(Bucket.class)).thenReturn(Flux.just(bucket1, bucket2));

        // Act
        List<Bucket> result = comsRestUtils.getBuckets();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("bucket1", result.get(0).getBucket());
        assertEquals("bucket2", result.get(1).getBucket());
    }

    @Test
    void testGetBuckets_ThrowsException() {
        // Arrange
        when(comsWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(new RuntimeException("Network error"));

        // Act & Assert
        StudentAssessmentAPIRuntimeException exception = assertThrows(
                StudentAssessmentAPIRuntimeException.class,
                () -> comsRestUtils.getBuckets()
        );

        assertTrue(exception.getMessage().contains("Failed to retrieve buckets from COMS"));
    }

    @Test
    void testUploadObject_Success() {
        // Arrange
        byte[] content = "test file content".getBytes();
        String path = "xam-files-202309/test-file.xam";

        ObjectMetadata expectedMetadata = ObjectMetadata.builder()
                .id("obj-123")
                .path("xam-files-202309")
                .name("test-file.xam")
                .size(17L)
                .build();

        when(comsWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.MULTIPART_FORM_DATA)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ObjectMetadata.class)).thenReturn(Mono.just(expectedMetadata));

        // Act
        ObjectMetadata result = comsRestUtils.uploadObject(content, path);

        // Assert
        assertNotNull(result);
        assertEquals("obj-123", result.getId());
        assertEquals("test-file.xam", result.getName());
        verify(requestBodyUriSpec).uri("https://coms.api.test/object");
    }

    @Test
    void testUploadObject_PathWithoutSlash() {
        // Arrange
        byte[] content = "test".getBytes();
        String path = "simple-file.txt";

        ObjectMetadata expectedMetadata = ObjectMetadata.builder()
                .id("obj-456")
                .name("simple-file.txt")
                .build();

        when(comsWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.MULTIPART_FORM_DATA)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ObjectMetadata.class)).thenReturn(Mono.just(expectedMetadata));

        // Act
        ObjectMetadata result = comsRestUtils.uploadObject(content, path);

        // Assert
        assertNotNull(result);
        assertEquals("obj-456", result.getId());
    }

    @Test
    void testUploadObject_ThrowsException() {
        // Arrange
        byte[] content = "test".getBytes();
        String path = "test.txt";

        when(comsWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.MULTIPART_FORM_DATA)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(new RuntimeException("Upload failed"));

        // Act & Assert
        StudentAssessmentAPIRuntimeException exception = assertThrows(
                StudentAssessmentAPIRuntimeException.class,
                () -> comsRestUtils.uploadObject(content, path)
        );

        assertTrue(exception.getMessage().contains("Failed to upload file to COMS"));
    }

    @Test
    void testDeleteObject_Success() {
        // Arrange
        String objectId = "obj-123";

        when(comsWebClient.delete()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        // Act
        assertDoesNotThrow(() -> comsRestUtils.deleteObject(objectId));

        // Assert
        verify(requestHeadersUriSpec).uri("https://coms.api.test/object/obj-123");
    }

    @Test
    void testDeleteObject_ThrowsException() {
        // Arrange
        String objectId = "obj-123";

        when(comsWebClient.delete()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(new RuntimeException("Delete failed"));

        // Act & Assert
        StudentAssessmentAPIRuntimeException exception = assertThrows(
                StudentAssessmentAPIRuntimeException.class,
                () -> comsRestUtils.deleteObject(objectId)
        );

        assertTrue(exception.getMessage().contains("Failed to delete object from COMS"));
    }

    @Test
    void testGetObjectMetadata_Success() {
        // Arrange
        String objectId = "obj-123";
        ObjectMetadata expectedMetadata = ObjectMetadata.builder()
                .id(objectId)
                .path("xam-files")
                .size(1024L)
                .build();

        when(comsWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ObjectMetadata.class)).thenReturn(Mono.just(expectedMetadata));

        // Act
        ObjectMetadata result = comsRestUtils.getObjectMetadata(objectId);

        // Assert
        assertNotNull(result);
        assertEquals(objectId, result.getId());
        assertEquals(1024L, result.getSize());
        verify(requestHeadersUriSpec).uri("https://coms.api.test/object/obj-123");
    }

    @Test
    void testGetObjectMetadata_ThrowsException() {
        // Arrange
        String objectId = "obj-123";

        when(comsWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(new RuntimeException("Not found"));

        // Act & Assert
        StudentAssessmentAPIRuntimeException exception = assertThrows(
                StudentAssessmentAPIRuntimeException.class,
                () -> comsRestUtils.getObjectMetadata(objectId)
        );

        assertTrue(exception.getMessage().contains("Failed to retrieve object metadata from COMS"));
    }

    @Test
    void testMakeObjectPublic_Success() {
        // Arrange
        String objectId = "obj-123";

        when(comsWebClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        // Act
        assertDoesNotThrow(() -> comsRestUtils.makeObjectPublic(objectId));

        // Assert
        verify(requestBodyUriSpec).uri("https://coms.api.test/object/obj-123/public");
        verify(requestBodySpec).bodyValue("{\"public\": true}");
    }

    @Test
    void testMakeObjectPublic_ThrowsException() {
        // Arrange
        String objectId = "obj-123";

        when(comsWebClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(new RuntimeException("Permission denied"));

        // Act & Assert
        StudentAssessmentAPIRuntimeException exception = assertThrows(
                StudentAssessmentAPIRuntimeException.class,
                () -> comsRestUtils.makeObjectPublic(objectId)
        );

        assertTrue(exception.getMessage().contains("Failed to make object public in COMS"));
    }

    @Test
    void testAddObjectPermission_WithUserId() {
        // Arrange
        String objectId = "obj-123";
        String permissionType = "READ";
        String userId = "user-456";

        when(comsWebClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        // Act
        assertDoesNotThrow(() -> comsRestUtils.addObjectPermission(objectId, permissionType, userId));

        // Assert
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestBodySpec).bodyValue(bodyCaptor.capture());
        String requestBody = bodyCaptor.getValue();
        assertTrue(requestBody.contains("READ"));
        assertTrue(requestBody.contains("user-456"));
    }

    @Test
    void testAddObjectPermission_WithoutUserId() {
        // Arrange
        String objectId = "obj-123";
        String permissionType = "WRITE";

        when(comsWebClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        // Act
        assertDoesNotThrow(() -> comsRestUtils.addObjectPermission(objectId, permissionType, null));

        // Assert
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestBodySpec).bodyValue(bodyCaptor.capture());
        String requestBody = bodyCaptor.getValue();
        assertTrue(requestBody.contains("WRITE"));
        assertFalse(requestBody.contains("userId"));
    }

    @Test
    void testAddObjectPermission_ThrowsException() {
        // Arrange
        String objectId = "obj-123";

        when(comsWebClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(new RuntimeException("Failed"));

        // Act & Assert
        StudentAssessmentAPIRuntimeException exception = assertThrows(
                StudentAssessmentAPIRuntimeException.class,
                () -> comsRestUtils.addObjectPermission(objectId, "READ", "user")
        );

        assertTrue(exception.getMessage().contains("Failed to add permission to object in COMS"));
    }

    @Test
    void testSyncPath_Success() {
        // Arrange
        String path = "xam-files-202309/";

        when(comsWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        // Act
        assertDoesNotThrow(() -> comsRestUtils.syncPath(path));

        // Assert
        verify(requestBodyUriSpec).uri("https://coms.api.test/object/sync");
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestBodySpec).bodyValue(bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("xam-files-202309/"));
    }

    @Test
    void testSyncPath_ThrowsException() {
        // Arrange
        String path = "test-path/";

        when(comsWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(new RuntimeException("Sync failed"));

        // Act & Assert
        StudentAssessmentAPIRuntimeException exception = assertThrows(
                StudentAssessmentAPIRuntimeException.class,
                () -> comsRestUtils.syncPath(path)
        );

        assertTrue(exception.getMessage().contains("Failed to sync path in COMS"));
    }
}

