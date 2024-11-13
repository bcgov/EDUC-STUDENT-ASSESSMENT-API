package ca.bc.gov.educ.eas.api.repository.v1;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {
    List<SessionEntity> findAllByActiveFromDateLessThanEqualOrderByActiveUntilDateDesc(LocalDateTime currentDate1);
    List<SessionEntity> findAllByActiveFromDateLessThanEqualAndActiveUntilDateGreaterThanEqual(LocalDateTime currentDate1, LocalDateTime currentDate2);
    List<SessionEntity> findBySchoolYear(String schoolYear);
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SessionEntity s WHERE s.courseMonth = '10' AND s.schoolYear = :currentYear")
    Boolean upcomingNovemberSessionExists(@Param("currentYear") String currentYear);
}
