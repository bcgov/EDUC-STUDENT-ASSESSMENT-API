package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AssessmentSessionRepository extends JpaRepository<AssessmentSessionEntity, UUID> {
    List<AssessmentSessionEntity> findAllByActiveFromDateLessThanEqualOrderByActiveUntilDateDesc(LocalDateTime currentDate1);
    List<AssessmentSessionEntity> findAllByActiveFromDateLessThanEqualAndActiveUntilDateGreaterThanEqual(LocalDateTime currentDate1, LocalDateTime currentDate2);
    List<AssessmentSessionEntity> findBySchoolYear(String schoolYear);
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM AssessmentSessionEntity s WHERE s.schoolYear = :schoolYear")
    Boolean upcomingSchoolYearSessionsExist(@Param("schoolYear") String schoolYear);
}
