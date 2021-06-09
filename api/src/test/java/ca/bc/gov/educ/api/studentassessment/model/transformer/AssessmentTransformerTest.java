package ca.bc.gov.educ.api.studentassessment.model.transformer;

import ca.bc.gov.educ.api.studentassessment.model.dto.Assessment;
import ca.bc.gov.educ.api.studentassessment.model.entity.AssessmentEntity;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
public class AssessmentTransformerTest {
    @Mock
    ModelMapper modelMapper;

    @InjectMocks
    AssessmentTransformer assessmentTransformer;

    @Test
    public void testTransformToDTO() {
        AssessmentEntity assessmentEntity = new AssessmentEntity();
        assessmentEntity.setAssessmentCode("Test");
        assessmentEntity.setAssessmentName("Test Description");
        assessmentEntity.setLanguage("EN");

        Assessment assessment = new Assessment();
        assessment.setAssessmentCode(assessmentEntity.getAssessmentCode());
        assessment.setAssessmentName(assessmentEntity.getAssessmentName());
        assessment.setLanguage(assessmentEntity.getLanguage());

        Mockito.when(modelMapper.map(assessmentEntity, Assessment.class)).thenReturn(assessment);
        var result = assessmentTransformer.transformToDTO(assessmentEntity);
        assertThat(result).isNotNull();
        assertThat(result.getAssessmentCode()).isEqualTo(assessment.getAssessmentCode());
    }

    @Test
    public void testTransformOptionalToDTO() {
        AssessmentEntity assessmentEntity = new AssessmentEntity();
        assessmentEntity.setAssessmentCode("Test");
        assessmentEntity.setAssessmentName("Test Description");
        assessmentEntity.setLanguage("EN");

        Assessment assessment = new Assessment();
        assessment.setAssessmentCode(assessmentEntity.getAssessmentCode());
        assessment.setAssessmentName(assessmentEntity.getAssessmentName());
        assessment.setLanguage(assessmentEntity.getLanguage());

        Mockito.when(modelMapper.map(assessmentEntity, Assessment.class)).thenReturn(assessment);
        var result = assessmentTransformer.transformToDTO(Optional.of(assessmentEntity));
        assertThat(result).isNotNull();
        assertThat(result.getAssessmentCode()).isEqualTo(assessment.getAssessmentCode());
    }

    @Test
    public void testTransformIterableToDTO() {
        AssessmentEntity assessmentEntity = new AssessmentEntity();
        assessmentEntity.setAssessmentCode("Test");
        assessmentEntity.setAssessmentName("Test Description");
        assessmentEntity.setLanguage("EN");

        Assessment assessment = new Assessment();
        assessment.setAssessmentCode(assessmentEntity.getAssessmentCode());
        assessment.setAssessmentName(assessmentEntity.getAssessmentName());
        assessment.setLanguage(assessmentEntity.getLanguage());

        Mockito.when(modelMapper.map(assessmentEntity, Assessment.class)).thenReturn(assessment);
        var result = assessmentTransformer.transformToDTO(Arrays.asList(assessmentEntity));
        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.get(0).getAssessmentCode()).isEqualTo(assessment.getAssessmentCode());
    }

    @Test
    public void testTransformToEntity() {
        Assessment assessment = new Assessment();
        assessment.setAssessmentCode("Test");
        assessment.setAssessmentName("Test Description");
        assessment.setLanguage("EN");

        AssessmentEntity assessmentEntity = new AssessmentEntity();
        assessmentEntity.setAssessmentCode(assessment.getAssessmentCode());
        assessmentEntity.setAssessmentName(assessment.getAssessmentName());
        assessmentEntity.setLanguage(assessment.getLanguage());

        Mockito.when(modelMapper.map(assessment, AssessmentEntity.class)).thenReturn(assessmentEntity);
        var result = assessmentTransformer.transformToEntity(assessment);
        assertThat(result).isNotNull();
        assertThat(result.getAssessmentCode()).isEqualTo(assessmentEntity.getAssessmentCode());
    }

}
