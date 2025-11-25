package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.AssessmentRegistrationTotalsBySchoolResult;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.NumberOfAttemptsStudent;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.RegistrationSummaryResult;
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

    @Query("""
        SELECT DISTINCT s FROM AssessmentStudentEntity s
        LEFT JOIN FETCH s.assessmentEntity a
        LEFT JOIN FETCH a.assessmentSessionEntity
        LEFT JOIN FETCH a.assessmentForms af
        LEFT JOIN FETCH af.assessmentComponentEntities ac
        LEFT JOIN FETCH ac.assessmentQuestionEntities
        LEFT JOIN FETCH ac.assessmentChoiceEntities
        LEFT JOIN FETCH s.assessmentStudentComponentEntities asc
        LEFT JOIN FETCH asc.assessmentStudentAnswerEntities
        LEFT JOIN FETCH asc.assessmentStudentChoiceEntities
        WHERE s.assessmentStudentID = :assessmentStudentID
        AND a.assessmentID = :assessmentID
    """)
    Optional<AssessmentStudentEntity> findByIdWithAssessmentDetails(UUID assessmentStudentID, UUID assessmentID);

    List<AssessmentStudentEntity> findByAssessmentEntity_AssessmentIDAndSchoolAtWriteSchoolIDAndStudentStatusCode(UUID sessionID, UUID schoolAtWriteSchoolID, String studentStatusCode);

    List<AssessmentStudentEntity> findByAssessmentEntity_AssessmentIDInAndStudentID(List<UUID> assessmentIDs, UUID studentID);

    @Query("""
        SELECT s FROM AssessmentStudentEntity s
        WHERE s.assessmentEntity.assessmentID in (:assessmentIDs)
        AND s.studentID = :studentID
        AND (:assessmentStudentID IS NULL OR s.assessmentStudentID <> :assessmentStudentID)
    """)
    List<AssessmentStudentEntity> findByAssessmentEntity_AssessmentIDAndStudentIDAndAssessmentStudentIDIsNot(List<UUID> assessmentIDs, UUID studentID, UUID assessmentStudentID);
    
    @Modifying
    @Query(value = "update assessment_student " +
            "set DOWNLOAD_DATE = :downloadDate, " +
            "UPDATE_USER = :updateUser, " +
            "UPDATE_DATE = :updateDate " +
            "where STUDENT_STATUS_CODE = 'ACTIVE'" +
            "and assessment_student_id in (select student.assessment_student_id from assessment as a, assessment_student as student, assessment_session sess " +
            "    where a.session_id = sess.session_id " +
            "    and a.assessment_id = student.assessment_id " +
            "    and sess.session_id = :assessmentSessionID " +
            "    and (student.provincial_special_case_code not in ('E') or student.provincial_special_case_code is null))", nativeQuery=true)
    void updateDownloadDataAllByAssessmentSessionAndNoExemption(UUID assessmentSessionID, LocalDateTime downloadDate, String updateUser, LocalDateTime updateDate);

    List<AssessmentStudentEntity> findByStudentID(UUID studentID);

    @Query(value="""
          select stud.pen as pen, stud.assessmentEntity.assessmentTypeCode as assessmentTypeCode, count(*) as numberOfAttempts
          from AssessmentStudentEntity as stud
          where (stud.proficiencyScore is not null
          or stud.provincialSpecialCaseCode in ('X','Q'))
          group by stud.pen, stud.assessmentEntity.assessmentTypeCode""")
    List<NumberOfAttemptsStudent> findNumberOfAttemptsCounts();

    List<AssessmentStudentEntity> findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndStudentStatusCodeIn(UUID sessionID, List<String> statuses);

    List<AssessmentStudentEntity> findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStudentStatusCodeIn(UUID sessionID, UUID schoolAtWriteSchoolID, List<String> statuses);

    @Query(value="""
    select stud from AssessmentStudentEntity as stud
    where stud.studentID = :studentID
    and ((stud.proficiencyScore is not null and stud.proficiencyScore != 0)
    or stud.provincialSpecialCaseCode is not null)""")
    List<AssessmentStudentEntity> findAllWrittenAssessmentsForStudent(UUID studentID);

    @Query(value="""
    select count(*) from AssessmentStudentEntity as stud
    where stud.studentID = :studentID
    and stud.assessmentEntity.assessmentTypeCode in (:assessmentCodes)
    and (stud.proficiencyScore is not null
    or stud.provincialSpecialCaseCode in ('X','Q'))""")
    int findNumberOfAttemptsForStudent(UUID studentID, List<String> assessmentCodes);

    List<AssessmentStudentEntity> findByAssessmentFormIDIn(List<UUID> assessmentFormIDs); //Only used in tests so didn't add ACTIVE check

    @Query(value="""
        select stud.assessmentEntity.assessmentID as assessmentID,
        count(case when stud.gradeAtRegistration = '04' then 1 end) as grade4Count,
        count(case when stud.gradeAtRegistration = '05' then 1 end) as grade5Count,
        count(case when stud.gradeAtRegistration = '06' then 1 end) as grade6Count,
        count(case when stud.gradeAtRegistration = '07' then 1 end) as grade7Count,
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
        and stud.studentStatusCode = 'ACTIVE'
        group by stud.assessmentEntity.assessmentID
    """)
    List<RegistrationSummaryResult> getRegistrationSummaryByAssessmentIDsAndDownloadDateNull(List<UUID> assessmentIDs);


    @Query(value="""
        select stud.assessmentEntity.assessmentID as assessmentID,
        count(case when stud.gradeAtRegistration = '04' then 1 end) as grade4Count,
        count(case when stud.gradeAtRegistration = '05' then 1 end) as grade5Count,
        count(case when stud.gradeAtRegistration = '06' then 1 end) as grade6Count,
        count(case when stud.gradeAtRegistration = '07' then 1 end) as grade7Count,
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
        and stud.studentStatusCode = 'ACTIVE'
        and (stud.provincialSpecialCaseCode is null
         or stud.provincialSpecialCaseCode <> :provincialSpecialCaseCode)
        group by stud.assessmentEntity.assessmentID
    """)
    List<RegistrationSummaryResult> getRegistrationSummaryByAssessmentIDsAndDownloadDateNotNullAndProvincialSpecialCaseCodeNotAndStudentStatusCodeActive(List<UUID> assessmentIDs, String provincialSpecialCaseCode);

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
    List<AssessmentRegistrationTotalsBySchoolResult> getRegistrationSummaryByAssessmentIDsAndSchoolIDsAndDownloadDateIsNotNullAndProvincialSpecialCaseCodeNotAndStudentStatusCodeActive(List<UUID> assessmentIDs, String provincialSpecialCaseCode);


    @Query("""
        SELECT s FROM AssessmentStudentEntity s
        WHERE s.assessmentEntity.assessmentID = :assessmentID
        AND s.assessmentFormID IN (:formIDs)
        AND s.studentStatusCode = 'ACTIVE'
        ORDER BY s.createDate DESC
        LIMIT 1""")
    Optional<AssessmentStudentEntity> findByAssessmentIdAndAssessmentFormIdOrderByCreateDateDesc(UUID assessmentID, List<UUID> formIDs);
}
