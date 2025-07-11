package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentListItem;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AssessmentStudentListItemMapperIntegrationTest {

    private final AssessmentStudentListItemMapper mapper = AssessmentStudentListItemMapper.mapper;

    @ParameterizedTest
    @MethodSource("provideWroteFlagScenarios")
    void testToStructure_ShouldCalculateWroteFlagCorrectly(
            Integer proficiencyScore, String specialCaseCode, boolean expectedWroteFlag, String scenario) {
        // Given
        AssessmentStudentEntity entity = createMockAssessmentStudentEntity();
        entity.setProficiencyScore(proficiencyScore);
        entity.setProvincialSpecialCaseCode(specialCaseCode);

        // When
        AssessmentStudentListItem result = mapper.toStructure(entity);

        // Then
        assertNotNull(result);
        assertEquals(expectedWroteFlag, result.getWroteFlag(), "Failed for scenario: " + scenario);
        assertEquals(entity.getAssessmentEntity().getAssessmentID().toString(), result.getAssessmentID());
        assertEquals(entity.getAssessmentEntity().getAssessmentSessionEntity().getSessionID().toString(), result.getSessionID());
        assertEquals(entity.getAssessmentEntity().getAssessmentTypeCode(), result.getAssessmentTypeCode());
        assertEquals(entity.getAssessmentEntity().getAssessmentSessionEntity().getCourseMonth(), result.getCourseMonth());
        assertEquals(entity.getAssessmentEntity().getAssessmentSessionEntity().getCourseYear(), result.getCourseYear());
        assertEquals(entity.getAssessmentCenterSchoolID().toString(), result.getAssessmentCenterSchoolID());
        assertEquals(entity.getSurname(), result.getSurname());
        assertEquals(entity.getGivenName(), result.getGivenName());
    }

    private static Stream<Arguments> provideWroteFlagScenarios() {
        return Stream.of(
                // Valid scenarios (wroteFlag = true)
                Arguments.of(4, null, true, "Proficiency score 4"),
                Arguments.of(null, "Q", true, "Special case code Q"),
                Arguments.of(2, "X", true, "Both proficiency score and special case code X"),
                Arguments.of(1, "", true, "Proficiency score with empty special case code"),
                
                // Invalid scenarios (wroteFlag = false)
                Arguments.of(null, null, false, "Both null"),
                Arguments.of(null, "", false, "Null proficiency score, empty special case code"),
                Arguments.of(null, "   ", false, "Null proficiency score, whitespace special case code"),
                Arguments.of(null, "A", false, "Special case code A (Aegrotat)"),
                Arguments.of(null, "Z", false, "Invalid special case code Z")
        );
    }

    @ParameterizedTest
    @MethodSource("provideMappingScenarios")
    void testToStructure_ShouldMapAllFieldsCorrectly(
            String givenName, String surname, UUID assessmentCenterSchoolID, String scenario) {
        // Given
        AssessmentStudentEntity entity = createMockAssessmentStudentEntity();
        entity.setGivenName(givenName);
        entity.setSurname(surname);
        entity.setAssessmentCenterSchoolID(assessmentCenterSchoolID);
        String assessmentSchoolId = assessmentCenterSchoolID != null ? assessmentCenterSchoolID.toString() : null;

        // When
        AssessmentStudentListItem result = mapper.toStructure(entity);

        // Then
        assertNotNull(result);
        assertEquals(givenName, result.getGivenName(), "Failed for scenario: " + scenario);
        assertEquals(surname, result.getSurname(), "Failed for scenario: " + scenario);
        assertEquals(assessmentSchoolId, result.getAssessmentCenterSchoolID(), "Failed for scenario: " + scenario);
    }

    private static Stream<Arguments> provideMappingScenarios() {
        return Stream.of(
                Arguments.of("John", "Connor", UUID.randomUUID(), "Standard names"),
                Arguments.of("", "", null, "Empty names and null school ID"),
                Arguments.of(null, null, UUID.randomUUID(), "Null names")
        );
    }

    private AssessmentStudentEntity createMockAssessmentStudentEntity() {
        AssessmentSessionEntity session = AssessmentSessionEntity.builder()
                .sessionID(UUID.randomUUID())
                .courseYear("2024")
                .courseMonth("10")
                .build();

        AssessmentEntity assessment = AssessmentEntity.builder()
                .assessmentID(UUID.randomUUID())
                .assessmentSessionEntity(session)
                .assessmentTypeCode("LTP10")
                .build();

        return AssessmentStudentEntity.builder()
                .assessmentStudentID(UUID.randomUUID())
                .assessmentEntity(assessment)
                .studentID(UUID.randomUUID())
                .givenName("John")
                .surname("Doe")
                .pen("123456789")
                .schoolOfRecordSchoolID(UUID.randomUUID())
                .assessmentCenterSchoolID(UUID.randomUUID())
                .build();
    }
} 