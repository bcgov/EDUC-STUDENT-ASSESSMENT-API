package ca.bc.gov.educ.api.studentassessment.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ca.bc.gov.educ.api.studentassessment.model.entity.StudentAssessmentEntity;

@Repository
public interface StudentAssessmentRepository extends JpaRepository<StudentAssessmentEntity, UUID> {

	@Query("select c from StudentAssessmentEntity c where c.assessmentKey.pen=:pen")
    Iterable<StudentAssessmentEntity> findByPen(String pen);

}
