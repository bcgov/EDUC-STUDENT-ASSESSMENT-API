package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.StagedAssessmentStudentLightEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.SummaryByFormQueryResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.SummaryByGradeQueryResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StagedAssessmentStudentLightRepository extends JpaRepository<StagedAssessmentStudentLightEntity, UUID>, JpaSpecificationExecutor<StagedAssessmentStudentLightEntity> {

    List<StagedAssessmentStudentLightEntity> findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndStagedAssessmentStudentStatusIn(UUID sessionID, List<String> studentStatuses);

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
        from StagedAssessmentStudentLightEntity stud
        where stud.assessmentEntity.assessmentSessionEntity.sessionID = :sessionID
        and stud.stagedAssessmentStudentStatus = 'ACTIVE'
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
        from StagedAssessmentStudentLightEntity stud
        where stud.assessmentEntity.assessmentSessionEntity.sessionID = :sessionID
        and stud.stagedAssessmentStudentStatus = 'ACTIVE'
        group by stud.assessmentEntity.assessmentTypeCode, stud.assessmentFormID
    """)
    List<SummaryByFormQueryResponse> getSummaryByFormForSession(UUID sessionID);
}
