package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.StudentStatusCodes;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
class TransferStudentOrchestrationServiceTest extends BaseAssessmentAPITest {

    @Autowired
    private TransferStudentOrchestrationService transferStudentOrchestrationService;

    @Autowired
    private AssessmentEventRepository assessmentEventRepository;

    @Autowired
    private StagedAssessmentStudentRepository stagedAssessmentStudentRepository;

    @Autowired
    private AssessmentSessionRepository assessmentSessionRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private AssessmentStudentRepository assessmentStudentRepository;

    @Autowired
    private AssessmentStudentHistoryRepository assessmentStudentHistoryRepository;

    @Autowired
    private AssessmentFormRepository assessmentFormRepository;

    @Autowired
    private AssessmentComponentRepository assessmentComponentRepository;

    @Autowired
    private AssessmentQuestionRepository assessmentQuestionRepository;

    @Autowired
    private AssessmentStudentAnswerRepository assessmentStudentAnswerRepository;

    @Autowired
    private ComponentTypeCodeRepository componentTypeCodeRepository;

    @Autowired
    private ComponentSubTypeCodeRepository componentSubTypeCodeRepository;

    @Autowired
    private AdaptedAssessmentIndicatorCodeRepository adaptedAssessmentIndicatorCodeRepository;

    private AssessmentSessionEntity session;
    private AssessmentEntity assessment;

    @AfterEach
    void cleanup() {
        assessmentStudentAnswerRepository.deleteAll();
        assessmentStudentRepository.deleteAll();
        assessmentStudentHistoryRepository.deleteAll();
        stagedAssessmentStudentRepository.deleteAll();
        assessmentEventRepository.deleteAll();
        assessmentQuestionRepository.deleteAll();
        assessmentComponentRepository.deleteAll();
        assessmentFormRepository.deleteAll();
        componentSubTypeCodeRepository.deleteAll();
        componentTypeCodeRepository.deleteAll();
        adaptedAssessmentIndicatorCodeRepository.deleteAll();
        assessmentRepository.deleteAll();
        assessmentSessionRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        createComponentTypeCodes();
        createComponentSubTypeCodes();

        adaptedAssessmentIndicatorCodeRepository.save(AdaptedAssessmentIndicatorCodeEntity.builder()
            .adaptedAssessmentCode("ADAPT")
            .label("ADAPT")
            .description("Adapted assessment ADAPT")
            .displayOrder(10)
            .legacyCode("A")
            .createUser("TEST")
            .createDate(LocalDateTime.now())
            .updateUser("TEST")
            .updateDate(LocalDateTime.now())
            .effectiveDate(LocalDateTime.now())
            .expiryDate(LocalDateTime.now().plusYears(10))
            .build());

        session = assessmentSessionRepository.save(createMockSessionEntity());
        assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
    }

    private void createComponentTypeCodes() {
        ComponentTypeCodeEntity mulChoiceType = ComponentTypeCodeEntity.builder()
                .componentTypeCode("MUL_CHOICE")
                .label("Multiple Choice")
                .description("Multiple Choice or Selected Choice")
                .displayOrder(10)
                .effectiveDate(LocalDateTime.now().minusYears(1))
                .expiryDate(LocalDateTime.of(2099, 12, 31, 0, 0))
                .createUser("TEST")
                .createDate(LocalDateTime.now())
                .updateUser("TEST")
                .updateDate(LocalDateTime.now())
                .build();

        ComponentTypeCodeEntity openEndedType = ComponentTypeCodeEntity.builder()
                .componentTypeCode("OPEN_ENDED")
                .label("Open Ended")
                .description("Open Ended")
                .displayOrder(10)
                .effectiveDate(LocalDateTime.now().minusYears(1))
                .expiryDate(LocalDateTime.of(2099, 12, 31, 0, 0))
                .createUser("TEST")
                .createDate(LocalDateTime.now())
                .updateUser("TEST")
                .updateDate(LocalDateTime.now())
                .build();

        componentTypeCodeRepository.save(mulChoiceType);
        componentTypeCodeRepository.save(openEndedType);
    }

