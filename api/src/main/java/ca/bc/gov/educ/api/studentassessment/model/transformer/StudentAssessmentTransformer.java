package ca.bc.gov.educ.api.studentassessment.model.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.bc.gov.educ.api.studentassessment.model.dto.StudentAssessment;
import ca.bc.gov.educ.api.studentassessment.model.entity.StudentAssessmentEntity;
import ca.bc.gov.educ.api.studentassessment.util.StudentAssessmentApiUtils;

@Component
public class StudentAssessmentTransformer {

    @Autowired
    ModelMapper modelMapper;

    public StudentAssessment transformToDTO (StudentAssessmentEntity studentAssessmentEntity) {
        StudentAssessment studentAssessment = modelMapper.map(studentAssessmentEntity, StudentAssessment.class);
        return studentAssessment;
    }

    public StudentAssessment transformToDTO ( Optional<StudentAssessmentEntity> studentAssessmentEntity ) {
        StudentAssessmentEntity cae = new StudentAssessmentEntity();

        if (studentAssessmentEntity.isPresent())
            cae = studentAssessmentEntity.get();

        StudentAssessment studentAssessment = modelMapper.map(cae, StudentAssessment.class);
        return studentAssessment;
    }

    public List<StudentAssessment> transformToDTO (Iterable<StudentAssessmentEntity> studentAssessmentEntities ) {

        List<StudentAssessment> studentAssessmentList = new ArrayList<StudentAssessment>();

        for (StudentAssessmentEntity studentAssessmentEntity : studentAssessmentEntities) {
            StudentAssessment studentAssessment = new StudentAssessment();
            studentAssessment = modelMapper.map(studentAssessmentEntity, StudentAssessment.class);
            studentAssessment.setPen(studentAssessmentEntity.getAssessmentKey().getPen());
            studentAssessment.setAssessmentCode(studentAssessmentEntity.getAssessmentKey().getAssessmentCode());
            studentAssessment.setSessionDate(StudentAssessmentApiUtils.parseTraxDate(studentAssessmentEntity.getAssessmentKey().getSessionDate()));
            studentAssessmentList.add(studentAssessment);
        }

        return studentAssessmentList;
    }

    public StudentAssessmentEntity transformToEntity(StudentAssessment studentAssessment) {
        StudentAssessmentEntity studentAssessmentEntity = modelMapper.map(studentAssessment, StudentAssessmentEntity.class);
        return studentAssessmentEntity;
    }
}
