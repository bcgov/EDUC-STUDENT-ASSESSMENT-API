package ca.bc.gov.educ.assessment.api.batch.service;

import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.service.v1.CodeTableService;
import ca.bc.gov.educ.assessment.api.service.v1.DOARReportService;
import ca.bc.gov.educ.assessment.api.service.v1.StudentAssessmentResultService;
import ca.bc.gov.educ.assessment.api.struct.external.grad.v1.GradStudentRecord;
import ca.bc.gov.educ.assessment.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentResultFileUpload;
import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentResultDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssessmentResultServiceMergeTest {

    @Mock
    private AssessmentSessionRepository assessmentSessionRepository;
    @Mock
    private StagedAssessmentStudentRepository stagedAssessmentStudentRepository;
    @Mock
    private AssessmentStudentRepository assessmentStudentRepository;
    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private CodeTableService codeTableService;
    @Mock
    private RestUtils restUtils;
    @Mock
    private StagedStudentResultRepository stagedStudentResultRepository;
    @Mock
    private StudentAssessmentResultService studentAssessmentResultService;
    @Mock
    private DOARReportService doarReportService;
    @Mock
    private AssessmentStudentDOARCalculationRepository assessmentStudentDOARCalculationRepository;

    @InjectMocks
    private AssessmentResultService assessmentResultService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void singleLevelMerge_resolvesTrueStudent() throws Exception {
        UUID mergedId = UUID.randomUUID();
        UUID trueId = UUID.randomUUID();

        Student merged = Student.builder()
                .studentID(mergedId.toString())
                .pen("111111111")
                .statusCode("M")
                .trueStudentID(trueId.toString())
                .legalFirstName("A").legalLastName("B").dob("2000-01-01").sexCode("M").emailVerified("Y")
                .build();
        Student trueStudent = Student.builder()
                .studentID(trueId.toString())
                .pen("999999999")
                .statusCode("A")
                .legalFirstName("C").legalLastName("D").dob("2000-01-01").sexCode("M").emailVerified("Y")
                .build();

        AssessmentSessionEntity session = new AssessmentSessionEntity();
        session.setSessionID(UUID.randomUUID());
        session.setCompletionDate(LocalDateTime.now());
        AssessmentEntity assessment = new AssessmentEntity();
        assessment.setAssessmentID(UUID.randomUUID());
        AssessmentFormEntity form = new AssessmentFormEntity();
        form.setAssessmentFormID(UUID.randomUUID());
        AssessmentResultDetails details = new AssessmentResultDetails();
        details.setPen("111111111");
        AssessmentResultFileUpload fileUpload = new AssessmentResultFileUpload();
        fileUpload.setCreateUser("U");
        fileUpload.setUpdateUser("U");
        details.setComponentType("1");
        AssessmentStudentEntity assessmentStudent = new AssessmentStudentEntity();
        assessmentStudent.setAssessmentEntity(assessment);


        when(restUtils.getStudentByPEN(any(), eq("111111111"))).thenReturn(Optional.of(merged));
        when(restUtils.getStudents(any(), anySet())).thenReturn(List.of(trueStudent));
        when(assessmentStudentRepository.findByAssessmentEntity_AssessmentIDAndStudentID(any(), any())).thenReturn(Optional.empty());
        when(restUtils.getGradStudentRecordByStudentID(any(), eq(trueId))).thenReturn(Optional.empty());
        when(studentAssessmentResultService.createNewAssessmentStudentEntity(any(), any(), any(), any(), any()))
                .thenReturn(assessmentStudent);
        when(assessmentStudentRepository.save(any())).thenReturn(assessmentStudent);
        when(doarReportService.prepareLTEDOARSummaryEntity(any(), any(), any())).thenReturn(new AssessmentStudentDOARCalculationEntity());
        when(studentAssessmentResultService.setAssessmentStudentTotals(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);

        invokeCreate(List.of(details), fileUpload, session, assessment, form);

        verify(restUtils, times(1)).getStudents(any(), anySet());
        verify(studentAssessmentResultService).createNewAssessmentStudentEntity(any(), studentCaptor.capture(), any(), any(), any());
        assertEquals(trueId.toString(), studentCaptor.getValue().getStudentID());
    }

    @Test
    void twoLevelMerge_resolvesFinalStudent() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID finalId = UUID.randomUUID();

        Student s1 = Student.builder()
                .studentID(id1.toString()).pen("111111111").statusCode("M").trueStudentID(id2.toString())
                .legalFirstName("A").legalLastName("B").dob("2000-01-01").sexCode("M").emailVerified("Y")
                .build();
        Student s2 = Student.builder()
                .studentID(id2.toString()).pen("222222222").statusCode("M").trueStudentID(finalId.toString())
                .legalFirstName("C").legalLastName("D").dob("2000-01-01").sexCode("M").emailVerified("Y")
                .build();
        Student sf = Student.builder()
                .studentID(finalId.toString()).pen("999999999").statusCode("A")
                .legalFirstName("E").legalLastName("F").dob("2000-01-01").sexCode("M").emailVerified("Y")
                .build();

        AssessmentSessionEntity session = new AssessmentSessionEntity();
        session.setSessionID(UUID.randomUUID());
        session.setCompletionDate(LocalDateTime.now());
        AssessmentEntity assessment = new AssessmentEntity();
        assessment.setAssessmentID(UUID.randomUUID());
        AssessmentFormEntity form = new AssessmentFormEntity();
        form.setAssessmentFormID(UUID.randomUUID());
        AssessmentResultDetails details = new AssessmentResultDetails();
        details.setPen("111111111");
        AssessmentResultFileUpload fileUpload = new AssessmentResultFileUpload();
        fileUpload.setCreateUser("U");
        fileUpload.setUpdateUser("U");
        details.setComponentType("1");
        AssessmentStudentEntity assessmentStudent = new AssessmentStudentEntity();
        assessmentStudent.setAssessmentEntity(assessment);

        when(restUtils.getStudentByPEN(any(), eq("111111111"))).thenReturn(Optional.of(s1));
        when(restUtils.getStudents(any(), anySet()))
                .thenReturn(List.of(s2))
                .thenReturn(List.of(sf));
        when(assessmentStudentRepository.findByAssessmentEntity_AssessmentIDAndStudentID(any(), any())).thenReturn(Optional.empty());
        when(restUtils.getGradStudentRecordByStudentID(any(), eq(finalId))).thenReturn(Optional.empty());
        when(studentAssessmentResultService.createNewAssessmentStudentEntity(any(), any(), any(), any(), any()))
                .thenReturn(new AssessmentStudentEntity());
        when(assessmentStudentRepository.save(any())).thenReturn(assessmentStudent);
        when(doarReportService.prepareLTEDOARSummaryEntity(any(), any(), any())).thenReturn(new AssessmentStudentDOARCalculationEntity());
        when(studentAssessmentResultService.setAssessmentStudentTotals(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);

        invokeCreate(List.of(details), fileUpload, session, assessment, form);

        verify(restUtils, times(2)).getStudents(any(), anySet());
        verify(studentAssessmentResultService).createNewAssessmentStudentEntity(any(), studentCaptor.capture(), any(), any(), any());
        assertEquals(finalId.toString(), studentCaptor.getValue().getStudentID());
    }

    private void invokeCreate(List<AssessmentResultDetails> details,
                              AssessmentResultFileUpload fileUpload,
                              AssessmentSessionEntity session,
                              AssessmentEntity assessment,
                              AssessmentFormEntity form) throws Exception {
        Method m = AssessmentResultService.class.getDeclaredMethod(
                "createStudentRecordForCorrectionFile",
                List.class,
                AssessmentResultFileUpload.class,
                AssessmentSessionEntity.class,
                AssessmentEntity.class,
                AssessmentFormEntity.class
        );
        m.setAccessible(true);
        m.invoke(assessmentResultService, details, fileUpload, session, assessment, form);
    }
}