    private void createComponentSubTypeCodes() {
        ComponentSubTypeCodeEntity noneSubType = ComponentSubTypeCodeEntity.builder()
                .componentSubTypeCode("NONE")
                .label("None")
                .description("None")
                .displayOrder(10)
                .effectiveDate(LocalDateTime.now().minusYears(1))
                .expiryDate(LocalDateTime.of(2099, 12, 31, 0, 0))
                .createUser("TEST")
                .createDate(LocalDateTime.now())
                .updateUser("TEST")
                .updateDate(LocalDateTime.now())
                .build();

        componentSubTypeCodeRepository.save(noneSubType);
    }

    @Test
    void testGetStudentRegistrationEvents_shouldCreateEventAndReturnPair() {
        UUID studentID = UUID.randomUUID();

        var result = transferStudentOrchestrationService.getStudentRegistrationEvents(studentID);

        assertNotNull(result);
        assertNotNull(result.getLeft());
        assertNotNull(result.getRight());
        assertThat(result.getLeft()).hasSize(1);
        assertThat(result.getRight()).hasSize(1);
        assertThat(result.getRight().getFirst()).isEqualTo(studentID);

        var events = assessmentEventRepository.findAll();
        assertThat(events).hasSize(1);
    }

    @Test
    void testTransferStagedStudentToMainTables_withNewStudent_shouldCreateNewStudent() {
        StagedAssessmentStudentEntity stagedStudent = createStagedStudentWithComponents(assessment);
        stagedStudent = stagedAssessmentStudentRepository.save(stagedStudent);

        transferStudentOrchestrationService.transferStagedStudentToMainTables(stagedStudent.getAssessmentStudentID());

        assertThat(stagedAssessmentStudentRepository.findById(stagedStudent.getAssessmentStudentID())).isEmpty();

        var mainStudents = assessmentStudentRepository.findAll();
        assertThat(mainStudents).hasSize(1);

        var createdStudent = mainStudents.getFirst();
        var createdStudentWithDetails = assessmentStudentRepository.findByIdWithAssessmentDetails(createdStudent.getAssessmentStudentID(), assessment.getAssessmentID()).orElse(createdStudent);
        assertThat(createdStudentWithDetails.getStudentID()).isEqualTo(stagedStudent.getStudentID());
        assertThat(createdStudentWithDetails.getGivenName()).isEqualTo(stagedStudent.getGivenName());
        assertThat(createdStudentWithDetails.getSurname()).isEqualTo(stagedStudent.getSurname());
        assertThat(createdStudentWithDetails.getPen()).isEqualTo(stagedStudent.getPen());
        assertThat(createdStudentWithDetails.getStudentStatusCode()).isEqualTo(StudentStatusCodes.ACTIVE.getCode());

        assertThat(createdStudentWithDetails.getAssessmentStudentComponentEntities()).hasSize(1);
        var component = createdStudentWithDetails.getAssessmentStudentComponentEntities().iterator().next();
        assertThat(component.getAssessmentStudentAnswerEntities()).hasSize(1);
    }

    @Test
    void testTransferStagedStudentToMainTables_withExistingStudent_shouldUpdateExistingStudent() {
        AssessmentStudentEntity existingStudent = createMockStudentEntity(assessment);
        existingStudent = assessmentStudentRepository.save(existingStudent);

        StagedAssessmentStudentEntity stagedStudent = createStagedStudentWithComponents(assessment);
        stagedStudent.setStudentID(existingStudent.getStudentID());
        stagedStudent.setGivenName("UpdatedFirst");
        stagedStudent.setSurname("UpdatedLast");
        stagedStudent = stagedAssessmentStudentRepository.save(stagedStudent);

        UUID originalStudentId = existingStudent.getAssessmentStudentID();

        transferStudentOrchestrationService.transferStagedStudentToMainTables(stagedStudent.getAssessmentStudentID());

        assertThat(stagedAssessmentStudentRepository.findById(stagedStudent.getAssessmentStudentID())).isEmpty();

        var updatedStudentWithDetails = assessmentStudentRepository.findByIdWithAssessmentDetails(originalStudentId, assessment.getAssessmentID()).orElse(null);
        assertNotNull(updatedStudentWithDetails);
        assertThat(updatedStudentWithDetails.getGivenName()).isEqualTo("UpdatedFirst");
        assertThat(updatedStudentWithDetails.getSurname()).isEqualTo("UpdatedLast");
        assertThat(updatedStudentWithDetails.getStudentID()).isEqualTo(existingStudent.getStudentID());

        assertThat(updatedStudentWithDetails.getAssessmentStudentComponentEntities()).hasSize(1);
        var component = updatedStudentWithDetails.getAssessmentStudentComponentEntities().iterator().next();
        assertThat(component.getAssessmentStudentAnswerEntities()).hasSize(1);

        var allStudents = assessmentStudentRepository.findAll();
        assertThat(allStudents).hasSize(1);
    }

