package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
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


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        studentRepository.deleteAll();
        stagedAssessmentStudentRepository.deleteAll();
        assessmentFormRepository.deleteAll();
        assessmentRepository.deleteAll();
        assessmentSessionRepository.deleteAll();
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

        var studentTotal = doarReportService
                .generateDetailedDOARBySchoolAndAssessmentType(formEntity.getAssessmentEntity().getAssessmentSessionEntity().getSessionID(), UUID.fromString(school.getSchoolId()), "NME10");
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

        var studentTotal = doarReportService
                .generateDetailedDOARBySchoolAndAssessmentType(formEntity.getAssessmentEntity().getAssessmentSessionEntity().getSessionID(), UUID.fromString(school.getSchoolId()), "NMF10");
        assertThat(studentTotal).hasSize(1);
        var row = studentTotal.get(0);
        assertThat(row.get(0)).isEqualTo("202501");
        assertThat(row.get(9)).isEqualTo("1.00");
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
