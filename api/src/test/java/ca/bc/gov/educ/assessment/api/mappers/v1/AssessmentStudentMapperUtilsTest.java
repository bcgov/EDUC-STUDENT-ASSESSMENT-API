package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AssessmentStudentMapperUtilsTest {

    @ParameterizedTest
    @MethodSource("provideTestCases")
    void testSetWroteFlag(Integer proficiencyScore, String specialCaseCode, boolean expectedWroteFlag, String scenario) {
        // Arrange
        AssessmentStudentEntity entity = new AssessmentStudentEntity();
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
} 