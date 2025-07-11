package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
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
public interface AssessmentStudentRepository extends JpaRepository<AssessmentStudentEntity, UUID>, JpaSpecificationExecutor<AssessmentStudentEntity> {
    Optional<AssessmentStudentEntity> findByAssessmentEntity_AssessmentIDAndStudentID(UUID assessmentID, UUID studentID);

    List<AssessmentStudentEntity> findByAssessmentEntity_AssessmentIDInAndStudentID(List<UUID> assessmentIDs, UUID studentID);

    Optional<AssessmentStudentEntity> findByAssessmentEntity_AssessmentIDAndStudentIDAndAssessmentStudentIDIsNot(UUID AssessmentID, UUID studentID, UUID assessmentStudentID);

    @Modifying
    @Query(value="""
    update AssessmentStudentEntity as stud
    set stud.downloadDate = :downloadDate,
        stud.updateUser = :updateUser,
        stud.updateDate = :updateDate
    where exists(select stud from AssessmentEntity as a, AssessmentStudentEntity as stud
    where a.assessmentSessionEntity.sessionID = :assessmentSessionID
    and stud.provincialSpecialCaseCode not in ('E'))""")
    void updateDownloadDataAllByAssessmentSessionAndNoExemption(UUID assessmentSessionID, LocalDateTime downloadDate, String updateUser, LocalDateTime updateDate);

    List<AssessmentStudentEntity> findByStudentID(UUID studentID);

    List<AssessmentStudentEntity> findByAssessmentEntity_AssessmentSessionEntity_SessionID(UUID sessionID);

    List<AssessmentStudentEntity> findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolID(UUID sessionID, UUID schoolAtWriteSchoolID);

    @Query(value="""
    select count(*) from AssessmentEntity as a, AssessmentStudentEntity as stud
    where a.assessmentID = stud.assessmentEntity.assessmentID
    and a.assessmentTypeCode in (:assessmentCodes)
    and stud.studentID = :studentID
    and (stud.proficiencyScore is not null
    or stud.provincialSpecialCaseCode in ('X','Q'))""")
    int findNumberOfAttemptsForStudent(UUID studentID, List<String> assessmentCodes);

    List<AssessmentStudentEntity> findByAssessmentFormIDIn(List<UUID> assessmentFormIDs);

    @Query("""
        SELECT s FROM AssessmentStudentEntity s 
        WHERE s.assessmentEntity.assessmentID = :assessmentID 
        AND s.assessmentFormID = :assessmentFormID 
        ORDER BY s.createDate DESC 
        LIMIT 1 """)
    Optional<AssessmentStudentEntity> findByAssessmentIdAndAssessmentFormIdOrderByCreateDateDesc(UUID assessmentID, UUID assessmentFormID);
}
