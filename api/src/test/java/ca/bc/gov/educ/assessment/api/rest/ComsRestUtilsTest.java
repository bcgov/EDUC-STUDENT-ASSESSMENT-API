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
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

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
        lenient().when(applicationProperties.getS3BucketName()).thenReturn("test-bucket");
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

        when(comsWebClient.put()).thenReturn(requestBodyUriSpec);
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
        verify(comsWebClient).put();
        verify(requestBodyUriSpec).uri("/bucket");
    }

    @Test
    void testCreateBucket_ThrowsException() {
        // Arrange
        Bucket inputBucket = Bucket.builder().bucket("test-bucket").build();

        when(comsWebClient.put()).thenReturn(requestBodyUriSpec);
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
        verify(requestHeadersUriSpec).uri("/object/obj-123");
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
        lenient().when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ObjectMetadata.class)).thenReturn(Mono.just(expectedMetadata));

        // Act
        ObjectMetadata result = comsRestUtils.getObjectMetadata(objectId);

        // Assert
        assertNotNull(result);
        assertEquals(objectId, result.getId());
        assertEquals(1024L, result.getSize());
    }

    @Test
    void testGetObjectMetadata_ThrowsException() {
        // Arrange
        String objectId = "obj-123";

        when(comsWebClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
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
        verify(requestBodyUriSpec).uri("/object/obj-123/public");
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
        verify(requestBodyUriSpec).uri("/object/sync");
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

    @Test
    void testUploadObject_WithFolderPath_CreatesChildBucket() {
        // Arrange
        String path = "xam-files-202606/07324032-202606-Results.xam";
        byte[] content = "# No assessment results for this school\n".getBytes();

        Bucket parentBucket = Bucket.builder()
                .bucketId("parent-bucket-id")
                .bucket("test-bucket")  // Match the config bucket name
                .build();

        Bucket childBucket = Bucket.builder()
                .bucketId("child-bucket-id")
                .bucket("test-bucket/xam-files-202606")
                .key("xam-files-202606")
                .build();

        ObjectMetadata uploadedObject = ObjectMetadata.builder()
                .id("obj-123")
                .name("07324032-202606-Results.xam")
                .path("xam-files-202606/07324032-202606-Results.xam")
                .size((long) content.length)
                .build();

        // Mock getBuckets - called in getBucketByName
        when(comsWebClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.bodyToFlux(Bucket.class)).thenReturn(Flux.just(parentBucket));

        // Mock createChildBucket
        when(comsWebClient.put()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        lenient().when(responseSpec.bodyToMono(Bucket.class)).thenReturn(Mono.just(childBucket));

        // Mock uploadObject PUT request
        lenient().when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.contentType(MediaType.APPLICATION_OCTET_STREAM)).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.bodyValue(any(byte[].class))).thenReturn(requestHeadersSpec);
        lenient().when(responseSpec.bodyToMono(ObjectMetadata.class)).thenReturn(Mono.just(uploadedObject));

        // Act
        ObjectMetadata result = comsRestUtils.uploadObject(content, path);

        // Assert
        assertNotNull(result);
        assertEquals("obj-123", result.getId());
        assertEquals("07324032-202606-Results.xam", result.getName());
    }

    @Test
    void testUploadObject_WithoutFolderPath_UploadsToParentBucket() {
        // Arrange
        String path = "simple-file.xam";
        byte[] content = "test content".getBytes();

        Bucket parentBucket = Bucket.builder()
                .bucketId("parent-bucket-id")
                .bucket("test-bucket")
                .build();

        ObjectMetadata uploadedObject = ObjectMetadata.builder()
                .id("obj-456")
                .name("simple-file.xam")
                .path("simple-file.xam")
                .size((long) content.length)
                .build();

        // Mock getBuckets - called in getBucketByName
        when(comsWebClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.bodyToFlux(Bucket.class)).thenReturn(Flux.just(parentBucket));

        // Mock uploadObject PUT request
        when(comsWebClient.put()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.contentType(MediaType.APPLICATION_OCTET_STREAM)).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.bodyValue(any(byte[].class))).thenReturn(requestHeadersSpec);
        lenient().when(responseSpec.bodyToMono(ObjectMetadata.class)).thenReturn(Mono.just(uploadedObject));

        // Act
        ObjectMetadata result = comsRestUtils.uploadObject(content, path);

        // Assert
        assertNotNull(result);
        assertEquals("obj-456", result.getId());
        assertEquals("simple-file.xam", result.getName());
    }

    @Test
    void testUploadObject_ThrowsException() {
        // Arrange
        String path = "xam-files-202606/07324032-202606-Results.xam";
        byte[] content = "test".getBytes();

        // Mock getBuckets to throw exception
        when(comsWebClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenThrow(new RuntimeException("Upload failed"));

        // Act & Assert
        StudentAssessmentAPIRuntimeException exception = assertThrows(
                StudentAssessmentAPIRuntimeException.class,
                () -> comsRestUtils.uploadObject(content, path)
        );

        assertTrue(exception.getMessage().contains("Failed to"));
    }
}

