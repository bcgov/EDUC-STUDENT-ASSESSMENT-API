package ca.bc.gov.educ.assessment.api.repository.v1;


import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentStudentRepository extends JpaRepository<AssessmentStudentEntity, UUID>, JpaSpecificationExecutor<AssessmentStudentEntity> {
    Optional<AssessmentStudentEntity> findByAssessmentEntity_AssessmentIDAndStudentID(UUID assessmentID, UUID studentID);

    List<AssessmentStudentEntity> findByAssessmentEntity_AssessmentIDInAndStudentID(List<UUID> assessmentIDs, UUID studentID);

    Optional<AssessmentStudentEntity> findByAssessmentEntity_AssessmentIDAndPen(UUID AssessmentID, String pen);

    List<AssessmentStudentEntity> findByStudentID(UUID studentID);

    List<AssessmentStudentEntity> findByAssessmentEntity_AssessmentSessionEntity_SessionID(UUID sessionID);

    List<AssessmentStudentEntity> findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolOfRecordSchoolID(UUID sessionID, UUID schoolOfRecordSchoolID);

    @Query(value="""
    select count(*) from AssessmentEntity as a, AssessmentStudentEntity as stud
    where a.assessmentID = stud.assessmentEntity.assessmentID
    and a.assessmentTypeCode in (:assessmentCodes)
    and stud.studentID = :studentID
    and (stud.proficiencyScore is not null
    or stud.provincialSpecialCaseCode in ('X','Q'))""")
    int findNumberOfAttemptsForStudent(UUID studentID, List<String> assessmentCodes);

    @Query(value="""
    select count(*) from AssessmentEntity as a, AssessmentStudentEntity as stud
    where a.assessmentID = stud.assessmentEntity.assessmentID
    and a.assessmentTypeCode in (:assessmentCodes)
    and stud.pen = :pen
    and (stud.proficiencyScore is not null
    or stud.provincialSpecialCaseCode in ('X','Q'))""")
    int findNumberOfAttemptsForStudentPEN(String pen, List<String> assessmentCodes);

}
