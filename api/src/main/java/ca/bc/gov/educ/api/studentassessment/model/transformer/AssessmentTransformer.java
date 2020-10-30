package ca.bc.gov.educ.api.studentassessment.model.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.bc.gov.educ.api.studentassessment.model.dto.Assessment;
import ca.bc.gov.educ.api.studentassessment.model.entity.AssessmentEntity;

@Component
public class AssessmentTransformer {

    @Autowired
    ModelMapper modelMapper;

    public Assessment transformToDTO (AssessmentEntity studentCourseEntity) {
        Assessment studentCourse = modelMapper.map(studentCourseEntity, Assessment.class);
        return studentCourse;
    }

    public Assessment transformToDTO ( Optional<AssessmentEntity> assessmentEntity ) {
        AssessmentEntity cae = new AssessmentEntity();

        if (assessmentEntity.isPresent())
            cae = assessmentEntity.get();

        Assessment assessment = modelMapper.map(cae, Assessment.class);
        return assessment;
    }

	public List<Assessment> transformToDTO (Iterable<AssessmentEntity> courseEntities ) {

        List<Assessment> courseList = new ArrayList<Assessment>();

        for (AssessmentEntity courseEntity : courseEntities) {
            Assessment course = new Assessment();
            course = modelMapper.map(courseEntity, Assessment.class);            
            courseList.add(course);
        }

        return courseList;
    }

    public AssessmentEntity transformToEntity(Assessment studentCourse) {
        AssessmentEntity courseAchievementEntity = modelMapper.map(studentCourse, AssessmentEntity.class);
        return courseAchievementEntity;
    }
}
