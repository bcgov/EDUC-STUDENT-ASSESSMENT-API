package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentLightEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.SummaryByFormQueryResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.SummaryByGradeQueryResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentStudentLightRepository extends JpaRepository<AssessmentStudentLightEntity, UUID>, JpaSpecificationExecutor<AssessmentStudentEntity> {

    List<AssessmentStudentLightEntity> findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndStudentStatusCode(UUID sessionID, String studentStatusCode);

    @Query("""
    select stud from AssessmentStudentLightEntity stud
    where stud.assessmentEntity.assessmentSessionEntity.sessionID = :sessionID
    and stud.studentStatusCode = :studentStatusCode
    and (stud.proficiencyScore is not null
         or stud.provincialSpecialCaseCode = :provincialSpecialCaseCode)
    """)
    List<AssessmentStudentLightEntity> findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndStudentStatusCodeAndProficiencyScoreIsNotNullOrProvincialSpecialCaseCode(UUID sessionID, String studentStatusCode, String provincialSpecialCaseCode);

    @Query("""
    select stud from AssessmentStudentLightEntity stud
    where stud.assessmentEntity.assessmentTypeCode = :assessmentTypeCode
    and stud.studentStatusCode = 'ACTIVE'
    and stud.assessmentEntity.assessmentSessionEntity.sessionID = :sessionID
    and (stud.proficiencyScore is not null
         or stud.provincialSpecialCaseCode is not null)
    """)
    List<AssessmentStudentLightEntity> findByAssessmentTypeCodeAndSessionIDAndProficiencyScoreNotNullOrProvincialSpecialCaseNotNull(String assessmentTypeCode, UUID sessionID);

    @Query("""
    select stud from AssessmentStudentLightEntity stud
    where stud.assessmentEntity.assessmentSessionEntity.sessionID = :sessionID
    and stud.downloadDate is not null
    and (stud.provincialSpecialCaseCode is null OR stud.provincialSpecialCaseCode != :provincialSpecialCaseCode)
    and stud.studentStatusCode = :studentStatusCode
    """)
    List<AssessmentStudentLightEntity> findByDownloadDateIsNotNullAndProvincialSpecialCaseCodeNotAndStudentStatusCode(UUID sessionID, String provincialSpecialCaseCode, String studentStatusCode);

    @Query("""
    select stud from AssessmentStudentLightEntity stud
    where stud.assessmentEntity.assessmentSessionEntity.sessionID = :sessionID
    and stud.studentStatusCode IN (:studentStatuses)
    and (stud.proficiencyScore is not null
         or stud.provincialSpecialCaseCode IN ('A', 'X', 'E', 'Q'))
    """)
    List<AssessmentStudentLightEntity> findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndStudentStatusCodeIn(UUID sessionID, List<String> studentStatuses);

    @Query("""
    select stud from AssessmentStudentLightEntity stud
    where stud.assessmentEntity.assessmentSessionEntity.sessionID = :sessionID
    and stud.downloadDate is not null
    and stud.studentStatusCode = 'ACTIVE'
    and (stud.provincialSpecialCaseCode is null
         or stud.provincialSpecialCaseCode <> :provincialSpecialCaseCode)
    order by stud.downloadDate desc
    limit 1
    """)
    Optional<AssessmentStudentLightEntity> findBySessionIDAndDownloadDateIsNotNullAndProvincialSpecialCaseCodeNotAndStudentStatusCodeActiveAndDownloadDateIsNotNull(UUID sessionID, String provincialSpecialCaseCode);

    @Query("""
    select stud from AssessmentStudentLightEntity stud
    where stud.assessmentEntity.assessmentSessionEntity.sessionID = :sessionID
    and stud.downloadDate is not null
    and stud.studentStatusCode = 'ACTIVE'
    and (stud.provincialSpecialCaseCode is null
         or stud.provincialSpecialCaseCode <> :provincialSpecialCaseCode)
    order by stud.downloadDate desc
    limit 1
    """)
    Optional<AssessmentStudentLightEntity> findBySessionIDAndDownloadDateIsNotNullAndProvincialSpecialCaseCodeNotAndStudentStatusCodeActive(UUID sessionID, String provincialSpecialCaseCode);

    @Query(value="""
        select stud.assessmentEntity.assessmentTypeCode as assessmentTypeCode,
        stud.gradeAtRegistration as grade,
        count(case when stud.proficiencyScore = 1 then 1 end) as profScore1,
        count(case when stud.proficiencyScore = 2 then 1 end) as profScore2,
        count(case when stud.proficiencyScore = 3 then 1 end) as profScore3,
        count(case when stud.proficiencyScore = 4 then 1 end) as profScore4,
        count(case when stud.provincialSpecialCaseCode = 'A' then 1 end) as aegCount,
        count(case when stud.provincialSpecialCaseCode = 'X' then 1 end) as ncCount,
        count(case when stud.provincialSpecialCaseCode = 'Q' then 1 end) as dsqCount,
        count(case when stud.provincialSpecialCaseCode = 'E' then 1 end) as xmtCount,
        count(*) as total
        from AssessmentStudentEntity stud
        where stud.assessmentEntity.assessmentSessionEntity.sessionID = :sessionID
        and (stud.provincialSpecialCaseCode is not null OR stud.proficiencyScore is not null)    
        and stud.studentStatusCode = 'ACTIVE'
        and stud.gradeAtRegistration is not null
        group by stud.assessmentEntity.assessmentTypeCode, stud.gradeAtRegistration
    """)
    List<SummaryByGradeQueryResponse> getSummaryByGradeForSession(UUID sessionID);

    @Query(value="""
        select stud.assessmentEntity.assessmentTypeCode as assessmentTypeCode,
        stud.assessmentFormID as formID,
        count(case when stud.proficiencyScore = 1 then 1 end) as profScore1,
        count(case when stud.proficiencyScore = 2 then 1 end) as profScore2,
        count(case when stud.proficiencyScore = 3 then 1 end) as profScore3,
        count(case when stud.proficiencyScore = 4 then 1 end) as profScore4,
        count(case when stud.provincialSpecialCaseCode = 'A' then 1 end) as aegCount,
        count(case when stud.provincialSpecialCaseCode = 'X' then 1 end) as ncCount,
        count(case when stud.provincialSpecialCaseCode = 'Q' then 1 end) as dsqCount,
        count(case when stud.provincialSpecialCaseCode = 'E' then 1 end) as xmtCount,
        count(*) as total
        from AssessmentStudentEntity stud
        where stud.assessmentEntity.assessmentSessionEntity.sessionID = :sessionID
        and (stud.provincialSpecialCaseCode is not null OR stud.proficiencyScore is not null)    
        and stud.studentStatusCode = 'ACTIVE'
        and stud.assessmentFormID is not null
        group by stud.assessmentEntity.assessmentTypeCode, stud.assessmentFormID
    """)
    List<SummaryByFormQueryResponse> getSummaryByFormForSession(UUID sessionID);
}
