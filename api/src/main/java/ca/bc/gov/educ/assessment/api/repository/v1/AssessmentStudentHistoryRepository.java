package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AssessmentStudentHistoryRepository extends JpaRepository<AssessmentStudentHistoryEntity, UUID> {
    List<AssessmentStudentHistoryEntity> findAllByAssessmentIDAndAssessmentStudentID(UUID asessmentID, UUID assessmentStudentID);
    void deleteAllByAssessmentIDAndAssessmentStudentID(UUID assessmentID, UUID assessmentStudentID);
    
    @Modifying (clearAutomatically = true)
    @Transactional
    @Query(value="""
    INSERT INTO assessment_student_history (assessment_student_history_ID, assessment_id, assessment_student_id, assessment_form_id,
         school_of_record_at_write_school_id, assessment_center_school_id, school_of_record_school_id, student_id, given_name, surname, pen, local_id, local_assessment_id,
         proficiency_score, provincial_special_case_code, number_of_attempts, adapted_assessment_code, irt_score, marking_session,
             raw_score, mc_total, oe_total, download_date, create_user, create_date, update_user, update_date)
    select  gen_random_uuid(), a.assessment_id, stud.assessment_student_id, stud.assessment_form_id,
         stud.school_of_record_at_write_school_id, stud.assessment_center_school_id, stud.school_of_record_school_id, stud.student_id, stud.given_name, stud.surname, stud.pen, stud.local_id, stud.local_assessment_id,
         stud.proficiency_score, stud.provincial_special_case_code, stud.number_of_attempts, stud.adapted_assessment_code, stud.irt_score, stud.marking_session,
             stud.raw_score, stud.mc_total, stud.oe_total, stud.download_date, stud.create_user, stud.create_date, :updateUser, :updateDate 
    from assessment a, assessment_student stud
    where a.session_id = :assessmentSessionID
    and stud.student_status_code = 'ACTIVE'
    and stud.provincial_special_case_code not in ('E')""", nativeQuery = true)
    void insertHistoryForDownloadDateUpdate(UUID assessmentSessionID, String updateUser, LocalDateTime updateDate);

}
