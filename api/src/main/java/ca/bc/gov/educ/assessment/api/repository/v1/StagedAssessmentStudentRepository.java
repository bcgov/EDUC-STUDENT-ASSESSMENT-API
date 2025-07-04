package ca.bc.gov.educ.assessment.api.repository.v1;


import ca.bc.gov.educ.assessment.api.model.v1.StagedAssessmentStudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StagedAssessmentStudentRepository extends JpaRepository<StagedAssessmentStudentEntity, UUID>, JpaSpecificationExecutor<StagedAssessmentStudentEntity> {
    @Query(value="""
    select stud.studentID from StagedAssessmentStudentEntity as stud
    where stud.assessmentEntity.assessmentSessionEntity.sessionID = :sessionID""")
    List<UUID> findAllStagedStudentsInSession(UUID sessionID);
}
