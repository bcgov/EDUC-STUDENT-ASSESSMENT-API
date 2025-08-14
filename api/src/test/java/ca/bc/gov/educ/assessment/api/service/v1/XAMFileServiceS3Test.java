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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class XAMFileServiceS3Test extends BaseAssessmentAPITest {

    private XAMFileService xamFileService;
    private AssessmentStudentRepository studentRepository;
    private RestUtils restUtils;
    private S3Client s3Client;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        AssessmentSessionRepository sessionRepository = mock(AssessmentSessionRepository.class);
        studentRepository = mock(AssessmentStudentRepository.class);
        restUtils = mock(RestUtils.class);
        s3Client = mock(S3Client.class);
        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);

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

        AssessmentSessionEntity sessionEntity = createMockSession();
        SchoolTombstone school = createTestSchool("12345678");

        doNothing().when(xamFileService).uploadToS3(any(File.class), anyString());

        assertDoesNotThrow(() -> xamFileService.uploadFilePathToS3(testFile.toString(), sessionEntity, school));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(xamFileService).uploadToS3(any(File.class), keyCaptor.capture());

        String capturedKey = keyCaptor.getValue();
        assertEquals("xam-files/12345678-202309-Results.xam", capturedKey);
    }

    @Test
    void testGenerateAndUploadXamFiles_Success() {
        AssessmentSessionEntity sessionEntity = createMockSession();
        when(sessionEntity.getSessionID()).thenReturn(UUID.randomUUID());

        when(studentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStudentStatusCodeIn(eq(sessionEntity.getSessionID()), any(), eq(List.of("ACTIVE"))))
            .thenReturn(List.of());

        List<SchoolTombstone> schools = Arrays.asList(
            createTestSchool("12345678", "MYED"),
            createTestSchool("87654321", "OTHER"),
            createTestSchool("11223344", "MYED")
        );
        when(restUtils.getAllSchoolTombstones()).thenReturn(schools);

        doReturn("test-file-path-1").when(xamFileService).generateXamFileAndReturnPath(sessionEntity, schools.get(0));
        doReturn("test-file-path-2").when(xamFileService).generateXamFileAndReturnPath(sessionEntity, schools.get(2));
        doNothing().when(xamFileService).uploadFilePathToS3(anyString(), eq(sessionEntity), any(SchoolTombstone.class));

        assertDoesNotThrow(() -> xamFileService.generateAndUploadXamFiles(sessionEntity));

        verify(xamFileService, times(2)).generateXamFileAndReturnPath(eq(sessionEntity), any(SchoolTombstone.class));
        verify(xamFileService, times(2)).uploadFilePathToS3(anyString(), eq(sessionEntity), any(SchoolTombstone.class));

        verify(xamFileService).generateXamFileAndReturnPath(sessionEntity, schools.get(0));
        verify(xamFileService).generateXamFileAndReturnPath(sessionEntity, schools.get(2));
        verify(xamFileService, never()).generateXamFileAndReturnPath(sessionEntity, schools.get(1));
    }

    @Test
    void testGenerateAndUploadXamFiles_PartialFailure() {
        AssessmentSessionEntity sessionEntity = createMockSession();
        when(sessionEntity.getSessionID()).thenReturn(UUID.randomUUID());

        SchoolTombstone school1 = createTestSchool("12345678", "MYED");
        SchoolTombstone school2 = createTestSchool("87654321", "MYED");
        when(restUtils.getAllSchoolTombstones()).thenReturn(Arrays.asList(school1, school2));

        doReturn("test-file-path-1").when(xamFileService).generateXamFileAndReturnPath(sessionEntity, school1);
        doThrow(new RuntimeException("File generation failed")).when(xamFileService).generateXamFileAndReturnPath(sessionEntity, school2);
        doNothing().when(xamFileService).uploadFilePathToS3(anyString(), eq(sessionEntity), any(SchoolTombstone.class));

        when(studentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStudentStatusCodeIn(eq(sessionEntity.getSessionID()), any(), eq(List.of("ACTIVE"))))
            .thenReturn(List.of());

        assertDoesNotThrow(() -> xamFileService.generateAndUploadXamFiles(sessionEntity));

        verify(xamFileService).generateXamFileAndReturnPath(sessionEntity, school1);
        verify(xamFileService).uploadFilePathToS3("test-file-path-1", sessionEntity, school1);

        verify(xamFileService).generateXamFileAndReturnPath(sessionEntity, school2);
        verify(xamFileService, never()).uploadFilePathToS3(anyString(), eq(sessionEntity), eq(school2));
    }

    @Test
    void testGenerateAndUploadXamFiles_UploadFailure() {
        AssessmentSessionEntity sessionEntity = createMockSession();
        when(sessionEntity.getSessionID()).thenReturn(UUID.randomUUID());

        SchoolTombstone school = createTestSchool("12345678", "MYED");
        when(restUtils.getAllSchoolTombstones()).thenReturn(List.of(school));

        doReturn("test-file-path").when(xamFileService).generateXamFileAndReturnPath(sessionEntity, school);
        doThrow(new RuntimeException("S3 upload failed")).when(xamFileService).uploadFilePathToS3(anyString(), eq(sessionEntity), eq(school));

        when(studentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStudentStatusCodeIn(eq(sessionEntity.getSessionID()), any(), eq(List.of("ACTIVE"))))
            .thenReturn(List.of());

        assertDoesNotThrow(() -> xamFileService.generateAndUploadXamFiles(sessionEntity));

        verify(xamFileService).generateXamFileAndReturnPath(sessionEntity, school);
        verify(xamFileService).uploadFilePathToS3("test-file-path", sessionEntity, school);
    }

    @Test
    void testFileCleanup_FileDeleteFails() {
        AssessmentSessionEntity sessionEntity = createMockSession();
        when(sessionEntity.getSessionID()).thenReturn(UUID.randomUUID());

        SchoolTombstone school = createTestSchool("12345678", "MYED");
        when(restUtils.getAllSchoolTombstones()).thenReturn(List.of(school));

        doReturn("test-file-path").when(xamFileService).generateXamFileAndReturnPath(sessionEntity, school);
        doNothing().when(xamFileService).uploadFilePathToS3(anyString(), eq(sessionEntity), eq(school));

        when(studentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStudentStatusCodeIn(eq(sessionEntity.getSessionID()), any(), eq(List.of("ACTIVE"))))
            .thenReturn(List.of());

        assertDoesNotThrow(() -> xamFileService.generateAndUploadXamFiles(sessionEntity));

        verify(xamFileService).generateXamFileAndReturnPath(sessionEntity, school);
        verify(xamFileService).uploadFilePathToS3("test-file-path", sessionEntity, school);
    }

    @Test
    void testS3KeyFormat() throws IOException {
        Path testFile = tempDir.resolve("test.xam");
        Files.write(testFile, "test content".getBytes());

        AssessmentSessionEntity sessionEntity = createMockSession();
        SchoolTombstone school = createTestSchool("12345678");

        doNothing().when(xamFileService).uploadToS3(any(File.class), anyString());

        xamFileService.uploadFilePathToS3(testFile.toString(), sessionEntity, school);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(xamFileService).uploadToS3(any(File.class), keyCaptor.capture());

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
