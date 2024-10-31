package ca.bc.gov.educ.eas.api.service.v1;

import ca.bc.gov.educ.eas.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentSessionTypeCodeCriteriaEntity;
import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentSessionTypeCodeCriteriaRepository;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class AssessmentService {

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentRepository assessmentRepository;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentSessionTypeCodeCriteriaRepository assessmentSessionTypeCodeCriteriaRepository;

    @Autowired
    public AssessmentService(AssessmentRepository assessmentRepository, AssessmentSessionTypeCodeCriteriaRepository assessmentSessionTypeCodeCriteriaRepository) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentSessionTypeCodeCriteriaRepository = assessmentSessionTypeCodeCriteriaRepository;
    }

    public void createAllAssessmentsForSchoolYear(List<SessionEntity> sessions){
        List<AssessmentEntity> newAssessments = new ArrayList<>();

        for (SessionEntity session : sessions){
            newAssessments.addAll(populateAssessmentsForSession(session));
        }

        assessmentRepository.saveAll(newAssessments);
    }

    private List<AssessmentEntity> populateAssessmentsForSession(SessionEntity session){
        List<AssessmentEntity> newAssessments = new ArrayList<>();

        List<AssessmentSessionTypeCodeCriteriaEntity> assessmentTypesForSession = assessmentSessionTypeCodeCriteriaRepository.findAllBySessionEndMonth(Integer.valueOf(session.getCourseMonth()), LocalDateTime.now(), LocalDateTime.now());

        for (AssessmentSessionTypeCodeCriteriaEntity assessmentType : assessmentTypesForSession){
            AssessmentEntity newAssessment = AssessmentEntity.builder()
                    .sessionEntity(session)
                    .assessmentTypeCode(assessmentType.getAssessmentTypeCodeEntity().getAssessmentTypeCode())
                    .createUser("EAS_API")
                    .createDate(LocalDateTime.now())
                    .updateUser("EAS_API")
                    .updateDate(LocalDateTime.now())
                    .build();

            newAssessments.add(newAssessment);
        }

        return newAssessments;
    }
}