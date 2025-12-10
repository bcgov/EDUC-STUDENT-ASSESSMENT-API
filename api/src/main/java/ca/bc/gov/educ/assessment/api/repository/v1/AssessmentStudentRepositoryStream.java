package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.stream.Stream;

public interface AssessmentStudentRepositoryStream {
    /**
     * @param spec The specification to filter results (can be null for all results)
     * @return Stream of AssessmentStudentEntity
     */
    Stream<AssessmentStudentEntity> streamAll(Specification<AssessmentStudentEntity> spec);
}