    @Test
    void testTransferStagedStudentToMainTables_shouldTransferAllStudentData() {
        StagedAssessmentStudentEntity stagedStudent = createStagedStudentWithComponents(assessment);
        stagedStudent.setSchoolAtWriteSchoolID(UUID.randomUUID());
        stagedStudent.setAssessmentCenterSchoolID(UUID.randomUUID());
        stagedStudent.setSchoolOfRecordSchoolID(UUID.randomUUID());
        stagedStudent.setLocalID("LOCAL123");
        stagedStudent.setGradeAtRegistration("10");
        stagedStudent.setLocalAssessmentID("ASSESS123");
        stagedStudent.setProficiencyScore(1);
        stagedStudent.setProvincialSpecialCaseCode("A");
        stagedStudent.setNumberOfAttempts(2);
        stagedStudent.setRawScore(BigDecimal.valueOf(78));
        stagedStudent.setMcTotal(BigDecimal.valueOf(45));
        stagedStudent.setOeTotal(BigDecimal.valueOf(33));
        stagedStudent.setAdaptedAssessmentCode("ADAPT");
        stagedStudent.setIrtScore("500");
        stagedStudent.setMarkingSession("SIX");

        stagedStudent = stagedAssessmentStudentRepository.save(stagedStudent);

        transferStudentOrchestrationService.transferStagedStudentToMainTables(stagedStudent.getAssessmentStudentID());

        var mainStudents = assessmentStudentRepository.findAll();
        assertThat(mainStudents).hasSize(1);

        var createdStudent = mainStudents.getFirst();
        assertThat(createdStudent.getAssessmentFormID()).isEqualTo(stagedStudent.getAssessmentFormID());
        assertThat(createdStudent.getSchoolAtWriteSchoolID()).isEqualTo(stagedStudent.getSchoolAtWriteSchoolID());
        assertThat(createdStudent.getAssessmentCenterSchoolID()).isEqualTo(stagedStudent.getAssessmentCenterSchoolID());
        assertThat(createdStudent.getSchoolOfRecordSchoolID()).isEqualTo(stagedStudent.getSchoolOfRecordSchoolID());
        assertThat(createdStudent.getLocalID()).isEqualTo(stagedStudent.getLocalID());
        assertThat(createdStudent.getGradeAtRegistration()).isEqualTo(stagedStudent.getGradeAtRegistration());
        assertThat(createdStudent.getLocalAssessmentID()).isEqualTo(stagedStudent.getLocalAssessmentID());
        assertThat(createdStudent.getProficiencyScore()).isEqualTo(stagedStudent.getProficiencyScore());
        assertThat(createdStudent.getProvincialSpecialCaseCode()).isEqualTo(stagedStudent.getProvincialSpecialCaseCode());
        assertThat(createdStudent.getNumberOfAttempts()).isEqualTo(stagedStudent.getNumberOfAttempts());
        assertThat(createdStudent.getRawScore().toBigInteger()).isEqualTo(stagedStudent.getRawScore().toBigInteger());
        assertThat(createdStudent.getMcTotal().toBigInteger()).isEqualTo(stagedStudent.getMcTotal().toBigInteger());
        assertThat(createdStudent.getOeTotal().toBigInteger()).isEqualTo(stagedStudent.getOeTotal().toBigInteger());
        assertThat(createdStudent.getAdaptedAssessmentCode()).isEqualTo(stagedStudent.getAdaptedAssessmentCode());
        assertThat(createdStudent.getIrtScore()).isEqualTo(stagedStudent.getIrtScore());
        assertThat(createdStudent.getMarkingSession()).isEqualTo(stagedStudent.getMarkingSession());
        assertThat(createdStudent.getStudentStatusCode()).isEqualTo(StudentStatusCodes.ACTIVE.getCode());
    }

