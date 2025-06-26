package ca.bc.gov.educ.assessment.api.repository.v1;


import ca.bc.gov.educ.assessment.api.model.v1.StagedAssessmentStudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StagedAssessmentStudentRepository extends JpaRepository<StagedAssessmentStudentEntity, UUID>, JpaSpecificationExecutor<StagedAssessmentStudentEntity> {

}
