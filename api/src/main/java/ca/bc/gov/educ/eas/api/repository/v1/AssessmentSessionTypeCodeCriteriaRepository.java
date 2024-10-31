package ca.bc.gov.educ.eas.api.repository.v1;

import ca.bc.gov.educ.eas.api.model.v1.AssessmentSessionTypeCodeCriteriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AssessmentSessionTypeCodeCriteriaRepository extends JpaRepository<AssessmentSessionTypeCodeCriteriaEntity, UUID> {

    @Query
            ("SELECT astcc FROM AssessmentSessionTypeCodeCriteriaEntity astcc " +
            "JOIN astcc.assessmentSessionCriteriaEntity asc " +
            "WHERE MONTH(asc.sessionEnd) = :month " +
            "AND astcc.effectiveDate <= :effectiveDate " +
            "AND astcc.expiryDate >= :expiryDate")
    List<AssessmentSessionTypeCodeCriteriaEntity> findAllBySessionEndMonth(@Param("month") Integer month, @Param("effectiveDate") LocalDateTime effectiveDate, @Param("expiryDate") LocalDateTime expiryDate);

}