package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentListItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AssessmentStudentMapperConsistencyTest {

    private final AssessmentStudentMapper studentMapper = AssessmentStudentMapper.mapper;
    private final AssessmentStudentListItemMapper listItemMapper = AssessmentStudentListItemMapper.mapper;

    @Test
    void testBothMappers_ShouldProduceConsistentWroteFlag() {
        // Given
        AssessmentStudentEntity entity = createMockAssessmentStudentEntity();
        entity.setProficiencyScore(85);
        entity.setProvincialSpecialCaseCode("X");

        // When
        AssessmentStudent student = studentMapper.toStructure(entity);
        AssessmentStudentListItem listItem = listItemMapper.toStructure(entity);

        // Then
        assertTrue(student.getWroteFlag(), "AssessmentStudent wroteFlag should be true");
        assertTrue(listItem.getWroteFlag(), "AssessmentStudentListItem wroteFlag should be true");
    }

    @Test
    void testBothMappers_WithNoProficiencyScoreOrSpecialCase_ShouldProduceConsistentWroteFlag() {
        // Given
        AssessmentStudentEntity entity = createMockAssessmentStudentEntity();
        entity.setProficiencyScore(null);
        entity.setProvincialSpecialCaseCode(null);

        // When
        AssessmentStudent student = studentMapper.toStructure(entity);
        AssessmentStudentListItem listItem = listItemMapper.toStructure(entity);

        // Then
        assertFalse(student.getWroteFlag(), "AssessmentStudent wroteFlag should be false");
        assertFalse(listItem.getWroteFlag(), "AssessmentStudentListItem wroteFlag should be false");
    }

    @Test
    void testBothMappers_WithSpecialCaseCodeQ_ShouldProduceConsistentWroteFlag() {
        // Given
        AssessmentStudentEntity entity = createMockAssessmentStudentEntity();
        entity.setProficiencyScore(null);
        entity.setProvincialSpecialCaseCode("Q");

        // When
        AssessmentStudent student = studentMapper.toStructure(entity);
        AssessmentStudentListItem listItem = listItemMapper.toStructure(entity);

        // Then
        assertTrue(student.getWroteFlag(), "AssessmentStudent wroteFlag should be true");
        assertTrue(listItem.getWroteFlag(), "AssessmentStudentListItem wroteFlag should be true");
    }

    @Test
    void testBothMappers_WithInvalidSpecialCaseCode_ShouldProduceConsistentWroteFlag() {
        // Given
        AssessmentStudentEntity entity = createMockAssessmentStudentEntity();
        entity.setProficiencyScore(null);
        entity.setProvincialSpecialCaseCode("A"); // Invalid code

        // When
        AssessmentStudent student = studentMapper.toStructure(entity);
        AssessmentStudentListItem listItem = listItemMapper.toStructure(entity);

        // Then
        assertFalse(student.getWroteFlag(), "AssessmentStudent wroteFlag should be false");
        assertFalse(listItem.getWroteFlag(), "AssessmentStudentListItem wroteFlag should be false");
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