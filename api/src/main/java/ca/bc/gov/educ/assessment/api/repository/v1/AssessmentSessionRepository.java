package ca.bc.gov.educ.assessment.api.repository.v1;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;

@Repository
public interface AssessmentSessionRepository extends JpaRepository<AssessmentSessionEntity, UUID> {
    List<AssessmentSessionEntity> findAllByActiveFromDateLessThanEqualOrderByActiveUntilDateDesc(LocalDateTime currentDate1);

    @Query("SELECT s FROM AssessmentSessionEntity s " +
           "WHERE (s.courseYear > :fromYear OR (s.courseYear = :fromYear AND s.courseMonth >= :fromMonth)) " +
           "AND (s.courseYear < :toYear OR (s.courseYear = :toYear AND s.courseMonth <= :toMonth)) " +
           "ORDER BY s.courseYear DESC, s.courseMonth DESC")
    List<AssessmentSessionEntity> findSessionsInYearMonthRange(
            @Param("fromYear") String fromYear,
            @Param("fromMonth") String fromMonth,
            @Param("toYear") String toYear,
            @Param("toMonth") String toMonth
    );
    List<AssessmentSessionEntity> findAllByActiveFromDateLessThanEqualAndActiveUntilDateGreaterThanEqualAndCompletionDateIsNull(LocalDateTime currentDate1, LocalDateTime currentDate2);
    List<AssessmentSessionEntity> findBySchoolYear(String schoolYear);
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM AssessmentSessionEntity s WHERE s.schoolYear = :schoolYear")
    Boolean upcomingSchoolYearSessionsExist(@Param("schoolYear") String schoolYear);

    Optional<AssessmentSessionEntity> findByCourseYearAndCourseMonth(String courseYear, String courseMonth);
}
