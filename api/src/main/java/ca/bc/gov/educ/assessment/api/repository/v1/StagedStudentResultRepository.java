package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.StagedAssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.StagedStudentResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StagedStudentResultRepository extends JpaRepository<StagedStudentResultEntity, UUID>, JpaSpecificationExecutor<StagedStudentResultEntity> {

    @Query(value="""
    SELECT stud FROM StagedStudentResultEntity stud WHERE stud.stagedStudentResultID
    NOT IN (SELECT saga.stagedStudentResultID FROM AssessmentSagaEntity saga WHERE saga.status != 'COMPLETED'
    AND saga.stagedStudentResultID IS NOT NULL)
    AND stud.stagedStudentResultStatus = 'LOADED'
    order by stud.createDate
    LIMIT :numberOfStudentsToProcess""")
    List<StagedStudentResultEntity> findTopLoadedStudentForProcessing(String numberOfStudentsToProcess);

    @Transactional
    @Modifying
    @Query("DELETE FROM StagedStudentResultEntity WHERE stagedStudentResultStatus = 'COMPLETED'")
    void deleteResultWithStatusCompleted();

    @Query("""
        SELECT s FROM StagedStudentResultEntity s
        WHERE s.assessmentEntity.assessmentID = :assessmentID
        AND s.stagedStudentResultStatus = 'LOADED'
        ORDER BY s.createDate DESC
        LIMIT 1""")
    Optional<StagedStudentResultEntity> findByAssessmentIdAndStagedStudentResultStatusOrderByCreateDateDesc(UUID assessmentID);

}
