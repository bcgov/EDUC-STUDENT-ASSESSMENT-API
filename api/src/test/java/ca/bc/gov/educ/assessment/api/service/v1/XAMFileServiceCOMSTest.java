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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

        when(comsRestUtils.uploadObject(any(byte[].class), anyString()))
                .thenReturn(uploadResponse);

        doNothing().when(comsRestUtils).makeObjectPublic(anyString());

        assertDoesNotThrow(() -> xamFileService.uploadToComs(testContent, testKey));

        ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        verify(comsRestUtils).uploadObject(contentCaptor.capture(), keyCaptor.capture());
        verify(comsRestUtils).makeObjectPublic(eq("test-object-id"));

        assertArrayEquals(testContent, contentCaptor.getValue());
        assertEquals(testKey, keyCaptor.getValue());
    }

    @Test
    void testUploadToComs_Exception() {
        byte[] testContent = "test content".getBytes();
        String testKey = "xam-files/12345678_" + UUID.randomUUID() + ".xam";

        when(comsRestUtils.uploadObject(any(byte[].class), anyString()))
            .thenThrow(new StudentAssessmentAPIRuntimeException("COMS upload failed"));

        StudentAssessmentAPIRuntimeException exception = assertThrows(StudentAssessmentAPIRuntimeException.class,
            () -> xamFileService.uploadToComs(testContent, testKey));

        assertTrue(exception.getMessage().contains("Failed to upload file to COMS"));

        verify(comsRestUtils, never()).makeObjectPublic(anyString());
    }

    @Test
    void testUploadToComs_VerificationFailure() {
        byte[] testContent = "test content".getBytes();
        String testKey = "xam-files/12345678_" + UUID.randomUUID() + ".xam";

        // Upload response with null ID - validation should fail
        ObjectMetadata uploadResponse = ObjectMetadata.builder()
                .id(null)  // NULL ID!
                .path(testKey)
                .name("test-file.xam")
                .size((long) testContent.length)
                .build();

        when(comsRestUtils.uploadObject(any(byte[].class), anyString()))
                .thenReturn(uploadResponse);

        StudentAssessmentAPIRuntimeException exception = assertThrows(StudentAssessmentAPIRuntimeException.class,
                () -> xamFileService.uploadToComs(testContent, testKey));

        assertTrue(exception.getMessage().contains("ID or Path is null"));

        verify(comsRestUtils).uploadObject(any(byte[].class), anyString());
        // Should not attempt to make public if validation failed
        verify(comsRestUtils, never()).makeObjectPublic(anyString());
    }

    @Test
    void testGenerateAndUploadXamFiles_Success() {
        AssessmentSessionEntity sessionEntity = createMockSession();
        when(sessionEntity.getSessionID()).thenReturn(UUID.randomUUID());

        SchoolTombstone school = createTestSchool("12345678", "MYED");
        var schoolID = school.getSchoolId();

        when(stagedStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStagedAssessmentStudentStatusIn(eq(sessionEntity.getSessionID()), any(), eq(List.of("ACTIVE"))))
            .thenReturn(List.of());

        List<SchoolTombstone> schools = Arrays.asList(
            school,
            createTestSchool("87654321", "OTHER")
        );
        when(restUtils.getAllSchoolTombstones()).thenReturn(schools);
        when(stagedStudentRepository.getSchoolIDsOfSchoolsWithStudentsInSession(any())).thenReturn(List.of(UUID.fromString(schoolID)));
        doNothing().when(xamFileService).uploadToComs(any(byte[].class), anyString());

        assertDoesNotThrow(() -> xamFileService.generateAndUploadXamFiles(sessionEntity));

        // Verify a single zip upload
        verify(xamFileService, times(1)).uploadToComs(any(byte[].class), anyString());

        // Verify the correct keys were used
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(xamFileService, times(1)).uploadToComs(any(byte[].class), keyCaptor.capture());

        assertEquals("xam-files-202309/xam-files-202309.zip", keyCaptor.getValue());
    }

    @Test
    void testGenerateAndUploadXamFiles_ComsException() {
        AssessmentSessionEntity sessionEntity = createMockSession();
        when(sessionEntity.getSessionID()).thenReturn(UUID.randomUUID());

        SchoolTombstone school = createTestSchool("12345678", "MYED");
        var schoolID = school.getSchoolId();
        when(restUtils.getAllSchoolTombstones()).thenReturn(List.of(school));
        when(stagedStudentRepository.getSchoolIDsOfSchoolsWithStudentsInSession(any())).thenReturn(List.of(UUID.fromString(schoolID)));
        when(stagedStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStagedAssessmentStudentStatusIn(eq(sessionEntity.getSessionID()), any(), eq(List.of("ACTIVE"))))
                .thenReturn(List.of());

        doThrow(new StudentAssessmentAPIRuntimeException("COMS upload failed")).when(xamFileService).uploadToComs(any(byte[].class), anyString());

        // The method should throw
        StudentAssessmentAPIRuntimeException exception = assertThrows(StudentAssessmentAPIRuntimeException.class,
                () -> xamFileService.generateAndUploadXamFiles(sessionEntity));

        assertTrue(exception.getMessage().contains("COMS upload failed"));

        verify(xamFileService).uploadToComs(any(byte[].class), eq("xam-files-202309/xam-files-202309.zip"));
    }

    @Test
    void testGenerateAndUploadXamFiles_OnlyMyEdSchools() throws IOException {
        AssessmentSessionEntity sessionEntity = createMockSession();
        when(sessionEntity.getSessionID()).thenReturn(UUID.randomUUID());

        SchoolTombstone school = createTestSchool("12345678", "MYED");
        var schoolID = school.getSchoolId();

        List<SchoolTombstone> schools = Arrays.asList(
                school,
            createTestSchool("87654321", "OTHER"),
            createTestSchool("11223344", "BCSIS")
        );
        when(restUtils.getAllSchoolTombstones()).thenReturn(schools);

        when(stagedStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStagedAssessmentStudentStatusIn(eq(sessionEntity.getSessionID()), any(), eq(List.of("ACTIVE"))))
            .thenReturn(List.of());

        ArgumentCaptor<byte[]> zipCaptor = ArgumentCaptor.forClass(byte[].class);
        doNothing().when(xamFileService).uploadToComs(any(byte[].class), anyString());
        when(stagedStudentRepository.getSchoolIDsOfSchoolsWithStudentsInSession(any())).thenReturn(List.of(UUID.fromString(schoolID)));
        xamFileService.generateAndUploadXamFiles(sessionEntity);

        // Verify a single zip upload
        verify(xamFileService, times(1)).uploadToComs(zipCaptor.capture(), anyString());

        // Verify zip only contains the MYED school
        Set<String> entryNames = getZipEntryNames(zipCaptor.getValue());
        assertTrue(entryNames.contains("12345678-202309-Results.xam"));
        assertFalse(entryNames.stream().anyMatch(name -> name.contains("87654321")));
        assertFalse(entryNames.stream().anyMatch(name -> name.contains("11223344")));
    }

    @Test
    void testComsKeyFormat() {
        AssessmentSessionEntity sessionEntity = createMockSession();
        SchoolTombstone school = createTestSchool("12345678");
        var schoolID = school.getSchoolId();

        when(stagedStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStagedAssessmentStudentStatusIn(eq(sessionEntity.getSessionID()), any(), eq(List.of("ACTIVE"))))
            .thenReturn(List.of());
        when(restUtils.getAllSchoolTombstones()).thenReturn(List.of(school));
        doNothing().when(xamFileService).uploadToComs(any(byte[].class), anyString());
        when(stagedStudentRepository.getSchoolIDsOfSchoolsWithStudentsInSession(any())).thenReturn(List.of(UUID.fromString(schoolID)));
        xamFileService.generateAndUploadXamFiles(sessionEntity);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(xamFileService).uploadToComs(any(byte[].class), keyCaptor.capture());

        String expectedKey = "xam-files-202309/xam-files-202309.zip";
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

    private Set<String> getZipEntryNames(byte[] zipBytes) throws IOException {
        Set<String> names = new HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        return names;
    }
}
