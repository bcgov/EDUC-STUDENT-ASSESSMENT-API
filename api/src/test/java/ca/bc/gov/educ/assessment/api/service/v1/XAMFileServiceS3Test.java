package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.StagedAssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class XAMFileServiceS3Test extends BaseAssessmentAPITest {

    private XAMFileService xamFileService;
    private AssessmentStudentRepository studentRepository;
    private StagedAssessmentStudentRepository stagedStudentRepository;
    private RestUtils restUtils;
    private S3Client s3Client;
    private ApplicationProperties applicationProperties;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        AssessmentSessionRepository sessionRepository = mock(AssessmentSessionRepository.class);
        studentRepository = mock(AssessmentStudentRepository.class);
        stagedStudentRepository = mock(StagedAssessmentStudentRepository.class);
        restUtils = mock(RestUtils.class);
        s3Client = mock(S3Client.class);
        applicationProperties = mock(ApplicationProperties.class);

        when(applicationProperties.getS3BucketName()).thenReturn("test-bucket");
        when(applicationProperties.getS3EndpointUrl()).thenReturn("https://test-endpoint.com");
        when(applicationProperties.getS3AccessKeyId()).thenReturn("test-access-key");
        when(applicationProperties.getS3AccessSecretKey()).thenReturn("test-secret-key");

        xamFileService = spy(new XAMFileService(studentRepository, sessionRepository, restUtils, s3Client, applicationProperties, stagedStudentRepository));
    }

    @AfterEach
    void tearDown() {
        try (var stream = Files.walk(tempDir)) {
            stream.filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        // Ignore cleanup errors
                    }
                });
        } catch (IOException e) {
            // Ignore cleanup errors
        }
    }

    @Test
    void testUploadToS3_Success() {
        byte[] testContent = "test content".getBytes();
        String testKey = "xam-files/12345678_" + UUID.randomUUID() + ".xam";

        S3ResponseMetadata responseMetadata = mock(S3ResponseMetadata.class);
        when(responseMetadata.requestId()).thenReturn("test-request-id");

        PutObjectResponse putResponse = PutObjectResponse.builder()
                .eTag("test-etag")
                .versionId("test-version")
                .build();

        PutObjectResponse spyPutResponse = spy(putResponse);
        when(spyPutResponse.responseMetadata()).thenReturn(responseMetadata);

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(spyPutResponse);

        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .contentLength((long) testContent.length)
                .lastModified(Instant.now())
                .build();
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(headResponse);

        assertDoesNotThrow(() -> xamFileService.uploadToS3(testContent, testKey));

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

        verify(s3Client).putObject(requestCaptor.capture(), bodyCaptor.capture());
        verify(s3Client).headObject(any(HeadObjectRequest.class));

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertEquals("test-bucket", capturedRequest.bucket());
        assertEquals(testKey, capturedRequest.key());
    }

    @Test
    void testUploadToS3_S3Exception() {
        byte[] testContent = "test content".getBytes();
        String testKey = "xam-files/12345678_" + UUID.randomUUID() + ".xam";

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(S3Exception.builder().message("S3 upload failed").build());

        StudentAssessmentAPIRuntimeException exception = assertThrows(StudentAssessmentAPIRuntimeException.class,
            () -> xamFileService.uploadToS3(testContent, testKey));

        assertTrue(exception.getMessage().contains("Failed to upload file to BCBox S3"));
        assertTrue(exception.getMessage().contains("S3 upload failed"));

        verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
    }

    @Test
    void testUploadToS3_VerificationFailure() {
        byte[] testContent = "test content".getBytes();
        String testKey = "xam-files/12345678_" + UUID.randomUUID() + ".xam";

        S3ResponseMetadata responseMetadata = mock(S3ResponseMetadata.class);
        when(responseMetadata.requestId()).thenReturn("test-request-id");

        PutObjectResponse putResponse = PutObjectResponse.builder()
                .eTag("test-etag")
                .versionId("test-version")
                .build();

        PutObjectResponse spyPutResponse = spy(putResponse);
        when(spyPutResponse.responseMetadata()).thenReturn(responseMetadata);

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(spyPutResponse);

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("Object not found").build());

        StudentAssessmentAPIRuntimeException exception = assertThrows(StudentAssessmentAPIRuntimeException.class,
                () -> xamFileService.uploadToS3(testContent, testKey));

        assertTrue(exception.getMessage().contains("File upload appeared successful but verification failed"));

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(s3Client).headObject(any(HeadObjectRequest.class));
    }

    @Test
    void testGenerateAndUploadXamFiles_Success() {
        AssessmentSessionEntity sessionEntity = createMockSession();
        when(sessionEntity.getSessionID()).thenReturn(UUID.randomUUID());

        when(stagedStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStagedAssessmentStudentStatusIn(eq(sessionEntity.getSessionID()), any(), eq(List.of("ACTIVE"))))
            .thenReturn(List.of());

        List<SchoolTombstone> schools = Arrays.asList(
            createTestSchool("12345678", "MYED"),
            createTestSchool("87654321", "OTHER"),
            createTestSchool("11223344", "MYED")
        );
        when(restUtils.getAllSchoolTombstones()).thenReturn(schools);

        doNothing().when(xamFileService).uploadToS3(any(byte[].class), anyString());

        assertDoesNotThrow(() -> xamFileService.generateAndUploadXamFiles(sessionEntity));

        // Verify uploadToS3 was called twice (once for each MYED school)
        verify(xamFileService, times(2)).uploadToS3(any(byte[].class), anyString());

        // Verify the correct keys were used
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(xamFileService, times(2)).uploadToS3(any(byte[].class), keyCaptor.capture());

        List<String> capturedKeys = keyCaptor.getAllValues();
        assertTrue(capturedKeys.contains("xam-files/12345678-202309-Results.xam"));
        assertTrue(capturedKeys.contains("xam-files/11223344-202309-Results.xam"));
    }

    @Test
    void testGenerateAndUploadXamFiles_S3Exception() {
        AssessmentSessionEntity sessionEntity = createMockSession();
        when(sessionEntity.getSessionID()).thenReturn(UUID.randomUUID());

        SchoolTombstone school = createTestSchool("12345678", "MYED");
        when(restUtils.getAllSchoolTombstones()).thenReturn(List.of(school));

        when(stagedStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStagedAssessmentStudentStatusIn(eq(sessionEntity.getSessionID()), any(), eq(List.of("ACTIVE"))))
                .thenReturn(List.of());

        doThrow(new StudentAssessmentAPIRuntimeException("S3 upload failed")).when(xamFileService).uploadToS3(any(byte[].class), anyString());

        // The method should throw
        StudentAssessmentAPIRuntimeException exception = assertThrows(StudentAssessmentAPIRuntimeException.class,
                () -> xamFileService.generateAndUploadXamFiles(sessionEntity));

        assertTrue(exception.getMessage().contains("S3 upload failed"));

        verify(xamFileService).uploadToS3(any(byte[].class), eq("xam-files/12345678-202309-Results.xam"));
    }

    @Test
    void testGenerateAndUploadXamFiles_OnlyMyEdSchools() {
        AssessmentSessionEntity sessionEntity = createMockSession();
        when(sessionEntity.getSessionID()).thenReturn(UUID.randomUUID());

        List<SchoolTombstone> schools = Arrays.asList(
            createTestSchool("12345678", "MYED"),
            createTestSchool("87654321", "OTHER"),
            createTestSchool("11223344", "BCSIS"),
            createTestSchool("55667788", "MYED")
        );
        when(restUtils.getAllSchoolTombstones()).thenReturn(schools);

        when(stagedStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStagedAssessmentStudentStatusIn(eq(sessionEntity.getSessionID()), any(), eq(List.of("ACTIVE"))))
            .thenReturn(List.of());

        doNothing().when(xamFileService).uploadToS3(any(byte[].class), anyString());

        xamFileService.generateAndUploadXamFiles(sessionEntity);

        // Verify uploadToS3 was called only for MYED schools (2 times)
        verify(xamFileService, times(2)).uploadToS3(any(byte[].class), anyString());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(xamFileService, times(2)).uploadToS3(any(byte[].class), keyCaptor.capture());

        List<String> capturedKeys = keyCaptor.getAllValues();
        assertTrue(capturedKeys.contains("xam-files/12345678-202309-Results.xam"));
        assertTrue(capturedKeys.contains("xam-files/55667788-202309-Results.xam"));
        assertFalse(capturedKeys.stream().anyMatch(key -> key.contains("87654321")));
        assertFalse(capturedKeys.stream().anyMatch(key -> key.contains("11223344")));
    }

    @Test
    void testS3KeyFormat() {
        AssessmentSessionEntity sessionEntity = createMockSession();
        SchoolTombstone school = createTestSchool("12345678");

        when(stagedStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStagedAssessmentStudentStatusIn(eq(sessionEntity.getSessionID()), any(), eq(List.of("ACTIVE"))))
            .thenReturn(List.of());

        when(restUtils.getAllSchoolTombstones()).thenReturn(List.of(school));
        doNothing().when(xamFileService).uploadToS3(any(byte[].class), anyString());

        xamFileService.generateAndUploadXamFiles(sessionEntity);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(xamFileService).uploadToS3(any(byte[].class), keyCaptor.capture());

        String expectedKey = "xam-files/12345678-202309-Results.xam";
        assertEquals(expectedKey, keyCaptor.getValue());
    }

    private SchoolTombstone createTestSchool(String mincode) {
        return createTestSchool(mincode, "MYED");
    }

    private SchoolTombstone createTestSchool(String mincode, String vendorCode) {
        SchoolTombstone school = new SchoolTombstone();
        school.setSchoolId(UUID.randomUUID().toString());
        school.setMincode(mincode);
        school.setVendorSourceSystemCode(vendorCode);
        return school;
    }

    private AssessmentSessionEntity createMockSession() {
        AssessmentSessionEntity session = mock(AssessmentSessionEntity.class);
        when(session.getCourseYear()).thenReturn("2023");
        when(session.getCourseMonth()).thenReturn("09");
        return session;
    }
}
