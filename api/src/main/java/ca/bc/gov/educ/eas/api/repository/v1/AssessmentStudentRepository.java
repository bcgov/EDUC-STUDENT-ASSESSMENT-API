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

    @Query(value="""
    SELECT stud FROM AssessmentStudentEntity stud WHERE stud.assessmentStudentID
    NOT IN (SELECT saga.assessmentStudentID FROM EasSagaEntity saga WHERE saga.status != 'COMPLETED'
    AND saga.assessmentStudentID IS NOT NULL)
    AND stud.assessmentStudentStatusCode = 'LOADED'
    order by stud.createDate
    LIMIT :numberOfStudentsToPublish""")
    List<AssessmentStudentEntity> findTopLoadedStudentForPublishing(String numberOfStudentsToPublish);
}
