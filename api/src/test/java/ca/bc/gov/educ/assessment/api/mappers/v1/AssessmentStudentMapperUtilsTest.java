package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AssessmentStudentMapperUtilsTest {

    @ParameterizedTest
    @MethodSource("provideTestCases")
    void testSetWroteFlag(Integer proficiencyScore, String specialCaseCode, boolean expectedWroteFlag, String scenario) {
        // Arrange
        AssessmentStudentEntity entity = createMockAssessmentStudentEntity(
            createMockAssessmentEntity(createMockSessionEntity(), AssessmentTypeCodes.LTP10.getCode()), UUID.randomUUID()
        );
        entity.setProficiencyScore(proficiencyScore);
        entity.setProvincialSpecialCaseCode(specialCaseCode);

        AssessmentStudent assessmentStudent = AssessmentStudent.builder()
                .assessmentID("123")
                .build();

        // Act
        AssessmentStudentMapperUtils.setWroteFlag(entity, assessmentStudent);

        // Assert
        assertEquals(expectedWroteFlag, assessmentStudent.getWroteFlag(), 
                "Failed for scenario: " + scenario);
    }

    private static Stream<Arguments> provideTestCases() {
        return Stream.of(
                Arguments.of(85, null, true, "Has proficiency score"),
                Arguments.of(null, "X", true, "Has special case code X"),
                Arguments.of(null, "Q", true, "Has special case code Q"),
                Arguments.of(85, "X", true, "Has both proficiency score and special case code"),
                Arguments.of(null, null, false, "Has neither proficiency score nor special case code"),
                Arguments.of(null, "A", false, "Has special case code but not X or Q")
        );
    }

    @Test
    void testSetWroteFlag_WithAssessmentStudentListItem() {
        // Arrange
        AssessmentStudentEntity entity = new AssessmentStudentEntity();
        entity.setProficiencyScore(90);

        AssessmentStudent assessmentStudent = AssessmentStudent.builder()
                .assessmentID("123")
                .build();

        // Act
        AssessmentStudentMapperUtils.setWroteFlag(entity, assessmentStudent);

        // Assert
        assertTrue(assessmentStudent.getWroteFlag());
    }

    @ParameterizedTest
    @MethodSource("provideDidNotAttemptFlagTestCases")
    void testDidNotAttemptFlag_Parameterized(Integer proficiencyScore, String specialCaseCode, 
                                             LocalDateTime completionDate, boolean expectedDidNotAttemptFlag, String scenario) {
        // Arrange
        AssessmentSessionEntity sessionEntity = createMockSessionEntityWithCompletionDate(completionDate);
        AssessmentStudentEntity entity = createMockAssessmentStudentEntity(
            createMockAssessmentEntity(sessionEntity, AssessmentTypeCodes.LTP10.getCode()), UUID.randomUUID()
        );
        entity.setProficiencyScore(proficiencyScore);
        entity.setProvincialSpecialCaseCode(specialCaseCode);

        AssessmentStudent assessmentStudent = AssessmentStudent.builder()
                .assessmentID("123")
                .build();

        // Act
        AssessmentStudentMapperUtils.setWroteFlag(entity, assessmentStudent);

        // Assert
        assertEquals(expectedDidNotAttemptFlag, assessmentStudent.getDidNotAttemptFlag(), 
                "Failed for scenario: " + scenario);
    }

    private static Stream<Arguments> provideDidNotAttemptFlagTestCases() {
        LocalDateTime pastDate = LocalDateTime.now().minusDays(10);
        LocalDateTime futureDate = LocalDateTime.now().plusDays(10);
        
        return Stream.of(
                Arguments.of(null, null, pastDate, true, 
                        "No proficiency score, no special case code, completion date in past"),
                Arguments.of(null, null, null, false, 
                        "No proficiency score, no special case code, completion date is null"),
                Arguments.of(null, null, futureDate, false, 
                        "No proficiency score, no special case code, completion date in future"),
                Arguments.of(85, null, pastDate, false, 
                        "Has proficiency score, completion date in past"),
                Arguments.of(null, "X", pastDate, false, 
                        "Has special case code X, completion date in past"),
                Arguments.of(null, "Q", pastDate, false, 
                        "Has special case code Q, completion date in past"),
                Arguments.of(null, "A", pastDate, false, 
                        "Has other special case code, completion date in past"),
                Arguments.of(85, "X", pastDate, false, 
                        "Has both proficiency score and special case code, completion date in past")
        );
    }

    public AssessmentStudentEntity createMockAssessmentStudentEntity(AssessmentEntity assessment, UUID studentId) {
        return AssessmentStudentEntity.builder()
            .assessmentStudentID(UUID.randomUUID())
            .assessmentEntity(assessment)
            .studentID(studentId)
            .givenName("John")
            .surname("Doe")
            .pen("123456789")
            .schoolOfRecordSchoolID(UUID.randomUUID())
            .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .updateDate(LocalDateTime.now())
            .build();
    }

    public AssessmentSessionEntity createMockSessionEntity() {
        LocalDateTime currentDate = LocalDateTime.now();
        return AssessmentSessionEntity.builder()
            .sessionID(UUID.randomUUID())
            .schoolYear(String.valueOf(currentDate.getYear()))
            .courseYear(Integer.toString(currentDate.getYear()))
            .courseMonth(Integer.toString(currentDate.getMonthValue()))
            .activeFromDate(currentDate.minusMonths(2))
            .activeUntilDate(currentDate.plusMonths(2))
            .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .updateDate(LocalDateTime.now())
            .assessments(null)
            .build();
    }

    public AssessmentSessionEntity createMockSessionEntityWithCompletionDate(LocalDateTime completionDate) {
        LocalDateTime currentDate = LocalDateTime.now();
        return AssessmentSessionEntity.builder()
            .sessionID(UUID.randomUUID())
            .schoolYear(String.valueOf(currentDate.getYear()))
            .courseYear(Integer.toString(currentDate.getYear()))
            .courseMonth(Integer.toString(currentDate.getMonthValue()))
            .activeFromDate(currentDate.minusMonths(2))
            .activeUntilDate(currentDate.plusMonths(2))
            .completionDate(completionDate)
            .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .updateDate(LocalDateTime.now())
            .assessments(null)
            .build();
    }

    public AssessmentEntity createMockAssessmentEntity(AssessmentSessionEntity assessmentSessionEntity, String assessmentTypeCode) {
        return AssessmentEntity.builder()
            .assessmentSessionEntity(assessmentSessionEntity)
            .assessmentTypeCode(assessmentTypeCode)
            .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .updateDate(LocalDateTime.now())
            .build();
    }
} 