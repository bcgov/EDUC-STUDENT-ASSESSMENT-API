package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentLightEntity;
import ca.bc.gov.educ.assessment.api.model.v1.StagedAssessmentStudentEntity;
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
public interface AssessmentStudentLightRepository extends JpaRepository<AssessmentStudentLightEntity, UUID>, JpaSpecificationExecutor<AssessmentStudentEntity> {

    List<AssessmentStudentLightEntity> findByAssessmentEntity_AssessmentSessionEntity_SessionID(UUID sessionID);

    List<AssessmentStudentLightEntity> findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndDownloadDateIsNotNull(UUID sessionID);

    @Query("""
    select stud from AssessmentStudentLightEntity stud
    where stud.assessmentEntity.assessmentSessionEntity.sessionID = :sessionID
    and stud.downloadDate is not null
    order by stud.downloadDate desc
    limit 1
    """)
    Optional<AssessmentStudentLightEntity> findBySessionIDAndDownloadDateIsNotNull(UUID sessionID);
}
