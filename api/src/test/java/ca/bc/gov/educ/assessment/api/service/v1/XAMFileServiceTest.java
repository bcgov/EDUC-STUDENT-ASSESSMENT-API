package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.StagedAssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.StagedAssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.*;

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

    @BeforeEach
    void setUp() {
        sessionRepository = mock(AssessmentSessionRepository.class);
        studentRepository = mock(AssessmentStudentRepository.class);
        stagedStudentRepository = mock(StagedAssessmentStudentRepository.class);
        restUtils = mock(RestUtils.class);
        S3Client s3Client = mock(S3Client.class);
        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);

        when(applicationProperties.getS3BucketName()).thenReturn("test-bucket");

        xamFileService = spy(new XAMFileService(studentRepository, sessionRepository, restUtils, s3Client, applicationProperties, stagedStudentRepository));
    }

    @AfterEach
    void tearDown() {
        sessionRepository.deleteAll();
        studentRepository.deleteAll();
    }

    @Test
    void testGenerateXamFile_success() {
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

        when(stagedStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStagedAssessmentStudentStatusIn(eq(sessionEntity.getSessionID()), any(), eq(List.of("ACTIVE"))))
                .thenReturn(List.of(student));

        SchoolTombstone school = mock(SchoolTombstone.class);
        when(school.getSchoolId()).thenReturn(UUID.randomUUID().toString());
        when(school.getMincode()).thenReturn("MINCODE1");
        when(school.getVendorSourceSystemCode()).thenReturn("MYED");

        byte[] data = xamFileService.generateXamContent(sessionEntity, school, true);
        assertTrue(data.length > 0);
        String content = new String(data);
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
    void testUploadToS3_success() {
        byte[] testContent = "test content".getBytes();
        assertDoesNotThrow(() -> xamFileService.uploadToS3(testContent, "dummyKey.txt"));
    }

    @Test
    void testGenerateAndUploadXamFiles() {
        AssessmentSessionEntity sessionEntity = mock(AssessmentSessionEntity.class);
        when(sessionEntity.getSessionID()).thenReturn(UUID.randomUUID());
        when(sessionEntity.getCourseYear()).thenReturn("2023");
        when(sessionEntity.getCourseMonth()).thenReturn("09");
        when(stagedStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStagedAssessmentStudentStatusIn(eq(sessionEntity.getSessionID()), any(), eq(List.of("ACTIVE"))))
                .thenReturn(List.of());

        SchoolTombstone myEdSchool = mock(SchoolTombstone.class);
        when(myEdSchool.getVendorSourceSystemCode()).thenReturn("MYED");
        when(myEdSchool.getMincode()).thenReturn("MINCODE4");
        when(myEdSchool.getSchoolId()).thenReturn(UUID.randomUUID().toString());
        when(restUtils.getAllSchoolTombstones()).thenReturn(List.of(myEdSchool));

        doNothing().when(xamFileService).uploadToS3(any(byte[].class), any());
        xamFileService.generateAndUploadXamFiles(sessionEntity);

        verify(xamFileService).uploadToS3(any(byte[].class), eq("xam-files/MINCODE4-202309-Results.xam"));
    }
}
