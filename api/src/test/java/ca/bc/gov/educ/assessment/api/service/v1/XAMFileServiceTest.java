package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class XAMFileServiceTest extends BaseAssessmentAPITest {

    private XAMFileService xamFileService;
    private AssessmentSessionRepository sessionRepository;
    private AssessmentStudentRepository studentRepository;
    private RestUtils restUtils;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(AssessmentSessionRepository.class);
        studentRepository = mock(AssessmentStudentRepository.class);
        restUtils = mock(RestUtils.class);
        S3Client s3Client = mock(S3Client.class);
        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);

        when(applicationProperties.getS3BucketName()).thenReturn("test-bucket");

        xamFileService = spy(new XAMFileService(studentRepository, sessionRepository, restUtils, s3Client, applicationProperties));
    }

    @AfterEach
    void tearDown() {
        sessionRepository.deleteAll();
        studentRepository.deleteAll();
    }

    @Test
    void testGenerateXamFile_success() throws Exception {
        AssessmentSessionEntity sessionEntity = mock(AssessmentSessionEntity.class);
        when(sessionEntity.getSessionID()).thenReturn(UUID.randomUUID());
        when(sessionEntity.getCourseYear()).thenReturn("2023");
        when(sessionEntity.getCourseMonth()).thenReturn("09");

        AssessmentStudentEntity student = mock(AssessmentStudentEntity.class);
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

        when(studentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolID(eq(sessionEntity.getSessionID()), any()))
                .thenReturn(List.of(student));

        SchoolTombstone school = mock(SchoolTombstone.class);
        when(school.getSchoolId()).thenReturn(UUID.randomUUID().toString());
        when(school.getMincode()).thenReturn("MINCODE1");
        when(school.getVendorSourceSystemCode()).thenReturn("MYED");

        File file = xamFileService.generateXamFile(sessionEntity, school);
        assertTrue(file.exists());
        String content = Files.readString(file.toPath());
        assertTrue(content.contains("Doe"));
        file.delete();
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


        when(studentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolID(sessionEntity.getSessionID(), schoolId))
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

        Exception ex = assertThrows(EntityNotFoundException.class, () -> {
            xamFileService.generateXamReport(sessionId, UUID.fromString(school.getSchoolId()));
        });
    }

    @Test
    void testUploadToS3_success() throws Exception {
        File dummyFile = new File("dummy.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dummyFile))) {
            writer.write("test");
        }
        assertDoesNotThrow(() -> xamFileService.uploadToS3(dummyFile, "dummyKey.txt"));
        dummyFile.delete();
    }

    @Test
    void testGenerateAndUploadXamFiles() {
        AssessmentSessionEntity sessionEntity = mock(AssessmentSessionEntity.class);
        when(sessionEntity.getSessionID()).thenReturn(UUID.randomUUID());
        when(sessionEntity.getCourseYear()).thenReturn("2023");
        when(sessionEntity.getCourseMonth()).thenReturn("09");
        when(studentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolID(eq(sessionEntity.getSessionID()), any()))
                .thenReturn(List.of());

        SchoolTombstone myEdSchool = mock(SchoolTombstone.class);
        when(myEdSchool.getVendorSourceSystemCode()).thenReturn("MYED");
        when(myEdSchool.getMincode()).thenReturn("MINCODE4");
        when(myEdSchool.getSchoolId()).thenReturn(UUID.randomUUID().toString());
        when(restUtils.getAllSchoolTombstones()).thenReturn(List.of(myEdSchool));

        doNothing().when(xamFileService).uploadToS3(any(), any());
        xamFileService.generateAndUploadXamFiles(sessionEntity);
    }

    @Test
    void testUploadFilePathToS3() throws Exception {
        File dummyFile = new File("dummy2.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dummyFile))) {
            writer.write("test");
        }
        AssessmentSessionEntity sessionEntity = mock(AssessmentSessionEntity.class);
        when(sessionEntity.getCourseYear()).thenReturn("2023");
        when(sessionEntity.getCourseMonth()).thenReturn("09");

        SchoolTombstone school = mock(SchoolTombstone.class);
        when(school.getMincode()).thenReturn("MINCODE5");
        when(school.getSchoolId()).thenReturn(UUID.randomUUID().toString());

        doNothing().when(xamFileService).uploadToS3(any(), any());
        xamFileService.uploadFilePathToS3(dummyFile.getAbsolutePath(), sessionEntity, school);
        verify(xamFileService, atLeastOnce()).uploadToS3(any(), any());
        dummyFile.delete();
    }
}
