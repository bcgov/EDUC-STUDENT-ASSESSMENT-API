package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.StagedAssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.SummaryByGradeQueryResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class StagedAssessmentStudentLightRepositoryTest extends BaseAssessmentAPITest {

    @Autowired
    StagedAssessmentStudentLightRepository stagedAssessmentStudentLightRepository;
    @Autowired
    StagedAssessmentStudentRepository stagedAssessmentStudentRepository;
    @Autowired
    AssessmentSessionRepository assessmentSessionRepository;
    @Autowired
    AssessmentRepository assessmentRepository;

    @AfterEach
    void after() {
        stagedAssessmentStudentRepository.deleteAll();
        assessmentRepository.deleteAll();
        assessmentSessionRepository.deleteAll();
    }

    @Test
    void testGetSummaryByGradeForSession_GivenStudentsWithNullGrade_ShouldCountThemInTotal() {
        var session = assessmentSessionRepository.save(createMockSessionEntity());
        var assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.NME10.getCode()));

        stagedAssessmentStudentRepository.saveAll(List.of(
                stagedStudent(assessment, null, 1),
                stagedStudent(assessment, null, 2),
                stagedStudent(assessment, "10", 3)));

        List<SummaryByGradeQueryResponse> rows = stagedAssessmentStudentLightRepository.getSummaryByGradeForSession(session.getSessionID());

        assertThat(rows).hasSize(2);

        var nullGradeRow = rows.stream().filter(r -> r.getGrade() == null).findFirst().orElseThrow();
        assertThat(nullGradeRow.getProfScore1()).isEqualTo(1);
        assertThat(nullGradeRow.getProfScore2()).isEqualTo(1);
        assertThat(nullGradeRow.getTotal()).isEqualTo(2);

        var gradeTenRow = rows.stream().filter(r -> "10".equals(r.getGrade())).findFirst().orElseThrow();
        assertThat(gradeTenRow.getProfScore3()).isEqualTo(1);
        assertThat(gradeTenRow.getTotal()).isEqualTo(1);
    }

    private StagedAssessmentStudentEntity stagedStudent(AssessmentEntity assessment, String grade, Integer proficiencyScore) {
        var student = createMockStagedStudentEntity(assessment);
        student.setGradeAtRegistration(grade);
        student.setProficiencyScore(proficiencyScore);
        return student;
    }
}
