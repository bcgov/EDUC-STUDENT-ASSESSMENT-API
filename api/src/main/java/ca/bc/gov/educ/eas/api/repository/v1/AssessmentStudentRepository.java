package ca.bc.gov.educ.eas.api.repository.v1;


import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
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

    List<AssessmentStudentEntity> findByAssessmentEntity_SessionEntity_SessionID(UUID sessionID);

    @Query(value="""
    SELECT stud FROM AssessmentStudentEntity stud WHERE stud.assessmentStudentID
    NOT IN (SELECT saga.assessmentStudentID FROM EasSagaEntity saga WHERE saga.status != 'COMPLETED'
    AND saga.assessmentStudentID IS NOT NULL)
    AND stud.assessmentStudentStatusCode = 'LOADED'
    order by stud.createDate
    LIMIT :numberOfStudentsToPublish""")
    List<AssessmentStudentEntity> findTopLoadedStudentForPublishing(String numberOfStudentsToPublish);

    @Query(value="""
    select count(*) from AssessmentEntity as a, AssessmentStudentEntity as stud
    where a.assessmentID = stud.assessmentEntity.assessmentID
    and a.assessmentTypeCode in (:assessmentCodes)
    and stud.studentID = :studentID
    and (stud.proficiencyScore is not null
    or stud.provincialSpecialCaseCode in ('A', 'Q'))""")
    int findNumberOfAttemptsForStudent(UUID studentID, List<String> assessmentCodes);

}
