package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.StagedAssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.StagedAssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.rest.ComsRestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.coms.v1.ObjectMetadata;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class XAMFileServiceTest extends BaseAssessmentAPITest {

    private XAMFileService xamFileService;
    private AssessmentSessionRepository sessionRepository;
    private AssessmentStudentRepository studentRepository;
    private StagedAssessmentStudentRepository stagedStudentRepository;
    private RestUtils restUtils;
    private ComsRestUtils comsRestUtils;
    private ApplicationProperties applicationProperties;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(AssessmentSessionRepository.class);
        studentRepository = mock(AssessmentStudentRepository.class);
        stagedStudentRepository = mock(StagedAssessmentStudentRepository.class);
        restUtils = mock(RestUtils.class);
        comsRestUtils = mock(ComsRestUtils.class);
        applicationProperties = mock(ApplicationProperties.class);

        when(applicationProperties.getS3BucketName()).thenReturn("test-bucket");
        when(applicationProperties.getComsEndpointUrl()).thenReturn("https://test-endpoint.com");

        ObjectMetadata uploadResponse = ObjectMetadata.builder()
                .id("test-object-id")
                .path("test-path")
                .name("test-file.xam")
                .size(100L)
                .build();

        when(comsRestUtils.uploadObject(any(byte[].class), any(String.class)))
                .thenReturn(uploadResponse);

        // Mock makeObjectPublic to do nothing (void method)
        doNothing().when(comsRestUtils).makeObjectPublic(any(String.class));

        xamFileService = spy(new XAMFileService(studentRepository, sessionRepository, restUtils, comsRestUtils, applicationProperties, stagedStudentRepository));
    }

    @AfterEach
    void tearDown() {
        sessionRepository.deleteAll();
        studentRepository.deleteAll();
    }

    @Test
    void testGenerateXamContent_success() {
        AssessmentSessionEntity sessionEntity = mock(AssessmentSessionEntity.class);
        when(sessionEntity.getSessionID()).thenReturn(UUID.randomUUID());
        when(sessionEntity.getCourseYear()).thenReturn("2023");
        when(sessionEntity.getCourseMonth()).thenReturn("09");

        StagedAssessmentStudentEntity student = mock(StagedAssessmentStudentEntity.class);
        when(student.getLocalID()).thenReturn("LID123");
        when(student.getPen()).thenReturn("PEN123456");
        when(student.getProficiencyScore()).thenReturn(5);
        when(student.getProvincialSpecialCaseCode()).thenReturn("N");
        when(student.getLocalAssessmentID()).thenReturn("LOCALASSMT");
        when(student.getSurname()).thenReturn("Doe");

        AssessmentEntity assessment = mock(AssessmentEntity.class);
        when(assessment.getAssessmentTypeCode()).thenReturn("TYPE");
        when(assessment.getAssessmentSessionEntity()).thenReturn(sessionEntity);
        when(student.getAssessmentEntity()).thenReturn(assessment);

        when(stagedStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStagedAssessmentStudentStatusIn(eq(sessionEntity.getSessionID()), any(), eq(List.of("ACTIVE", "MERGED"))))
                .thenReturn(List.of(student));

        SchoolTombstone school = mock(SchoolTombstone.class);
        when(school.getSchoolId()).thenReturn(UUID.randomUUID().toString());
        when(school.getMincode()).thenReturn("MINCODE1");
        when(school.getVendorSourceSystemCode()).thenReturn("MYED");

        String content = xamFileService.generateXamContent(sessionEntity, school, true);
        assertFalse(content.isEmpty());
        assertTrue(content.contains("Doe"));
    }

    @Test
    void testGenerateXamReport_success() {
        UUID sessionId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID examSchoolId = UUID.randomUUID();

        SchoolTombstone mainSchool = new SchoolTombstone();
        mainSchool.setSchoolId(schoolId.toString());
        mainSchool.setMincode("123456");
        mainSchool.setVendorSourceSystemCode("MYED");

        SchoolTombstone examSchool = new SchoolTombstone();
        examSchool.setSchoolId(examSchoolId.toString());
        examSchool.setMincode("987654");

        AssessmentSessionEntity sessionEntity = mock(AssessmentSessionEntity.class);
        when(sessionEntity.getSessionID()).thenReturn(sessionId);
        when(sessionEntity.getCourseYear()).thenReturn("2023");
        when(sessionEntity.getCourseMonth()).thenReturn("09");
        when(sessionRepository.findById(eq(sessionId))).thenReturn(Optional.of(sessionEntity));

        AssessmentStudentEntity student = mock(AssessmentStudentEntity.class);
        when(student.getAssessmentCenterSchoolID()).thenReturn(examSchoolId);
        when(student.getPen()).thenReturn("123456789");


        when(studentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStudentStatusCodeIn(sessionEntity.getSessionID(), schoolId, List.of("ACTIVE")))
                .thenReturn(List.of(student));

        when(restUtils.getSchoolBySchoolID(schoolId.toString())).thenReturn(Optional.of(mainSchool));
        when(restUtils.getSchoolBySchoolID(examSchoolId.toString())).thenReturn(Optional.of(examSchool));

        DownloadableReportResponse response = xamFileService.generateXamReport(sessionId, schoolId);

        assertNotNull(response);
        assertFalse(response.getDocumentData().isEmpty());
        assertTrue(response.getReportType().contains("123456-202309-Results.xam"));

        String decodedContent = new String(Base64.getDecoder().decode(response.getDocumentData()));
        assertTrue(decodedContent.contains("987654"));
    }

    @Test
    void testGenerateXamFile_sessionNotFound() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(eq(sessionId))).thenReturn(Optional.empty());

        SchoolTombstone school = mock(SchoolTombstone.class);
        when(school.getSchoolId()).thenReturn(UUID.randomUUID().toString());
        when(school.getMincode()).thenReturn("MINCODE3");
        when(school.getVendorSourceSystemCode()).thenReturn("MYED");

        assertThrows(EntityNotFoundException.class, () -> xamFileService.generateXamReport(sessionId, UUID.fromString(school.getSchoolId())));
    }

    @Test
    void testUploadToComs_success() {
        byte[] testContent = "test content".getBytes();
        assertDoesNotThrow(() -> xamFileService.uploadToComs(testContent, "dummyKey.txt"));

        verify(comsRestUtils).uploadObject(any(byte[].class), eq("dummyKey.txt"));
        verify(comsRestUtils).makeObjectPublic(eq("test-object-id"));
    }

    @Test
    void testUploadToComs_failsWhenResponseMissingId() {
        byte[] testContent = "test content".getBytes();

        // Mock upload to return response with null ID
        ObjectMetadata invalidResponse = ObjectMetadata.builder()
                .id(null)  // Missing ID!
                .path("test-path")
                .name("test-file.xam")
                .size(100L)
                .build();
        when(comsRestUtils.uploadObject(any(byte[].class), any(String.class)))
                .thenReturn(invalidResponse);

        // Should throw exception because ID is null
        Exception exception = assertThrows(Exception.class,
            () -> xamFileService.uploadToComs(testContent, "dummyKey.txt"));

        assertTrue(exception.getMessage().contains("ID or Path is null"));

        // Should not attempt to make public if validation failed
        verify(comsRestUtils, never()).makeObjectPublic(anyString());
    }

    @Test
    void testUploadToComs_failsWhenResponseMissingPath() {
        byte[] testContent = "test content".getBytes();

        // Mock upload to return response with null path
        ObjectMetadata invalidResponse = ObjectMetadata.builder()
                .id("test-id")
                .path(null)  // Missing path!
                .name("test-file.xam")
                .size(100L)
                .build();
        when(comsRestUtils.uploadObject(any(byte[].class), any(String.class)))
                .thenReturn(invalidResponse);

        // Should throw exception because path is null
        Exception exception = assertThrows(Exception.class,
            () -> xamFileService.uploadToComs(testContent, "dummyKey.txt"));

        assertTrue(exception.getMessage().contains("ID or Path is null"));

        // Should not attempt to make public if validation failed
        verify(comsRestUtils, never()).makeObjectPublic(anyString());
    }

    @Test
    void testGenerateAndUploadXamFiles() {
        var schoolID = UUID.randomUUID().toString();
        AssessmentSessionEntity sessionEntity = mock(AssessmentSessionEntity.class);
        when(sessionEntity.getSessionID()).thenReturn(UUID.randomUUID());
        when(sessionEntity.getCourseYear()).thenReturn("2023");
        when(sessionEntity.getCourseMonth()).thenReturn("09");
        when(stagedStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStagedAssessmentStudentStatusIn(eq(sessionEntity.getSessionID()), any(), eq(List.of("ACTIVE", "MERGED"))))
                .thenReturn(List.of());

        SchoolTombstone myEdSchool = mock(SchoolTombstone.class);
        when(myEdSchool.getVendorSourceSystemCode()).thenReturn("MYED");
        when(myEdSchool.getMincode()).thenReturn("MINCODE4");
        when(myEdSchool.getSchoolId()).thenReturn(schoolID);
        when(restUtils.getAllSchoolTombstones()).thenReturn(List.of(myEdSchool));
        when(stagedStudentRepository.getSchoolIDsOfSchoolsWithStudentsInSession(any())).thenReturn(List.of(UUID.fromString(schoolID)));

        doNothing().when(xamFileService).uploadToComs(any(byte[].class), any());
        xamFileService.generateAndUploadXamFiles(sessionEntity);

        // Verify a single zip is uploaded
        verify(xamFileService, times(1)).uploadToComs(any(byte[].class), eq("xam-files-202309/xam-files-202309.zip"));
    }

    @Test
    void testGenerateAndUploadXamFiles_withDifferentYearAndMonth() {
        // Test with a different year and month to ensure dynamic folder creation
        var schoolID = UUID.randomUUID().toString();

        AssessmentSessionEntity sessionEntity = mock(AssessmentSessionEntity.class);
        when(sessionEntity.getSessionID()).thenReturn(UUID.randomUUID());
        when(sessionEntity.getCourseYear()).thenReturn("2025");
        when(sessionEntity.getCourseMonth()).thenReturn("10");
        when(stagedStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStagedAssessmentStudentStatusIn(eq(sessionEntity.getSessionID()), any(), eq(List.of("ACTIVE", "MERGED"))))
                .thenReturn(List.of());

        SchoolTombstone myEdSchool = mock(SchoolTombstone.class);
        when(myEdSchool.getVendorSourceSystemCode()).thenReturn("MYED");
        when(myEdSchool.getMincode()).thenReturn("12345678");
        when(myEdSchool.getSchoolId()).thenReturn(schoolID);
        when(restUtils.getAllSchoolTombstones()).thenReturn(List.of(myEdSchool));
        when(stagedStudentRepository.getSchoolIDsOfSchoolsWithStudentsInSession(any())).thenReturn(List.of(UUID.fromString(schoolID)));

        doNothing().when(xamFileService).uploadToComs(any(byte[].class), any());
        xamFileService.generateAndUploadXamFiles(sessionEntity);

        // Verify single zip uploaded to xam-files-202510/
        verify(xamFileService, times(1)).uploadToComs(any(byte[].class), eq("xam-files-202510/xam-files-202510.zip"));
    }

    @Test
    void testGenerateAndUploadXamFiles_multipleSchools() throws IOException {
        // Test that multiple schools all end up in the same zip
        AssessmentSessionEntity sessionEntity = mock(AssessmentSessionEntity.class);
        when(sessionEntity.getSessionID()).thenReturn(UUID.randomUUID());
        when(sessionEntity.getCourseYear()).thenReturn("2024");
        when(sessionEntity.getCourseMonth()).thenReturn("03");
        when(stagedStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStagedAssessmentStudentStatusIn(eq(sessionEntity.getSessionID()), any(), eq(List.of("ACTIVE", "MERGED"))))
                .thenReturn(List.of());

        SchoolTombstone school1 = mock(SchoolTombstone.class);
        when(school1.getVendorSourceSystemCode()).thenReturn("MYED");
        when(school1.getMincode()).thenReturn("11111111");
        when(school1.getSchoolId()).thenReturn(UUID.randomUUID().toString());

        SchoolTombstone school2 = mock(SchoolTombstone.class);
        when(school2.getVendorSourceSystemCode()).thenReturn("MYED");
        when(school2.getMincode()).thenReturn("22222222");
        when(school2.getSchoolId()).thenReturn(UUID.randomUUID().toString());

        SchoolTombstone nonMyEdSchool = mock(SchoolTombstone.class);
        when(nonMyEdSchool.getVendorSourceSystemCode()).thenReturn("OTHER");
        when(nonMyEdSchool.getMincode()).thenReturn("33333333");

        var schoolsWithStudents = List.of(UUID.fromString(school1.getSchoolId()), UUID.fromString(school2.getSchoolId()));
        when(restUtils.getAllSchoolTombstones()).thenReturn(List.of(school1, school2, nonMyEdSchool));
        when(stagedStudentRepository.getSchoolIDsOfSchoolsWithStudentsInSession(any()))
                .thenReturn(schoolsWithStudents);

        org.mockito.ArgumentCaptor<byte[]> zipCaptor = org.mockito.ArgumentCaptor.forClass(byte[].class);
        doNothing().when(xamFileService).uploadToComs(any(byte[].class), any());
        xamFileService.generateAndUploadXamFiles(sessionEntity);

        // Verify a single zip is uploaded
        verify(xamFileService, times(1)).uploadToComs(zipCaptor.capture(), eq("xam-files-202403/xam-files-202403.zip"));

        // Verify zip contents contain both MYED schools but not the non-MYED school
        Set<String> entryNames = getZipEntryNames(zipCaptor.getValue());
        assertTrue(entryNames.contains("11111111-202403-Results.xam"));
        assertTrue(entryNames.contains("22222222-202403-Results.xam"));
        assertFalse(entryNames.stream().anyMatch(name -> name.contains("33333333")));
    }

    @Test
    void testUploadToComs_verifyFolderPathFormat() {
        // Test that COMS upload accepts folder-prefixed keys and validates response
        byte[] testContent = "test XAM file content".getBytes();
        String folderKey = "xam-files-202510/TEST-202510-Results.xam";

        assertDoesNotThrow(() -> xamFileService.uploadToComs(testContent, folderKey));

        // Verify the upload was made with the full path including folder prefix
        verify(comsRestUtils).uploadObject(testContent, folderKey);
        // Verify upload response was validated (makeObjectPublic only called if validation passed)
        verify(comsRestUtils).makeObjectPublic(eq("test-object-id"));
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

    @Test
    void testGenerateAndUploadXamFilesAsync_SessionNotFound_ThrowsEntityNotFoundException() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> xamFileService.generateAndUploadXamFiles(sessionId));
    }

    @Test
    void testGenerateAndUploadXamFilesAsync_SessionWithoutCompletionDate_ThrowsRuntimeException() {
        UUID sessionId = UUID.randomUUID();
        AssessmentSessionEntity session = mock(AssessmentSessionEntity.class);
        when(session.getCompletionDate()).thenReturn(null);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        StudentAssessmentAPIRuntimeException exception = assertThrows(
                StudentAssessmentAPIRuntimeException.class,
                () -> xamFileService.generateAndUploadXamFiles(sessionId));

        assertTrue(exception.getMessage().contains("completion date is not set"));
    }

    @Test
    void testGenerateAndUploadXamFilesAsync_ValidSession_DelegatesToInternalMethod() {
        UUID sessionId = UUID.randomUUID();
        AssessmentSessionEntity session = mock(AssessmentSessionEntity.class);
        when(session.getSessionID()).thenReturn(sessionId);
        when(session.getCourseYear()).thenReturn("2024");
        when(session.getCourseMonth()).thenReturn("06");
        when(session.getCompletionDate()).thenReturn(LocalDateTime.now().minusDays(1));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        when(restUtils.getAllSchoolTombstones()).thenReturn(List.of());
        when(studentRepository.getSchoolIDsOfSchoolsWithStudentsInSession(sessionId)).thenReturn(List.of());

        assertDoesNotThrow(() -> xamFileService.generateAndUploadXamFiles(sessionId));
        // No MYED schools with students → no zip is uploaded
        verify(xamFileService, never()).uploadToComs(any(), any());
    }
}