    @Test
    void testTransferStagedStudentToMainTables_withMultipleComponentsAndAnswers_shouldTransferAll() {
        StagedAssessmentStudentEntity stagedStudent = createStagedStudentWithMultipleComponents(assessment);
        stagedStudent = stagedAssessmentStudentRepository.save(stagedStudent);

        transferStudentOrchestrationService.transferStagedStudentToMainTables(stagedStudent.getAssessmentStudentID());

        var mainStudents = assessmentStudentRepository.findAll();
        assertThat(mainStudents).hasSize(1);

        var createdStudent = mainStudents.getFirst();
        var createdStudentWithDetails2 = assessmentStudentRepository.findByIdWithAssessmentDetails(createdStudent.getAssessmentStudentID(), assessment.getAssessmentID()).orElse(createdStudent);
        assertThat(createdStudentWithDetails2.getAssessmentStudentComponentEntities()).hasSize(2);

        int totalAnswers = createdStudentWithDetails2.getAssessmentStudentComponentEntities().stream()
                .mapToInt(comp -> comp.getAssessmentStudentAnswerEntities().size())
                .sum();
        assertThat(totalAnswers).isEqualTo(3);
    }

    @Test
    void testTransferStagedStudentToMainTables_updatingExistingStudent_shouldClearOldComponentsAndAddNew() {
        AssessmentStudentEntity existingStudent = createMockStudentEntity(assessment);

        AssessmentFormEntity existingForm = assessmentFormRepository.save(createMockAssessmentFormEntity(assessment, "B"));
        AssessmentComponentEntity existingComponent = assessmentComponentRepository.save(
            createMockAssessmentComponentEntity(existingForm, "MUL_CHOICE", "NONE")
        );
        AssessmentQuestionEntity existingQuestion = assessmentQuestionRepository.save(
            createMockAssessmentQuestionEntity(existingComponent, 1, 1)
        );

        AssessmentStudentComponentEntity existingStudentComponent = createMockAssessmentStudentComponentEntity(existingStudent, existingComponent.getAssessmentComponentID());
        existingStudentComponent.getAssessmentStudentAnswerEntities().add(
                createMockAssessmentStudentAnswerEntity(existingQuestion.getAssessmentQuestionID(), BigDecimal.ONE, existingStudentComponent)
        );
        existingStudent.getAssessmentStudentComponentEntities().add(existingStudentComponent);
        existingStudent = assessmentStudentRepository.save(existingStudent);

        StagedAssessmentStudentEntity stagedStudent = createStagedStudentWithComponents(assessment);
        stagedStudent.setStudentID(existingStudent.getStudentID());
        stagedStudent = stagedAssessmentStudentRepository.save(stagedStudent);

        UUID originalComponentId = existingStudentComponent.getAssessmentComponentID();

        int componentsBeforeTransfer = assessmentStudentRepository.findByIdWithAssessmentDetails(
            existingStudent.getAssessmentStudentID(),
            assessment.getAssessmentID()
        ).get().getAssessmentStudentComponentEntities().size();
        assertThat(componentsBeforeTransfer).isEqualTo(1);

        transferStudentOrchestrationService.transferStagedStudentToMainTables(stagedStudent.getAssessmentStudentID());

        assessmentStudentRepository.flush();

        var updatedStudent = assessmentStudentRepository.findByIdWithAssessmentDetails(
            existingStudent.getAssessmentStudentID(),
            assessment.getAssessmentID()
        ).orElse(null);
        assertNotNull(updatedStudent);

        assertThat(updatedStudent.getAssessmentStudentComponentEntities()).isNotEmpty();
        boolean foundNewComponent = updatedStudent.getAssessmentStudentComponentEntities().stream()
            .anyMatch(c -> c.getAssessmentComponentID() != null && !c.getAssessmentComponentID().equals(originalComponentId) && "TEST".equals(c.getCreateUser()));
        assertTrue(foundNewComponent, "Expected at least one new component from staged student (createUser=TEST) with a different assessmentComponentID than the original");
    }

