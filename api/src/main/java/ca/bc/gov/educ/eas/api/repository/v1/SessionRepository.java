package ca.bc.gov.educ.eas.api.repository.v1;

import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {
    List<SessionEntity> findAllByActiveFromDateLessThanEqualAndActiveUntilDateGreaterThanEqual(LocalDateTime currentDate1, LocalDateTime currentDate2);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SessionEntity s WHERE s.courseMonth = '10' AND s.schoolYear = :currentYear")
    Boolean upcomingNovemberSessionExists(@Param("currentYear") String currentYear);
}
