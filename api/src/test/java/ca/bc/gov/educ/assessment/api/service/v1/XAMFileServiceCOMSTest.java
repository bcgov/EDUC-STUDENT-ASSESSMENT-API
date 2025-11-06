package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.StagedAssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.rest.ComsRestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.coms.v1.ObjectMetadata;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class XAMFileServiceCOMSTest extends BaseAssessmentAPITest {

    private XAMFileService xamFileService;
    private AssessmentStudentRepository studentRepository;
    private StagedAssessmentStudentRepository stagedStudentRepository;
    private RestUtils restUtils;
    private ComsRestUtils comsRestUtils;
    private ApplicationProperties applicationProperties;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        AssessmentSessionRepository sessionRepository = mock(AssessmentSessionRepository.class);
        studentRepository = mock(AssessmentStudentRepository.class);
        stagedStudentRepository = mock(StagedAssessmentStudentRepository.class);
        restUtils = mock(RestUtils.class);
        comsRestUtils = mock(ComsRestUtils.class);
        applicationProperties = mock(ApplicationProperties.class);

        when(applicationProperties.getS3BucketName()).thenReturn("test-bucket");
        when(applicationProperties.getComsEndpointUrl()).thenReturn("https://test-endpoint.com");

        xamFileService = spy(new XAMFileService(studentRepository, sessionRepository, restUtils, comsRestUtils, applicationProperties, stagedStudentRepository));
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
    void testUploadToComs_Success() {
        byte[] testContent = "test content".getBytes();
        String testKey = "xam-files/12345678_" + UUID.randomUUID() + ".xam";

        ObjectMetadata uploadResponse = ObjectMetadata.builder()
                .id("test-object-id")
                .path(testKey)
                .name("test-file.xam")
                .size((long) testContent.length)
                .build();

        when(comsRestUtils.uploadObject(any(byte[].class), eq(testKey)))
                .thenReturn(uploadResponse);

        ObjectMetadata metadataResponse = ObjectMetadata.builder()
                .id("test-object-id")
                .path(testKey)
                .size((long) testContent.length)
                .build();
        when(comsRestUtils.getObjectMetadata(eq("test-object-id")))

                .thenReturn(metadataResponse);

        assertDoesNotThrow(() -> xamFileService.uploadToComs(testContent, testKey));

        ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        verify(comsRestUtils).uploadObject(contentCaptor.capture(), keyCaptor.capture());
        verify(comsRestUtils).getObjectMetadata(eq("test-object-id"));

        assertArrayEquals(testContent, contentCaptor.getValue());
        assertEquals(testKey, keyCaptor.getValue());
    }

    @Test
    void testUploadToComs_Exception() {
        byte[] testContent = "test content".getBytes();
        String testKey = "xam-files/12345678_" + UUID.randomUUID() + ".xam";

        when(comsRestUtils.uploadObject(any(byte[].class), eq(testKey)))
            .thenThrow(new StudentAssessmentAPIRuntimeException("COMS upload failed"));

        StudentAssessmentAPIRuntimeException exception = assertThrows(StudentAssessmentAPIRuntimeException.class,
            () -> xamFileService.uploadToComs(testContent, testKey));

        assertTrue(exception.getMessage().contains("Failed to upload file to COMS"));

        verify(comsRestUtils, never()).getObjectMetadata(anyString());
    }

    @Test
    void testUploadToComs_VerificationFailure() {
        byte[] testContent = "test content".getBytes();
        String testKey = "xam-files/12345678_" + UUID.randomUUID() + ".xam";

        ObjectMetadata uploadResponse = ObjectMetadata.builder()
                .id("test-object-id")
                .path(testKey)
                .name("test-file.xam")
                .size((long) testContent.length)
                .build();

        when(comsRestUtils.uploadObject(any(byte[].class), eq(testKey)))
                .thenReturn(uploadResponse);

        when(comsRestUtils.getObjectMetadata(eq("test-object-id")))
                .thenThrow(new StudentAssessmentAPIRuntimeException("Object not found"));

        StudentAssessmentAPIRuntimeException exception = assertThrows(StudentAssessmentAPIRuntimeException.class,
                () -> xamFileService.uploadToComs(testContent, testKey));

        assertTrue(exception.getMessage().contains("File upload appeared successful but verification failed"));

        verify(comsRestUtils).uploadObject(any(byte[].class), eq(testKey));
        verify(comsRestUtils).getObjectMetadata(eq("test-object-id"));
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

        doNothing().when(xamFileService).uploadToComs(any(byte[].class), anyString());

        assertDoesNotThrow(() -> xamFileService.generateAndUploadXamFiles(sessionEntity));

        // Verify uploadToComs was called twice (once for each MYED school)
        verify(xamFileService, times(2)).uploadToComs(any(byte[].class), anyString());

        // Verify the correct keys were used
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(xamFileService, times(2)).uploadToComs(any(byte[].class), keyCaptor.capture());

        List<String> capturedKeys = keyCaptor.getAllValues();
        assertTrue(capturedKeys.contains("xam-files-202309/12345678-202309-Results.xam"));
        assertTrue(capturedKeys.contains("xam-files-202309/11223344-202309-Results.xam"));
    }

    @Test
    void testGenerateAndUploadXamFiles_ComsException() {
        AssessmentSessionEntity sessionEntity = createMockSession();
        when(sessionEntity.getSessionID()).thenReturn(UUID.randomUUID());

        SchoolTombstone school = createTestSchool("12345678", "MYED");
        when(restUtils.getAllSchoolTombstones()).thenReturn(List.of(school));

        when(stagedStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStagedAssessmentStudentStatusIn(eq(sessionEntity.getSessionID()), any(), eq(List.of("ACTIVE"))))
                .thenReturn(List.of());

        doThrow(new StudentAssessmentAPIRuntimeException("COMS upload failed")).when(xamFileService).uploadToComs(any(byte[].class), anyString());

        // The method should throw
        StudentAssessmentAPIRuntimeException exception = assertThrows(StudentAssessmentAPIRuntimeException.class,
                () -> xamFileService.generateAndUploadXamFiles(sessionEntity));

        assertTrue(exception.getMessage().contains("COMS upload failed"));

        verify(xamFileService).uploadToComs(any(byte[].class), eq("xam-files-202309/12345678-202309-Results.xam"));
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

        doNothing().when(xamFileService).uploadToComs(any(byte[].class), anyString());

        xamFileService.generateAndUploadXamFiles(sessionEntity);

        // Verify uploadToComs was called only for MYED schools (2 times)
        verify(xamFileService, times(2)).uploadToComs(any(byte[].class), anyString());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(xamFileService, times(2)).uploadToComs(any(byte[].class), keyCaptor.capture());

        List<String> capturedKeys = keyCaptor.getAllValues();
        assertTrue(capturedKeys.contains("xam-files-202309/12345678-202309-Results.xam"));
        assertTrue(capturedKeys.contains("xam-files-202309/55667788-202309-Results.xam"));
        assertFalse(capturedKeys.stream().anyMatch(key -> key.contains("87654321")));
        assertFalse(capturedKeys.stream().anyMatch(key -> key.contains("11223344")));
    }

    @Test
    void testComsKeyFormat() {
        AssessmentSessionEntity sessionEntity = createMockSession();
        SchoolTombstone school = createTestSchool("12345678");

        when(stagedStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStagedAssessmentStudentStatusIn(eq(sessionEntity.getSessionID()), any(), eq(List.of("ACTIVE"))))
            .thenReturn(List.of());

        when(restUtils.getAllSchoolTombstones()).thenReturn(List.of(school));
        doNothing().when(xamFileService).uploadToComs(any(byte[].class), anyString());

        xamFileService.generateAndUploadXamFiles(sessionEntity);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(xamFileService).uploadToComs(any(byte[].class), keyCaptor.capture());

        String expectedKey = "xam-files-202309/12345678-202309-Results.xam";
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
