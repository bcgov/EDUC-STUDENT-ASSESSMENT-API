package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.exception.PreconditionRequiredException;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.v1.TransferOnApprovalSagaData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DOARReportServiceTest extends BaseAssessmentAPITest {

    @Autowired
    private DOARReportService doarReportService;
    @Autowired
    private AssessmentSessionRepository assessmentSessionRepository;
    @Autowired
    private AssessmentFormRepository assessmentFormRepository;
    @Autowired
    private AssessmentRepository assessmentRepository;
    @Autowired
    private AssessmentQuestionRepository assessmentQuestionRepository;
    @Autowired
    StagedAssessmentStudentRepository stagedAssessmentStudentRepository;
    @Autowired
    private RestUtils restUtils;
    @Autowired
    private AssessmentComponentRepository assessmentComponentRepository;
    @Autowired
    AssessmentStudentRepository studentRepository;
    @Autowired
    private AssessmentStudentDOARCalculationRepository assessmentStudentDOARCalculationRepository;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        assessmentStudentDOARCalculationRepository.deleteAll();
        studentRepository.deleteAll();
        stagedAssessmentStudentRepository.deleteAll();
        assessmentQuestionRepository.deleteAll();
        assessmentFormRepository.deleteAll();
        assessmentRepository.deleteAll();
        assessmentSessionRepository.deleteAll();
    }

    @ParameterizedTest
    @CsvSource({
            "NMF10",
            "LTE10",
            "LTP10",
            "LTF12",
            "LTE12",
            "NME10"
    })
    void createAndPopulateDOARSummaryCalculations_NMF10(String typeCode) {
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentFormEntity formEntity = setData(typeCode, school);
        var comps = assessmentComponentRepository.findByAssessmentFormEntity_AssessmentFormID(formEntity.getAssessmentFormID());
        var comp1UUID = comps.get(0).getAssessmentComponentID();
        var comp2UUID = comps.get(1).getAssessmentComponentID();
        List<AssessmentQuestionEntity> ques1 = assessmentQuestionRepository.findByAssessmentComponentEntity_AssessmentComponentID(comp1UUID);
        List<AssessmentQuestionEntity> ques2 = assessmentQuestionRepository.findByAssessmentComponentEntity_AssessmentComponentID(comp2UUID);

        List<AssessmentQuestionEntity> ques = new ArrayList<>();
        ques.addAll(ques1);
        ques.addAll(ques2);

        List<AssessmentQuestionEntity> updatedQues = new ArrayList<>();

        for (int i = 1; i < ques.size(); i++) {
            if (i % 2 == 0) {
                ques.get(i).setTaskCode("P");
                ques.get(i).setClaimCode("I");
                ques.get(i).setCognitiveLevelCode("8");
            } else if (i % 7 == 0) {
                ques.get(i).setTaskCode("F");
                ques.get(i).setClaimCode("S");
                ques.get(i).setCognitiveLevelCode("7");
            } else {
                ques.get(i).setTaskCode("M");
                ques.get(i).setClaimCode("P");
                ques.get(i).setCognitiveLevelCode("9");
            }
            updatedQues.add(ques.get(i));
        }
        assessmentQuestionRepository.saveAll(updatedQues);

        var student = studentRepository.findByAssessmentFormIDIn(List.of(formEntity.getAssessmentFormID()));

        var sagaData = TransferOnApprovalSagaData
                .builder()
                .stagedStudentAssessmentID(UUID.randomUUID().toString())
                .studentID(String.valueOf(student.get(0).getStudentID()))
                .assessmentID(String.valueOf(student.get(0).getAssessmentEntity().getAssessmentID()))
                .build();
        doarReportService.createAndPopulateDOARSummaryCalculations(sagaData);

        var studentCalc = assessmentStudentDOARCalculationRepository.findByAssessmentStudentID(student.get(0).getAssessmentStudentID());
        assertThat(studentCalc).isPresent();
    }

    @Test
    void testDOARTotalForNME10Columns() {
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentFormEntity formEntity = setData("NME10", school);
        var comps = assessmentComponentRepository.findByAssessmentFormEntity_AssessmentFormID(formEntity.getAssessmentFormID());
        var comp1UUID = comps.get(0).getAssessmentComponentID();
        var comp2UUID = comps.get(1).getAssessmentComponentID();
        List<AssessmentQuestionEntity> ques1 = assessmentQuestionRepository.findByAssessmentComponentEntity_AssessmentComponentID(comp1UUID);
        List<AssessmentQuestionEntity> ques2 = assessmentQuestionRepository.findByAssessmentComponentEntity_AssessmentComponentID(comp2UUID);

        List<AssessmentQuestionEntity> ques = new ArrayList<>();
        ques.addAll(ques1);
        ques.addAll(ques2);

        List<AssessmentQuestionEntity> updatedQues = new ArrayList<>();

        for (int i = 1; i < ques.size(); i++) {
            if (i % 2 == 0) {
                ques.get(i).setTaskCode("P");
                ques.get(i).setClaimCode("I");
                ques.get(i).setCognitiveLevelCode("8");
            } else if (i % 7 == 0) {
                ques.get(i).setTaskCode("F");
                ques.get(i).setClaimCode("S");
                ques.get(i).setCognitiveLevelCode("7");
            } else {
                ques.get(i).setTaskCode("M");
                ques.get(i).setClaimCode("P");
                ques.get(i).setCognitiveLevelCode("9");
            }
            updatedQues.add(ques.get(i));
        }
        assessmentQuestionRepository.saveAll(updatedQues);
        var student = studentRepository.findByAssessmentFormIDIn(List.of(formEntity.getAssessmentFormID()));
        var sagaData = TransferOnApprovalSagaData
                .builder()
                .stagedStudentAssessmentID(UUID.randomUUID().toString())
                .studentID(String.valueOf(student.get(0).getStudentID()))
                .assessmentID(String.valueOf(student.get(0).getAssessmentEntity().getAssessmentID()))
                .build();
        doarReportService.createAndPopulateDOARSummaryCalculations(sagaData);

        var studentTotal = doarReportService
                .generateDetailedDOARBySchoolAndAssessmentType(formEntity.getAssessmentEntity().getAssessmentSessionEntity().getSessionID(), school, "NME10");
        assertThat(studentTotal).hasSize(1);
    }

    @Test
    void testDOARTotalForNMF10Columns() {
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentFormEntity formEntity = setData("NMF10", school);
        var comps = assessmentComponentRepository.findByAssessmentFormEntity_AssessmentFormID(formEntity.getAssessmentFormID());
        var comp1UUID = comps.get(0).getAssessmentComponentID();
        var comp2UUID = comps.get(1).getAssessmentComponentID();
        List<AssessmentQuestionEntity> ques1 = assessmentQuestionRepository.findByAssessmentComponentEntity_AssessmentComponentID(comp1UUID);
        List<AssessmentQuestionEntity> ques2 = assessmentQuestionRepository.findByAssessmentComponentEntity_AssessmentComponentID(comp2UUID);

        List<AssessmentQuestionEntity> ques = new ArrayList<>();
        ques.addAll(ques1);
        ques.addAll(ques2);

        List<AssessmentQuestionEntity> updatedQues = new ArrayList<>();

        for (int i = 1; i < ques.size(); i++) {
            ques.get(i).setTaskCode("P");
            ques.get(i).setClaimCode("I");
            ques.get(i).setCognitiveLevelCode("8");
            updatedQues.add(ques.get(i));
        }
        assessmentQuestionRepository.saveAll(updatedQues);
        var student = studentRepository.findByAssessmentFormIDIn(List.of(formEntity.getAssessmentFormID()));
        var sagaData = TransferOnApprovalSagaData
                .builder()
                .stagedStudentAssessmentID(UUID.randomUUID().toString())
                .studentID(String.valueOf(student.get(0).getStudentID()))
                .assessmentID(String.valueOf(student.get(0).getAssessmentEntity().getAssessmentID()))
                .build();
        doarReportService.createAndPopulateDOARSummaryCalculations(sagaData);

        var studentTotal = doarReportService
                .generateDetailedDOARBySchoolAndAssessmentType(formEntity.getAssessmentEntity().getAssessmentSessionEntity().getSessionID(), school, "NMF10");
        assertThat(studentTotal).hasSize(1);
        var row = studentTotal.get(0);
        assertThat(row.get(0)).isEqualTo("202501");
        assertThat(row.get(9)).isEqualTo("0.00");
    }

    @Test
    void testIsDetailedDOARAvailable_WhenStudentHasProficiencyScore_ReturnsTrue() {
        var school = this.createMockSchool();
        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, "NME10"));

        var student = createMockStudentEntity(savedAssessment);
        student.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        student.setProficiencyScore(3);
        studentRepository.save(student);

        boolean result = doarReportService.isDetailedDOARAvailable(savedSession.getSessionID(), UUID.fromString(school.getSchoolId()), "NME10");
        assertThat(result).isTrue();
    }

    @Test
    void testIsDetailedDOARAvailable_WhenStudentHasSpecialCaseX_ReturnsTrue() {
        var school = this.createMockSchool();
        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, "LTE10"));

        var student = createMockStudentEntity(savedAssessment);
        student.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        student.setProvincialSpecialCaseCode("X");
        studentRepository.save(student);

        boolean result = doarReportService.isDetailedDOARAvailable(savedSession.getSessionID(), UUID.fromString(school.getSchoolId()), "LTE10");
        assertThat(result).isTrue();
    }

    @Test
    void testIsDetailedDOARAvailable_WhenStudentHasSpecialCaseE_ReturnsTrue() {
        var school = this.createMockSchool();
        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, "LTF12"));

        var student = createMockStudentEntity(savedAssessment);
        student.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        student.setProvincialSpecialCaseCode("E");
        studentRepository.save(student);

        boolean result = doarReportService.isDetailedDOARAvailable(savedSession.getSessionID(), UUID.fromString(school.getSchoolId()), "LTF12");
        assertThat(result).isTrue();
    }

    @Test
    void testIsDetailedDOARAvailable_WhenNoStudentsWithResults_ReturnsFalse() {
        var school = this.createMockSchool();
        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        assessmentRepository.save(createMockAssessmentEntity(savedSession, "NMF10"));

        boolean result = doarReportService.isDetailedDOARAvailable(savedSession.getSessionID(), UUID.fromString(school.getSchoolId()), "NMF10");
        assertThat(result).isFalse();
    }

    @Test
    void testIsDetailedDOARAvailable_WhenAssessmentTypeNotInSession_ReturnsFalse() {
        var school = this.createMockSchool();
        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        assessmentRepository.save(createMockAssessmentEntity(savedSession, "NME10"));

        boolean result = doarReportService.isDetailedDOARAvailable(savedSession.getSessionID(), UUID.fromString(school.getSchoolId()), "LTP10");
        assertThat(result).isFalse();
    }

    @Test
    void testIsDetailedDOARAvailable_WhenStudentBelongsToDifferentSchool_ReturnsFalse() {
        var school = this.createMockSchool();
        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, "NME10"));

        var student = createMockStudentEntity(savedAssessment);
        student.setSchoolAtWriteSchoolID(UUID.randomUUID());
        student.setProficiencyScore(2);
        studentRepository.save(student);

        boolean result = doarReportService.isDetailedDOARAvailable(savedSession.getSessionID(), UUID.fromString(school.getSchoolId()), "NME10");
        assertThat(result).isFalse();
    }

    @Test
    void testIsDistrictDetailedDOARAvailable_WhenStudentHasProficiencyScore_ReturnsTrue() {
        var district = this.createMockDistrict();
        var school = this.createMockSchool();
        school.setDistrictId(district.getDistrictId());
        when(this.restUtils.getAllSchoolTombstones()).thenReturn(List.of(school));

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, "NME10"));

        var student = createMockStudentEntity(savedAssessment);
        student.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        student.setProficiencyScore(3);
        studentRepository.save(student);

        boolean result = doarReportService.isDistrictDetailedDOARAvailable(savedSession.getSessionID(), UUID.fromString(district.getDistrictId()), "NME10");
        assertThat(result).isTrue();
    }

    @Test
    void testIsDistrictDetailedDOARAvailable_WhenOnlyIndependentSchoolHasResults_ReturnsFalse() {
        var district = this.createMockDistrict();
        var independentSchool = this.createMockSchool();
        independentSchool.setDistrictId(district.getDistrictId());
        independentSchool.setIndependentAuthorityId(UUID.randomUUID().toString());
        independentSchool.setSchoolCategoryCode("INDEPEND");
        when(this.restUtils.getAllSchoolTombstones()).thenReturn(List.of(independentSchool));

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, "NME10"));

        var student = createMockStudentEntity(savedAssessment);
        student.setSchoolAtWriteSchoolID(UUID.fromString(independentSchool.getSchoolId()));
        student.setProficiencyScore(3);
        studentRepository.save(student);

        boolean result = doarReportService.isDistrictDetailedDOARAvailable(savedSession.getSessionID(), UUID.fromString(district.getDistrictId()), "NME10");
        assertThat(result).isFalse();
    }

    @Test
    void testIsDistrictDetailedDOARAvailable_WhenNoStudentsWithResults_ReturnsFalse() {
        var district = this.createMockDistrict();
        var school = this.createMockSchool();
        school.setDistrictId(district.getDistrictId());
        when(this.restUtils.getAllSchoolTombstones()).thenReturn(List.of(school));

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        assessmentRepository.save(createMockAssessmentEntity(savedSession, "NME10"));

        boolean result = doarReportService.isDistrictDetailedDOARAvailable(savedSession.getSessionID(), UUID.fromString(district.getDistrictId()), "NME10");
        assertThat(result).isFalse();
    }

    @Test
    void testGenerateDetailedDOARByDistrict_OrdersRowsByMincodeAscending() {
        var district = this.createMockDistrict();
        var schoolHighMincode = this.createMockSchool();
        schoolHighMincode.setDistrictId(district.getDistrictId());
        schoolHighMincode.setMincode("99999999");
        var schoolLowMincode = this.createMockSchool();
        schoolLowMincode.setDistrictId(district.getDistrictId());
        schoolLowMincode.setMincode("11111111");
        when(this.restUtils.getAllSchoolTombstones()).thenReturn(List.of(schoolHighMincode, schoolLowMincode));

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, "NME10"));

        var student1 = createMockStudentEntity(savedAssessment);
        student1.setSchoolAtWriteSchoolID(UUID.fromString(schoolHighMincode.getSchoolId()));
        student1.setProficiencyScore(2);
        var savedStudent1 = studentRepository.save(student1);
        assessmentStudentDOARCalculationRepository.save(createMockNMEDOARCalculationEntity(savedStudent1));

        var student2 = createMockStudentEntity(savedAssessment);
        student2.setSchoolAtWriteSchoolID(UUID.fromString(schoolLowMincode.getSchoolId()));
        student2.setProficiencyScore(3);
        var savedStudent2 = studentRepository.save(student2);
        assessmentStudentDOARCalculationRepository.save(createMockNMEDOARCalculationEntity(savedStudent2));

        var rows = doarReportService.generateDetailedDOARByDistrictAndAssessmentType(savedSession.getSessionID(), UUID.fromString(district.getDistrictId()), "NME10");

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get(1)).isEqualTo("11111111");
        assertThat(rows.get(1).get(1)).isEqualTo("99999999");
    }

    @Test
    void testGenerateDetailedDOARByDistrict_ExcludesStudentsFromOtherDistricts() {
        var district = this.createMockDistrict();
        var districtSchool = this.createMockSchool();
        districtSchool.setDistrictId(district.getDistrictId());
        var otherDistrictSchool = this.createMockSchool();
        when(this.restUtils.getAllSchoolTombstones()).thenReturn(List.of(districtSchool, otherDistrictSchool));

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, "NME10"));

        var student1 = createMockStudentEntity(savedAssessment);
        student1.setSchoolAtWriteSchoolID(UUID.fromString(districtSchool.getSchoolId()));
        student1.setProficiencyScore(2);
        var savedStudent1 = studentRepository.save(student1);
        assessmentStudentDOARCalculationRepository.save(createMockNMEDOARCalculationEntity(savedStudent1));

        var student2 = createMockStudentEntity(savedAssessment);
        student2.setSchoolAtWriteSchoolID(UUID.fromString(otherDistrictSchool.getSchoolId()));
        student2.setProficiencyScore(3);
        var savedStudent2 = studentRepository.save(student2);
        assessmentStudentDOARCalculationRepository.save(createMockNMEDOARCalculationEntity(savedStudent2));

        var rows = doarReportService.generateDetailedDOARByDistrictAndAssessmentType(savedSession.getSessionID(), UUID.fromString(district.getDistrictId()), "NME10");

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get(1)).isEqualTo(districtSchool.getMincode());
    }

    @Test
    void testGenerateDetailedDOARByDistrict_WhenNoResults_ThrowsPreconditionRequired() {
        var district = this.createMockDistrict();
        var school = this.createMockSchool();
        school.setDistrictId(district.getDistrictId());
        when(this.restUtils.getAllSchoolTombstones()).thenReturn(List.of(school));

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        assessmentRepository.save(createMockAssessmentEntity(savedSession, "NME10"));

        var sessionID = savedSession.getSessionID();
        var districtID = UUID.fromString(district.getDistrictId());
        assertThatThrownBy(() -> doarReportService.generateDetailedDOARByDistrictAndAssessmentType(sessionID, districtID, "NME10"))
                .isInstanceOf(PreconditionRequiredException.class);
    }

    private AssessmentStudentDOARCalculationEntity createMockNMEDOARCalculationEntity(AssessmentStudentEntity student) {
        return AssessmentStudentDOARCalculationEntity.builder()
                .assessmentStudentID(student.getAssessmentStudentID())
                .assessmentID(student.getAssessmentEntity().getAssessmentID())
                .taskPlan(BigDecimal.ONE)
                .taskEstimate(BigDecimal.ONE)
                .taskFair(BigDecimal.ONE)
                .taskModel(BigDecimal.ONE)
                .numeracyInterpret(BigDecimal.ONE)
                .numeracyApply(BigDecimal.ONE)
                .numeracySolve(BigDecimal.ONE)
                .numeracyAnalyze(BigDecimal.ONE)
                .dok1(BigDecimal.ONE)
                .dok2(BigDecimal.ONE)
                .dok3(BigDecimal.ONE)
                .createUser("TEST")
                .updateUser("TEST")
                .createDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();
    }

    private AssessmentFormEntity setData(String assessmentTypeCode, SchoolTombstone schoolTombstone) {
        var session = createMockSessionEntity();
        session.setCourseMonth("01");
        session.setCourseYear("2025");
        session.setSchoolYear("2024/2025");
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessmentEntity = assessmentRepository.save(createMockAssessmentEntity(savedSession, assessmentTypeCode));

        var savedForm = assessmentFormRepository.save(createMockAssessmentFormEntity(savedAssessmentEntity, "A"));

        var savedMultiComp = assessmentComponentRepository.save(createMockAssessmentComponentEntity(savedForm, "MUL_CHOICE", "NONE"));
        for(int i = 1;i < 29;i++) {
            assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedMultiComp, i, i));
        }

        var savedOpenEndedComp = assessmentComponentRepository.save(createMockAssessmentComponentEntity(savedForm, "OPEN_ENDED", "NONE"));
        var q1 = createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 2);
        q1.setMasterQuestionNumber(2);
        var oe1 = assessmentQuestionRepository.save(q1);

        var q2 = createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 3);
        q2.setMasterQuestionNumber(2);
        assessmentQuestionRepository.save(q2);

        var q3 = createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 5);
        q3.setMasterQuestionNumber(4);
        assessmentQuestionRepository.save(q3);

        var q4 = createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 6);
        q4.setMasterQuestionNumber(4);
        var oe4 = assessmentQuestionRepository.save(q4);

        var studentEntity1 = createMockStudentEntity(savedAssessmentEntity);
        studentEntity1.setSchoolAtWriteSchoolID(UUID.fromString(schoolTombstone.getSchoolId()));
        studentEntity1.setProficiencyScore(2);
        var componentEntity1 = createMockAssessmentStudentComponentEntity(studentEntity1, savedMultiComp.getAssessmentComponentID());
        var componentEntity2 = createMockAssessmentStudentComponentEntity(studentEntity1, savedOpenEndedComp.getAssessmentComponentID());

        var multiQues = assessmentQuestionRepository.findByAssessmentComponentEntity_AssessmentComponentID(savedMultiComp.getAssessmentComponentID());
        for(int i = 1;i < multiQues.size() ;i++) {
            if(i % 2 == 0) {
                componentEntity1.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(multiQues.get(i).getAssessmentQuestionID(), BigDecimal.ZERO, componentEntity1));
            } else {
                componentEntity1.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(multiQues.get(i).getAssessmentQuestionID(), BigDecimal.ONE, componentEntity1));

            }
        }

        componentEntity2.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(oe1.getAssessmentQuestionID(), BigDecimal.ONE, componentEntity2));
        componentEntity2.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(oe4.getAssessmentQuestionID(), new BigDecimal(9999), componentEntity2));

        studentEntity1.getAssessmentStudentComponentEntities().addAll(List.of(componentEntity1, componentEntity2));
        studentEntity1.setAssessmentFormID(savedForm.getAssessmentFormID());
        studentRepository.save(studentEntity1);

        return savedForm;
    }

}
