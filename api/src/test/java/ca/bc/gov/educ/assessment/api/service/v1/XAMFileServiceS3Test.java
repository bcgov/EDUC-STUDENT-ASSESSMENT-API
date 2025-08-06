package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class XAMFileServiceS3Test extends BaseAssessmentAPITest {

    private XAMFileService xamFileService;
    private AssessmentSessionRepository sessionRepository;
    private AssessmentStudentRepository studentRepository;
    private RestUtils restUtils;
    private S3Client s3Client;
    private ApplicationProperties applicationProperties;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(AssessmentSessionRepository.class);
        studentRepository = mock(AssessmentStudentRepository.class);
        restUtils = mock(RestUtils.class);
        s3Client = mock(S3Client.class);
        applicationProperties = mock(ApplicationProperties.class);

        when(applicationProperties.getS3BucketName()).thenReturn("test-bucket");

        xamFileService = spy(new XAMFileService(studentRepository, sessionRepository, restUtils, s3Client, applicationProperties));
    }

    @AfterEach
    void tearDown() {
        try {
            Files.walk(tempDir)
                .filter(Files::isRegularFile)
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
    void testUploadToS3_Success() throws IOException {
        Path testFile = tempDir.resolve("test.xam");
        Files.write(testFile, "test content".getBytes());
        File file = testFile.toFile();

        String testKey = "xam-files/12345678_" + UUID.randomUUID() + ".xam";

        assertDoesNotThrow(() -> xamFileService.uploadToS3(file, testKey));

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

        verify(s3Client).putObject(requestCaptor.capture(), bodyCaptor.capture());

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertEquals("test-bucket", capturedRequest.bucket());
        assertEquals(testKey, capturedRequest.key());
    }

    @Test
    void testUploadToS3_S3Exception() throws IOException {
        Path testFile = tempDir.resolve("test.xam");
        Files.write(testFile, "test content".getBytes());
        File file = testFile.toFile();

        String testKey = "xam-files/12345678_" + UUID.randomUUID() + ".xam";

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(S3Exception.builder().message("S3 upload failed").build());

        StudentAssessmentAPIRuntimeException exception = assertThrows(StudentAssessmentAPIRuntimeException.class,
            () -> xamFileService.uploadToS3(file, testKey));

        assertTrue(exception.getMessage().contains("Failed to upload file to BCBox S3"));
        assertTrue(exception.getMessage().contains("S3 upload failed"));
    }

    @Test
    void testUploadFilePathToS3_Success() throws IOException {
        Path testFile = tempDir.resolve("school-session-results.xam");
        Files.write(testFile, "test xam content".getBytes());

        UUID sessionID = UUID.randomUUID();
        SchoolTombstone school = createTestSchool("12345678");

        doNothing().when(xamFileService).uploadToS3(any(File.class), anyString());

        assertDoesNotThrow(() -> xamFileService.uploadFilePathToS3(testFile.toString(), sessionID, school));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(xamFileService).uploadToS3(any(File.class), keyCaptor.capture());

        String capturedKey = keyCaptor.getValue();
        assertEquals("xam-files/12345678_" + sessionID + ".xam", capturedKey);
    }

    @Test
    void testGenerateAndUploadXamFiles_Success() {
        UUID sessionID = UUID.randomUUID();

        AssessmentSessionEntity sessionEntity = createMockSession();
        when(sessionRepository.findById(sessionID)).thenReturn(Optional.of(sessionEntity));

        when(studentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolID(eq(sessionID), any()))
            .thenReturn(List.of());

        List<SchoolTombstone> schools = Arrays.asList(
            createTestSchool("12345678", "MYED"),
            createTestSchool("87654321", "OTHER"),
            createTestSchool("11223344", "MYED")
        );
        when(restUtils.getAllSchoolTombstones()).thenReturn(schools);

        doReturn("test-file-path-1").when(xamFileService).generateXamFileAndReturnPath(sessionID, schools.get(0));
        doReturn("test-file-path-2").when(xamFileService).generateXamFileAndReturnPath(sessionID, schools.get(2));
        doNothing().when(xamFileService).uploadFilePathToS3(anyString(), eq(sessionID), any(SchoolTombstone.class));

        assertDoesNotThrow(() -> xamFileService.generateAndUploadXamFiles(sessionID));

        verify(xamFileService, times(2)).generateXamFileAndReturnPath(eq(sessionID), any(SchoolTombstone.class));
        verify(xamFileService, times(2)).uploadFilePathToS3(anyString(), eq(sessionID), any(SchoolTombstone.class));

        verify(xamFileService).generateXamFileAndReturnPath(sessionID, schools.get(0));
        verify(xamFileService).generateXamFileAndReturnPath(sessionID, schools.get(2));
        verify(xamFileService, never()).generateXamFileAndReturnPath(sessionID, schools.get(1));
    }

    @Test
    void testGenerateAndUploadXamFiles_PartialFailure() {
        UUID sessionID = UUID.randomUUID();

        AssessmentSessionEntity sessionEntity = createMockSession();
        when(sessionRepository.findById(sessionID)).thenReturn(Optional.of(sessionEntity));

        SchoolTombstone school1 = createTestSchool("12345678", "MYED");
        SchoolTombstone school2 = createTestSchool("87654321", "MYED");
        when(restUtils.getAllSchoolTombstones()).thenReturn(Arrays.asList(school1, school2));

        doReturn("test-file-path-1").when(xamFileService).generateXamFileAndReturnPath(sessionID, school1);
        doThrow(new RuntimeException("File generation failed")).when(xamFileService).generateXamFileAndReturnPath(sessionID, school2);
        doNothing().when(xamFileService).uploadFilePathToS3(anyString(), eq(sessionID), any(SchoolTombstone.class));

        when(studentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolID(eq(sessionID), any()))
            .thenReturn(List.of());

        assertDoesNotThrow(() -> xamFileService.generateAndUploadXamFiles(sessionID));

        verify(xamFileService).generateXamFileAndReturnPath(sessionID, school1);
        verify(xamFileService).uploadFilePathToS3("test-file-path-1", sessionID, school1);

        verify(xamFileService).generateXamFileAndReturnPath(sessionID, school2);
        verify(xamFileService, never()).uploadFilePathToS3(anyString(), eq(sessionID), eq(school2));
    }

    @Test
    void testGenerateAndUploadXamFiles_UploadFailure() {
        UUID sessionID = UUID.randomUUID();

        AssessmentSessionEntity sessionEntity = createMockSession();
        when(sessionRepository.findById(sessionID)).thenReturn(Optional.of(sessionEntity));

        SchoolTombstone school = createTestSchool("12345678", "MYED");
        when(restUtils.getAllSchoolTombstones()).thenReturn(List.of(school));

        doReturn("test-file-path").when(xamFileService).generateXamFileAndReturnPath(sessionID, school);
        doThrow(new RuntimeException("S3 upload failed")).when(xamFileService).uploadFilePathToS3(anyString(), eq(sessionID), eq(school));

        when(studentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolID(eq(sessionID), any()))
            .thenReturn(List.of());

        assertDoesNotThrow(() -> xamFileService.generateAndUploadXamFiles(sessionID));

        verify(xamFileService).generateXamFileAndReturnPath(sessionID, school);
        verify(xamFileService).uploadFilePathToS3("test-file-path", sessionID, school);
    }

    @Test
    void testFileCleanup_FileDeleteFails() {
        UUID sessionID = UUID.randomUUID();

        AssessmentSessionEntity sessionEntity = createMockSession();
        when(sessionRepository.findById(sessionID)).thenReturn(Optional.of(sessionEntity));

        SchoolTombstone school = createTestSchool("12345678", "MYED");
        when(restUtils.getAllSchoolTombstones()).thenReturn(List.of(school));

        doReturn("test-file-path").when(xamFileService).generateXamFileAndReturnPath(sessionID, school);
        doNothing().when(xamFileService).uploadFilePathToS3(anyString(), eq(sessionID), eq(school));

        when(studentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolID(eq(sessionID), any()))
            .thenReturn(List.of());

        assertDoesNotThrow(() -> xamFileService.generateAndUploadXamFiles(sessionID));

        verify(xamFileService).generateXamFileAndReturnPath(sessionID, school);
        verify(xamFileService).uploadFilePathToS3("test-file-path", sessionID, school);
    }

    @Test
    void testS3KeyFormat() throws IOException {
        Path testFile = tempDir.resolve("test.xam");
        Files.write(testFile, "test content".getBytes());

        UUID sessionID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        SchoolTombstone school = createTestSchool("12345678");

        doNothing().when(xamFileService).uploadToS3(any(File.class), anyString());

        xamFileService.uploadFilePathToS3(testFile.toString(), sessionID, school);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(xamFileService).uploadToS3(any(File.class), keyCaptor.capture());

        String expectedKey = "xam-files/12345678_123e4567-e89b-12d3-a456-426614174000.xam";
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
