package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentListItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentStudentListItemMapperDecoratorTest {

    @Mock
    private AssessmentStudentListItemMapper delegate;

    @Test
    void testToStructure_SetsWroteFlagUsingSharedUtility() {
        // Arrange
        AssessmentStudentEntity entity = new AssessmentStudentEntity();
        entity.setProficiencyScore(85);
        entity.setProvincialSpecialCaseCode(null);

        AssessmentStudentListItem expectedItem = AssessmentStudentListItem.builder()
                .assessmentID("123")
                .build();

        when(delegate.toStructure(entity)).thenReturn(expectedItem);

        AssessmentStudentListItemMapperDecorator decorator = new AssessmentStudentListItemMapperDecorator(delegate) {};

        // Act
        AssessmentStudentListItem result = decorator.toStructure(entity);

        // Assert
        assertTrue(result.getWroteFlag());
        assertEquals(expectedItem, result); // Verify the original item is returned
    }

    @Test
    void testToStructure_WithSpecialCaseCode_SetsWroteFlagToTrue() {
        // Arrange
        AssessmentStudentEntity entity = new AssessmentStudentEntity();
        entity.setProficiencyScore(null);
        entity.setProvincialSpecialCaseCode("X");

        AssessmentStudentListItem expectedItem = AssessmentStudentListItem.builder()
                .assessmentID("123")
                .build();

        when(delegate.toStructure(entity)).thenReturn(expectedItem);

        AssessmentStudentListItemMapperDecorator decorator = new AssessmentStudentListItemMapperDecorator(delegate) {};

        // Act
        AssessmentStudentListItem result = decorator.toStructure(entity);

        // Assert
        assertTrue(result.getWroteFlag());
    }
} 