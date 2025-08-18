package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.RegistrationSummaryResult;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.AssessmentRegistrationTotalsBySchoolResult;
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

    List<AssessmentStudentEntity> findByAssessmentEntity_AssessmentIDAndSchoolAtWriteSchoolID(UUID sessionID, UUID schoolAtWriteSchoolID);

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

    List<AssessmentStudentEntity> findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndStudentStatusCodeIn(UUID sessionID, List<String> statuses);

    List<AssessmentStudentEntity> findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStudentStatusCodeIn(UUID sessionID, UUID schoolAtWriteSchoolID, List<String> statuses);

    @Query(value="""
    select count(*) from AssessmentStudentEntity as stud
    where stud.studentID = :studentID
    and stud.assessmentEntity.assessmentTypeCode in (:assessmentCodes)
    and (stud.proficiencyScore is not null
    or stud.provincialSpecialCaseCode in ('X','Q'))""")
    int findNumberOfAttemptsForStudent(UUID studentID, List<String> assessmentCodes);

    List<AssessmentStudentEntity> findByAssessmentFormIDIn(List<UUID> assessmentFormIDs);

    @Query(value="""
        select stud.assessmentEntity.assessmentID as assessmentID,
        count(case when stud.gradeAtRegistration = '08' then 1 end) as grade8Count,
        count(case when stud.gradeAtRegistration = '09' then 1 end) as grade9Count,
        count(case when stud.gradeAtRegistration = '10' then 1 end) as grade10Count,
        count(case when stud.gradeAtRegistration = '11' then 1 end) as grade11Count,
        count(case when stud.gradeAtRegistration = '12' then 1 end) as grade12Count,
        count(case when stud.gradeAtRegistration = 'AD' then 1 end) as gradeADCount,
        count(case when stud.gradeAtRegistration = 'OT' then 1 end) as gradeOTCount,
        count(case when stud.gradeAtRegistration = 'HS' then 1 end) as gradeHSCount,
        count(case when stud.gradeAtRegistration = 'AN' then 1 end) as gradeANCount,
        sum(case when stud.gradeAtRegistration in ('08','09','10','11','12','AD','OT','HS','AN') then 1 else 0 end) as total
        from AssessmentStudentEntity stud
        where stud.assessmentEntity.assessmentID in (:assessmentIDs)
        and stud.downloadDate is null
        group by stud.assessmentEntity.assessmentID
    """)
    List<RegistrationSummaryResult> getRegistrationSummaryByAssessmentIDsAndDownloadDateNull(List<UUID> assessmentIDs);


    @Query(value="""
        select stud.assessmentEntity.assessmentID as assessmentID,
        count(case when stud.gradeAtRegistration = '08' then 1 end) as grade8Count,
        count(case when stud.gradeAtRegistration = '09' then 1 end) as grade9Count,
        count(case when stud.gradeAtRegistration = '10' then 1 end) as grade10Count,
        count(case when stud.gradeAtRegistration = '11' then 1 end) as grade11Count,
        count(case when stud.gradeAtRegistration = '12' then 1 end) as grade12Count,
        count(case when stud.gradeAtRegistration = 'AD' then 1 end) as gradeADCount,
        count(case when stud.gradeAtRegistration = 'OT' then 1 end) as gradeOTCount,
        count(case when stud.gradeAtRegistration = 'HS' then 1 end) as gradeHSCount,
        count(case when stud.gradeAtRegistration = 'AN' then 1 end) as gradeANCount,
        sum(case when stud.gradeAtRegistration in ('08','09','10','11','12','AD','OT','HS','AN') then 1 else 0 end) as total
        from AssessmentStudentEntity stud
        where stud.assessmentEntity.assessmentID in (:assessmentIDs)
        and stud.downloadDate is not null
        and (stud.provincialSpecialCaseCode is null
         or stud.provincialSpecialCaseCode <> :provincialSpecialCaseCode)
        group by stud.assessmentEntity.assessmentID
    """)
    List<RegistrationSummaryResult> getRegistrationSummaryByAssessmentIDsAndDownloadDateNotNullAndProvincialSpecialCaseCodeNot(List<UUID> assessmentIDs, String provincialSpecialCaseCode);

    @Query(value="""
        select stud.assessmentEntity.assessmentID as assessmentID,
        stud.schoolOfRecordSchoolID as schoolOfRecordSchoolID,
        count(case when stud.gradeAtRegistration = '08' then 1 end) as grade8Count,
        count(case when stud.gradeAtRegistration = '09' then 1 end) as grade9Count,
        count(case when stud.gradeAtRegistration = '10' then 1 end) as grade10Count,
        count(case when stud.gradeAtRegistration = '11' then 1 end) as grade11Count,
        count(case when stud.gradeAtRegistration = '12' then 1 end) as grade12Count,
        count(case when stud.gradeAtRegistration = 'AD' then 1 end) as gradeADCount,
        count(case when stud.gradeAtRegistration = 'OT' then 1 end) as gradeOTCount,
        count(case when stud.gradeAtRegistration = 'HS' then 1 end) as gradeHSCount,
        count(case when stud.gradeAtRegistration = 'AN' then 1 end) as gradeANCount,
        count(case when stud.gradeAtRegistration not in ('08', '09', '10', '11', '12', 'AD', 'OT', 'HS', 'AN') or stud.gradeAtRegistration is null then 1 end) as blankGradeCount,
        count(*) as total
        from AssessmentStudentEntity stud
        where stud.assessmentEntity.assessmentID in (:assessmentIDs)
        and stud.studentStatusCode = 'ACTIVE'
        group by stud.assessmentEntity.assessmentID, stud.schoolOfRecordSchoolID
    """)
    List<AssessmentRegistrationTotalsBySchoolResult> getRegistrationSummaryByAssessmentIDsAndSchoolIDs(List<UUID> assessmentIDs);

    @Query(value="""
        select stud.assessmentEntity.assessmentID as assessmentID,
        stud.schoolOfRecordSchoolID as schoolOfRecordSchoolID,
        count(case when stud.gradeAtRegistration = '08' then 1 end) as grade8Count,
        count(case when stud.gradeAtRegistration = '09' then 1 end) as grade9Count,
        count(case when stud.gradeAtRegistration = '10' then 1 end) as grade10Count,
        count(case when stud.gradeAtRegistration = '11' then 1 end) as grade11Count,
        count(case when stud.gradeAtRegistration = '12' then 1 end) as grade12Count,
        count(case when stud.gradeAtRegistration = 'AD' then 1 end) as gradeADCount,
        count(case when stud.gradeAtRegistration = 'OT' then 1 end) as gradeOTCount,
        count(case when stud.gradeAtRegistration = 'HS' then 1 end) as gradeHSCount,
        count(case when stud.gradeAtRegistration = 'AN' then 1 end) as gradeANCount,
        count(case when stud.gradeAtRegistration not in ('08', '09', '10', '11', '12', 'AD', 'OT', 'HS', 'AN') or stud.gradeAtRegistration is null then 1 end) as blankGradeCount,
        count(*) as total
        from AssessmentStudentEntity stud
        where stud.assessmentEntity.assessmentID in (:assessmentIDs)
        and stud.downloadDate is not null
        and stud.studentStatusCode = 'ACTIVE'
        and (stud.provincialSpecialCaseCode is null
         or stud.provincialSpecialCaseCode <> :provincialSpecialCaseCode)
        group by stud.assessmentEntity.assessmentID, stud.schoolOfRecordSchoolID
    """)
    List<AssessmentRegistrationTotalsBySchoolResult> getRegistrationSummaryByAssessmentIDsAndSchoolIDsAndDownloadDateIsNotNullAndProvincialSpecialCaseCodeNot(List<UUID> assessmentIDs, String provincialSpecialCaseCode);


    @Query("""
        SELECT s FROM AssessmentStudentEntity s
        WHERE s.assessmentEntity.assessmentID = :assessmentID
        AND s.assessmentFormID IN (:formIDs)
        ORDER BY s.createDate DESC
        LIMIT 1""")
    Optional<AssessmentStudentEntity> findByAssessmentIdAndAssessmentFormIdOrderByCreateDateDesc(UUID assessmentID, List<UUID> formIDs);
}
