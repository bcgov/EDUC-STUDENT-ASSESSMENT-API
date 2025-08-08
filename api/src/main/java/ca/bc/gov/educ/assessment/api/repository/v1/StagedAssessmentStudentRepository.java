package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.StagedAssessmentStudentEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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
    @Query("""
        UPDATE StagedAssessmentStudentEntity s
        SET s.stagedAssessmentStudentStatus = :status,
            s.updateUser = :updateUser,
            s.updateDate = :updateDate""")
    int updateAllStagedAssessmentStudentStatus(String status, String updateUser, LocalDateTime updateDate);

    @Modifying
    @Query("""
        UPDATE StagedAssessmentStudentEntity s 
        SET s.stagedAssessmentStudentStatus = :newStatus, 
            s.updateUser = :updateUser, 
            s.updateDate = :updateDate 
        WHERE s.stagedAssessmentStudentStatus = :currentStatus 
        AND s.assessmentStudentID IN :studentIds""")
    int updateStagedAssessmentStudentStatusByIds(List<UUID> studentIds, String currentStatus, String newStatus, String updateUser, LocalDateTime updateDate);

    @Query(value = """
        SELECT s.assessmentStudentID 
        FROM StagedAssessmentStudentEntity s 
        WHERE s.stagedAssessmentStudentStatus = :status 
        ORDER BY s.updateDate DESC""")
    List<UUID> findStudentIdsByStatusOrderByUpdateDate(String status, Pageable pageable);

    List<StagedAssessmentStudentEntity> findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndStagedAssessmentStudentStatusIn(UUID sessionID, List<String> studentStatuses);
}