    @Test
    void testTransferStagedStudentToMainTables_shouldCreateHistoryRecord() {
        StagedAssessmentStudentEntity stagedStudent = createStagedStudentWithComponents(assessment);
        stagedStudent = stagedAssessmentStudentRepository.save(stagedStudent);

        transferStudentOrchestrationService.transferStagedStudentToMainTables(stagedStudent.getAssessmentStudentID());

        var historyRecords = assessmentStudentHistoryRepository.findAll();
        assertThat(historyRecords).hasSize(1);

        var historyRecord = historyRecords.get(0);
        var createdStudent = assessmentStudentRepository.findAll().get(0);
        assertThat(historyRecord.getAssessmentStudentID()).isEqualTo(createdStudent.getAssessmentStudentID());
    }

    @Test
    void testTransferStagedStudentToMainTables_withNonExistentStagedStudent_shouldThrowException() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(RuntimeException.class, () ->
            transferStudentOrchestrationService.transferStagedStudentToMainTables(nonExistentId)
        );
    }

    private StagedAssessmentStudentEntity createStagedStudentWithComponents(AssessmentEntity assessment) {
        StagedAssessmentStudentEntity stagedStudent = createMockStagedStudentEntity(assessment);

        AssessmentFormEntity assessmentForm = assessmentFormRepository.save(createMockAssessmentFormEntity(assessment, "A"));
        stagedStudent.setAssessmentFormID(assessmentForm.getAssessmentFormID());
        AssessmentComponentEntity assessmentComponent = assessmentComponentRepository.save(
            createMockAssessmentComponentEntity(assessmentForm, "MUL_CHOICE", "NONE")
        );

        AssessmentQuestionEntity assessmentQuestion = assessmentQuestionRepository.save(
            createMockAssessmentQuestionEntity(assessmentComponent, 1, 1)
        );

        StagedAssessmentStudentComponentEntity component = StagedAssessmentStudentComponentEntity.builder()
                .assessmentStudentComponentID(UUID.randomUUID())
                .stagedAssessmentStudentEntity(stagedStudent)
                .assessmentComponentID(assessmentComponent.getAssessmentComponentID())
                .choicePath("A")
                .createUser("TEST")
                .createDate(LocalDateTime.now())
                .updateUser("TEST")
                .updateDate(LocalDateTime.now())
                .build();

        StagedAssessmentStudentAnswerEntity answer = StagedAssessmentStudentAnswerEntity.builder()
                .assessmentStudentAnswerID(UUID.randomUUID())
                .stagedAssessmentStudentComponentEntity(component)
                .assessmentQuestionID(assessmentQuestion.getAssessmentQuestionID())
                .score(BigDecimal.valueOf(2))
                .createUser("TEST")
                .createDate(LocalDateTime.now())
                .updateUser("TEST")
                .updateDate(LocalDateTime.now())
                .build();

        Set<StagedAssessmentStudentAnswerEntity> answers = new HashSet<>();
        answers.add(answer);
        component.setStagedAssessmentStudentAnswerEntities(answers);

        Set<StagedAssessmentStudentComponentEntity> components = new HashSet<>();
        components.add(component);
        stagedStudent.setStagedAssessmentStudentComponentEntities(components);

        return stagedStudent;
    }

    private StagedAssessmentStudentEntity createStagedStudentWithMultipleComponents(AssessmentEntity assessment) {
        StagedAssessmentStudentEntity stagedStudent = createMockStagedStudentEntity(assessment);

        AssessmentFormEntity assessmentForm = assessmentFormRepository.save(createMockAssessmentFormEntity(assessment, "A"));
        stagedStudent.setAssessmentFormID(assessmentForm.getAssessmentFormID());
        AssessmentComponentEntity assessmentComponent1 = assessmentComponentRepository.save(
            createMockAssessmentComponentEntity(assessmentForm, "MUL_CHOICE", "NONE")
        );
        AssessmentComponentEntity assessmentComponent2 = assessmentComponentRepository.save(
            createMockAssessmentComponentEntity(assessmentForm, "OPEN_ENDED", "NONE")
        );

        AssessmentQuestionEntity assessmentQuestion1 = assessmentQuestionRepository.save(
            createMockAssessmentQuestionEntity(assessmentComponent1, 1, 1)
        );
        AssessmentQuestionEntity assessmentQuestion2 = assessmentQuestionRepository.save(
            createMockAssessmentQuestionEntity(assessmentComponent1, 2, 2)
        );
        AssessmentQuestionEntity assessmentQuestion3 = assessmentQuestionRepository.save(
            createMockAssessmentQuestionEntity(assessmentComponent2, 3, 3)
        );

        StagedAssessmentStudentComponentEntity component1 = StagedAssessmentStudentComponentEntity.builder()
                .assessmentStudentComponentID(UUID.randomUUID())
                .stagedAssessmentStudentEntity(stagedStudent)
                .assessmentComponentID(assessmentComponent1.getAssessmentComponentID())
                .choicePath("A")
                .createUser("TEST")
                .createDate(LocalDateTime.now())
                .updateUser("TEST")
                .updateDate(LocalDateTime.now())
                .build();

        StagedAssessmentStudentAnswerEntity answer1 = StagedAssessmentStudentAnswerEntity.builder()
                .assessmentStudentAnswerID(UUID.randomUUID())
                .stagedAssessmentStudentComponentEntity(component1)
                .assessmentQuestionID(assessmentQuestion1.getAssessmentQuestionID())
                .score(BigDecimal.valueOf(2))
                .createUser("TEST")
                .createDate(LocalDateTime.now())
                .updateUser("TEST")
                .updateDate(LocalDateTime.now())
                .build();

        StagedAssessmentStudentAnswerEntity answer2 = StagedAssessmentStudentAnswerEntity.builder()
                .assessmentStudentAnswerID(UUID.randomUUID())
                .stagedAssessmentStudentComponentEntity(component1)
                .assessmentQuestionID(assessmentQuestion2.getAssessmentQuestionID())
                .score(BigDecimal.valueOf(1))
                .createUser("TEST")
                .createDate(LocalDateTime.now())
                .updateUser("TEST")
                .updateDate(LocalDateTime.now())
                .build();

        Set<StagedAssessmentStudentAnswerEntity> answers1 = new HashSet<>();
        answers1.add(answer1);
        answers1.add(answer2);
        component1.setStagedAssessmentStudentAnswerEntities(answers1);

        StagedAssessmentStudentComponentEntity component2 = StagedAssessmentStudentComponentEntity.builder()
                .assessmentStudentComponentID(UUID.randomUUID())
                .stagedAssessmentStudentEntity(stagedStudent)
                .assessmentComponentID(assessmentComponent2.getAssessmentComponentID())
                .choicePath("B")
                .createUser("TEST")
                .createDate(LocalDateTime.now())
                .updateUser("TEST")
                .updateDate(LocalDateTime.now())
                .build();

        StagedAssessmentStudentAnswerEntity answer3 = StagedAssessmentStudentAnswerEntity.builder()
                .assessmentStudentAnswerID(UUID.randomUUID())
                .stagedAssessmentStudentComponentEntity(component2)
                .assessmentQuestionID(assessmentQuestion3.getAssessmentQuestionID())
                .score(BigDecimal.valueOf(3))
                .createUser("TEST")
                .createDate(LocalDateTime.now())
                .updateUser("TEST")
                .updateDate(LocalDateTime.now())
                .build();

        Set<StagedAssessmentStudentAnswerEntity> answers2 = new HashSet<>();
        answers2.add(answer3);
        component2.setStagedAssessmentStudentAnswerEntities(answers2);

        Set<StagedAssessmentStudentComponentEntity> components = new HashSet<>();
        components.add(component1);
        components.add(component2);
        stagedStudent.setStagedAssessmentStudentComponentEntities(components);

        return stagedStudent;
    }
}
