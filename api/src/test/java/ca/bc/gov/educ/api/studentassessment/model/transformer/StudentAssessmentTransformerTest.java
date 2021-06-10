package ca.bc.gov.educ.api.studentassessment.model.transformer;

import ca.bc.gov.educ.api.studentassessment.model.dto.Assessment;
import ca.bc.gov.educ.api.studentassessment.model.dto.StudentAssessment;
import ca.bc.gov.educ.api.studentassessment.model.entity.StudentAssessmentEntity;
import ca.bc.gov.educ.api.studentassessment.model.entity.StudentAssessmentId;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
public class StudentAssessmentTransformerTest {

    @Mock
    ModelMapper modelMapper;

    @InjectMocks
    StudentAssessmentTransformer studentAssessmentTransformer;

    @Test
    public void testTransformToDTO() {
        Assessment assessment = new Assessment();
        assessment.setAssessmentCode("assmt");
        assessment.setAssessmentName("assessment name");
        assessment.setLanguage("en");
        assessment.setStartDate(new Date(System.currentTimeMillis() - 10000L));
        assessment.setEndDate(new Date(System.currentTimeMillis() + 10000L));

        StudentAssessment studentAssessment = new StudentAssessment();
        studentAssessment.setPen("123456789");
        studentAssessment.setAssessmentCode("assmt");
        studentAssessment.setAssessmentName("assessment name");
        studentAssessment.setMincodeAssessment("12345678");
        studentAssessment.setMincodeAssessmentName("Test school");
        studentAssessment.setAssessmentDetails(assessment);

        StudentAssessmentId studentAssessmentId = new StudentAssessmentId();
        studentAssessmentId.setPen(studentAssessment.getPen());
        studentAssessmentId.setAssessmentCode(studentAssessment.getAssessmentCode());

        StudentAssessmentEntity studentAssessmentEntity = new StudentAssessmentEntity();
        studentAssessmentEntity.setAssessmentKey(studentAssessmentId);
        studentAssessmentEntity.setMincodeAssessment(studentAssessment.getMincodeAssessment());

        Mockito.when(modelMapper.map(studentAssessmentEntity, StudentAssessment.class)).thenReturn(studentAssessment);
        var result = studentAssessmentTransformer.transformToDTO(studentAssessmentEntity);
        assertThat(result).isNotNull();
        assertThat(result.getAssessmentCode()).isEqualTo(studentAssessment.getAssessmentCode());
        assertThat(result.getPen()).isEqualTo(studentAssessment.getPen());
        assertThat(result.getMincodeAssessment()).isEqualTo(studentAssessment.getMincodeAssessment());
    }

    @Test
    public void testTransformOptionalToDTO() {
        Assessment assessment = new Assessment();
        assessment.setAssessmentCode("assmt");
        assessment.setAssessmentName("assessment name");
        assessment.setLanguage("en");
        assessment.setStartDate(new Date(System.currentTimeMillis() - 10000L));
        assessment.setEndDate(new Date(System.currentTimeMillis() + 10000L));

        StudentAssessment studentAssessment = new StudentAssessment();
        studentAssessment.setPen("123456789");
        studentAssessment.setAssessmentCode("assmt");
        studentAssessment.setAssessmentName("assessment name");
        studentAssessment.setMincodeAssessment("12345678");
        studentAssessment.setMincodeAssessmentName("Test school");
        studentAssessment.setAssessmentDetails(assessment);

        StudentAssessmentId studentAssessmentId = new StudentAssessmentId();
        studentAssessmentId.setPen(studentAssessment.getPen());
        studentAssessmentId.setAssessmentCode(studentAssessment.getAssessmentCode());

        StudentAssessmentEntity studentAssessmentEntity = new StudentAssessmentEntity();
        studentAssessmentEntity.setAssessmentKey(studentAssessmentId);
        studentAssessmentEntity.setMincodeAssessment(studentAssessment.getMincodeAssessment());

        Mockito.when(modelMapper.map(studentAssessmentEntity, StudentAssessment.class)).thenReturn(studentAssessment);
        var result = studentAssessmentTransformer.transformToDTO(Optional.of(studentAssessmentEntity));
        assertThat(result).isNotNull();
        assertThat(result.getAssessmentCode()).isEqualTo(studentAssessment.getAssessmentCode());
        assertThat(result.getPen()).isEqualTo(studentAssessment.getPen());
        assertThat(result.getMincodeAssessment()).isEqualTo(studentAssessment.getMincodeAssessment());
    }

    @Test
    public void testTransformToEntity() {
        Assessment assessment = new Assessment();
        assessment.setAssessmentCode("assmt");
        assessment.setAssessmentName("assessment name");
        assessment.setLanguage("en");
        assessment.setStartDate(new Date(System.currentTimeMillis() - 10000L));
        assessment.setEndDate(new Date(System.currentTimeMillis() + 10000L));

        StudentAssessment studentAssessment = new StudentAssessment();
        studentAssessment.setPen("123456789");
        studentAssessment.setAssessmentCode("assmt");
        studentAssessment.setAssessmentName("assessment name");
        studentAssessment.setMincodeAssessment("12345678");
        studentAssessment.setMincodeAssessmentName("Test school");
        studentAssessment.setAssessmentDetails(assessment);

        StudentAssessmentId studentAssessmentId = new StudentAssessmentId();
        studentAssessmentId.setPen(studentAssessment.getPen());
        studentAssessmentId.setAssessmentCode(studentAssessment.getAssessmentCode());

        StudentAssessmentEntity studentAssessmentEntity = new StudentAssessmentEntity();
        studentAssessmentEntity.setAssessmentKey(studentAssessmentId);
        studentAssessmentEntity.setMincodeAssessment(studentAssessment.getMincodeAssessment());

        Mockito.when(modelMapper.map(studentAssessment, StudentAssessmentEntity.class)).thenReturn(studentAssessmentEntity);
        var result = studentAssessmentTransformer.transformToEntity(studentAssessment);
        assertThat(result).isNotNull();
        assertThat(result.getAssessmentKey().getAssessmentCode()).isEqualTo(studentAssessment.getAssessmentCode());
        assertThat(result.getAssessmentKey().getPen()).isEqualTo(studentAssessment.getPen());
        assertThat(result.getMincodeAssessment()).isEqualTo(studentAssessment.getMincodeAssessment());
    }
}
