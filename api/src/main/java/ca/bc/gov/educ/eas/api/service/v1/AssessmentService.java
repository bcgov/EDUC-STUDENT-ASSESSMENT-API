package ca.bc.gov.educ.eas.api.service.v1;

import ca.bc.gov.educ.eas.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentCriteriaRepository;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AssessmentService {

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentRepository assessmentRepository;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentCriteriaRepository assessmentCriteriaRepository;

    @Autowired
    public AssessmentService(AssessmentRepository assessmentRepository, AssessmentCriteriaRepository assessmentCriteriaRepository) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentCriteriaRepository = assessmentCriteriaRepository;
    }

}