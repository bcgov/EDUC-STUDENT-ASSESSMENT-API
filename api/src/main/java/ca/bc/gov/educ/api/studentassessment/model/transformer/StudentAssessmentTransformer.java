package ca.bc.gov.educ.api.studentassessment.model.transformer;

import ca.bc.gov.educ.api.studentassessment.model.dto.StudentAssessment;
import ca.bc.gov.educ.api.studentassessment.model.entity.StudentAssessmentEntity;
import ca.bc.gov.educ.api.studentassessment.util.StudentAssessmentApiUtils;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class StudentAssessmentTransformer {

    @Autowired
    ModelMapper modelMapper;

    public StudentAssessment transformToDTO (StudentAssessmentEntity studentCourseEntity) {
        StudentAssessment studentCourse = modelMapper.map(studentCourseEntity, StudentAssessment.class);
        studentCourse.setSessionDate(StudentAssessmentApiUtils.formatDate(studentCourseEntity.getSessionDate()));

        return studentCourse;
    }

    public StudentAssessment transformToDTO ( Optional<StudentAssessmentEntity> courseAchievementEntity ) {
        StudentAssessmentEntity cae = new StudentAssessmentEntity();

        if (courseAchievementEntity.isPresent())
            cae = courseAchievementEntity.get();

        StudentAssessment courseAchievement = modelMapper.map(cae, StudentAssessment.class);
        courseAchievement.setSessionDate(StudentAssessmentApiUtils.formatDate(cae.getSessionDate()));

        return courseAchievement;
    }

    public List<StudentAssessment> transformToDTO (Iterable<StudentAssessmentEntity> courseAchievementEntities ) {

        List<StudentAssessment> courseAchievementList = new ArrayList<StudentAssessment>();

        for (StudentAssessmentEntity courseAchievementEntity : courseAchievementEntities) {
            StudentAssessment courseAchievement = new StudentAssessment();
            courseAchievement = modelMapper.map(courseAchievementEntity, StudentAssessment.class);
            courseAchievement.setSessionDate(StudentAssessmentApiUtils.formatDate(courseAchievementEntity.getSessionDate()));
            courseAchievementList.add(courseAchievement);
        }

        return courseAchievementList;
    }

    public StudentAssessmentEntity transformToEntity(StudentAssessment studentCourse) {
        StudentAssessmentEntity courseAchievementEntity = modelMapper.map(studentCourse, StudentAssessmentEntity.class);
        courseAchievementEntity.setSessionDate(StudentAssessmentApiUtils.parseDate(studentCourse.getSessionDate()));
        return courseAchievementEntity;
    }
}
