package ca.bc.gov.educ.assessment.api.repository.v1;


import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.StagedAssessmentStudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StagedAssessmentStudentRepository extends JpaRepository<StagedAssessmentStudentEntity, UUID>, JpaSpecificationExecutor<StagedAssessmentStudentEntity> {
    @Query(value="""
    select stud.studentID from StagedAssessmentStudentEntity as stud
    where stud.assessmentEntity.assessmentSessionEntity.sessionID = :sessionID""")
    List<UUID> findAllStagedStudentsInSession(UUID sessionID);

    @Query("""
        SELECT s FROM StagedAssessmentStudentEntity s 
        WHERE s.assessmentEntity.assessmentID = :assessmentID 
        AND s.assessmentFormID IN (:formIDs) 
        ORDER BY s.createDate DESC 
        LIMIT 1 """)
    Optional<StagedAssessmentStudentEntity> findByAssessmentIdAndAssessmentFormIdOrderByCreateDateDesc(UUID assessmentID, List<UUID> formIDs);

    @Modifying
    @Query(value = "DELETE FROM STAGED_ASSESSMENT_STUDENT WHERE ASSESSMENT_ID = :assessmentID", nativeQuery = true)
    void deleteAllByAssessmentID(UUID assessmentID);
}
