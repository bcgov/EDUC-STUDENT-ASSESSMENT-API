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

    List<AssessmentStudentLightEntity> findByAssessmentEntity_AssessmentSessionEntity_SessionID(UUID sessionID);

    @Query("""
    select stud from AssessmentStudentLightEntity stud
    where stud.assessmentEntity.assessmentTypeCode = :assessmentTypeCode
    and stud.assessmentEntity.assessmentSessionEntity.sessionID = :sessionID
    and (stud.proficiencyScore is not null
         or stud.provincialSpecialCaseCode is not null)
    """)
    List<AssessmentStudentLightEntity> findByAssessmentTypeCodeAndSessionIDAndProficiencyScoreNotNullOrProvincialSpecialCaseNotNull(String assessmentTypeCode, UUID sessionID);

    List<AssessmentStudentLightEntity> findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndDownloadDateIsNotNullAndProvincialSpecialCaseCodeNotAndStudentStatusCode(UUID sessionID, String provincialSpecialCaseCode, String studentStatusCode);

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
    Optional<AssessmentStudentLightEntity> findBySessionIDAndDownloadDateIsNotNullAndProvincialSpecialCaseCodeNotAndStudentStatusCodeActive(UUID sessionID, String provincialSpecialCaseCode);

    @Query(value="""
        select stud.assessmentEntity.assessmentTypeCode as assessmentTypeCode,
        stud.gradeAtRegistration as grade,
        count(case when stud.proficiencyScore = 1 then 1 end) as profScore1,
        count(case when stud.proficiencyScore = 2 then 1 end) as profScore2,
        count(case when stud.proficiencyScore = 3 then 1 end) as profScore3,
        count(case when stud.proficiencyScore = 4 then 1 end) as profScore4,
        count(case when stud.provincialSpecialCaseCode = 'AEG' then 1 end) as aegCount,
        count(case when stud.provincialSpecialCaseCode = 'NC' then 1 end) as ncCount,
        count(case when stud.provincialSpecialCaseCode = 'DSQ' then 1 end) as dsqCount,
        count(case when stud.provincialSpecialCaseCode = 'XMT' then 1 end) as xmtCount,
        count(stud.gradeAtRegistration) as total
        from AssessmentStudentEntity stud
        where stud.assessmentEntity.assessmentSessionEntity.sessionID = :sessionID
        and stud.studentStatusCode = 'ACTIVE'
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
        count(case when stud.provincialSpecialCaseCode = 'AEG' then 1 end) as aegCount,
        count(case when stud.provincialSpecialCaseCode = 'NC' then 1 end) as ncCount,
        count(case when stud.provincialSpecialCaseCode = 'DSQ' then 1 end) as dsqCount,
        count(case when stud.provincialSpecialCaseCode = 'XMT' then 1 end) as xmtCount,
        count(stud.assessmentFormID) as total
        from AssessmentStudentEntity stud
        where stud.assessmentEntity.assessmentSessionEntity.sessionID = :sessionID
        and stud.studentStatusCode = 'ACTIVE'
        group by stud.assessmentEntity.assessmentTypeCode, stud.assessmentFormID
    """)
    List<SummaryByFormQueryResponse> getSummaryByFormForSession(UUID sessionID);
}
