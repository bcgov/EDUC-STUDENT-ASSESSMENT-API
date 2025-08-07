package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentFormEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentFormRepository extends JpaRepository<AssessmentFormEntity, UUID> {

    @Query("SELECT form FROM AssessmentFormEntity form " +
            "WHERE form.assessmentEntity.assessmentSessionEntity.courseYear = :courseYear " +
            "AND form.assessmentEntity.assessmentSessionEntity.courseMonth = :courseMonth " +
            "AND form.assessmentEntity.assessmentTypeCode = :assessmentTypeCode " +
            "AND form.formCode = :formCode")
    Optional<AssessmentFormEntity> findFormBySessionAndAssessmentType(String courseYear, String courseMonth, String assessmentTypeCode, String formCode);
    Optional<AssessmentFormEntity> findByAssessmentEntity_AssessmentIDAndFormCode(UUID assessmentID, String formCode);
    List<AssessmentFormEntity> findAllByAssessmentEntity_AssessmentSessionEntity_SessionID(UUID sessionID);

}
