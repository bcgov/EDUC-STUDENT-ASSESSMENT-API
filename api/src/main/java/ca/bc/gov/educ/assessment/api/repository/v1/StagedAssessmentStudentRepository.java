package ca.bc.gov.educ.assessment.api.repository.v1;

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
        UPDATE STAGED_ASSESSMENT_STUDENT
        SET STAGED_ASSESSMENT_STUDENT_STATUS = 
            CASE 
                WHEN STAGED_ASSESSMENT_STUDENT_STATUS IN ('MERGED', 'NOPENFOUND') THEN 'DELETE'
                WHEN STAGED_ASSESSMENT_STUDENT_STATUS NOT IN ('DELETE', 'TRANSFER') THEN 'TRANSFER'
                ELSE STAGED_ASSESSMENT_STUDENT_STATUS
            END,
            UPDATE_USER = :updateUser,
            UPDATE_DATE = :updateDate
        WHERE STAGED_ASSESSMENT_STUDENT_STATUS NOT IN ('DELETE', 'TRANSFER')""", nativeQuery = true)
    int updateAllStagedStudentsForTransferOrDelete(@Param("updateUser") String updateUser, @Param("updateDate") LocalDateTime updateDate);

    @Query(value = """
        SELECT s
        FROM StagedAssessmentStudentEntity s
        WHERE s.stagedAssessmentStudentStatus = :status
        ORDER BY s.updateDate DESC""")
    List<StagedAssessmentStudentEntity> findStudentIdsByStatusOrderByUpdateDate(String status, Pageable pageable);

    Optional<StagedAssessmentStudentEntity> findByAssessmentEntity_AssessmentIDAndStudentID(UUID assessmentID, UUID studentID);

    List<StagedAssessmentStudentEntity> findByAssessmentFormIDIn(List<UUID> assessmentFormIDs);

    List<StagedAssessmentStudentEntity> findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStagedAssessmentStudentStatusIn(UUID sessionID, UUID schoolAtWriteSchoolID, List<String> statuses);

    Optional<StagedAssessmentStudentEntity> findByAssessmentEntity_assessmentIDAndPen(UUID assessmentID, String pen);
}
