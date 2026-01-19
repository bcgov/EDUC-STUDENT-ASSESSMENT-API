package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.StagedAssessmentStudentEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
        SELECT s.schoolAtWriteSchoolID
        FROM StagedAssessmentStudentEntity s
        WHERE s.assessmentEntity.assessmentSessionEntity.sessionID = :assessmentSessionID
        AND s.schoolAtWriteSchoolID is not null
        GROUP BY s.schoolAtWriteSchoolID
        HAVING COUNT(*) > 10
    """)
    List<UUID> getSchoolIDsOfSchoolsWithMoreThanStudentsInSession(UUID assessmentSessionID);

    @Query("""
        SELECT s.schoolAtWriteSchoolID
        FROM StagedAssessmentStudentEntity s
        WHERE s.assessmentEntity.assessmentSessionEntity.sessionID = :assessmentSessionID
        AND s.schoolAtWriteSchoolID is not null
        GROUP BY s.schoolAtWriteSchoolID
    """)
    List<UUID> getSchoolIDsOfSchoolsWithStudentsInSession(UUID assessmentSessionID);

    List<StagedAssessmentStudentEntity> findByStudentID(UUID studentID);

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

    @Modifying
    @Query(value = """
        update STAGED_ASSESSMENT_STUDENT 
        set STAGED_ASSESSMENT_STUDENT_STATUS = 'TRANSFER',
        UPDATE_USER = :updateUser,
        UPDATE_DATE = :updateDate
        where STAGED_ASSESSMENT_STUDENT_STATUS in('ACTIVE', 'MERGED')""", nativeQuery = true)
    void markStudentsForTransfer(@Param("updateUser") String updateUser, @Param("updateDate") LocalDateTime updateDate);

    @Modifying
    @Query(value = """
        DELETE FROM STAGED_ASSESSMENT_STUDENT 
        WHERE STAGED_ASSESSMENT_STUDENT_STATUS in ('NOPENFOUND')""", nativeQuery = true)
    void deleteStudentsWithPenIssues();

    @Query(value = """
        SELECT s
        FROM StagedAssessmentStudentEntity s
        WHERE s.stagedAssessmentStudentStatus = :status
        AND NOT EXISTS (SELECT 1 FROM AssessmentSagaEntity saga WHERE saga.status != 'COMPLETED' AND saga.stagedAssessmentStudentID = s.assessmentStudentID AND saga.assessmentID = s.assessmentEntity.assessmentID)
        ORDER BY s.updateDate DESC""")
    List<StagedAssessmentStudentEntity> findStudentIdsByStatusOrderByUpdateDate(String status, Pageable pageable);

    Optional<StagedAssessmentStudentEntity> findByAssessmentEntity_AssessmentIDAndStudentID(UUID assessmentID, UUID studentID);

    List<StagedAssessmentStudentEntity> findByAssessmentFormIDIn(List<UUID> assessmentFormIDs);

    List<StagedAssessmentStudentEntity> findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStagedAssessmentStudentStatusIn(UUID sessionID, UUID schoolAtWriteSchoolID, List<String> statuses);

    Optional<StagedAssessmentStudentEntity> findByAssessmentEntity_assessmentIDAndPen(UUID assessmentID, String pen);

    Optional<StagedAssessmentStudentEntity> findByAssessmentEntity_assessmentIDAndMergedPen(UUID assessmentID, String pen);
}
