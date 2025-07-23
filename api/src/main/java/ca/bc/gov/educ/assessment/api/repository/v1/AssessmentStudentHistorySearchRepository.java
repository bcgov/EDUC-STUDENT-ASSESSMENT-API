package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentHistoryEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentHistorySearchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AssessmentStudentHistorySearchRepository extends JpaRepository<AssessmentStudentHistorySearchEntity, UUID>, JpaSpecificationExecutor<AssessmentStudentHistorySearchEntity> {

}
