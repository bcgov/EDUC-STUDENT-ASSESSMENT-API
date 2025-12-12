delete from assessment_student_answer ans
    using assessment_student_component comp, assessment_student stud, assessment assess
where ans.assessment_student_component_id = comp.assessment_student_component_id
  and comp.assessment_student_id = stud.assessment_student_id
  and stud.assessment_id = assess.assessment_id
  and assess.session_id = '<REPLACE_ME>';

delete from assessment_student_choice_question_set choice_ques
    using assessment_student_choice choice, assessment_student_component comp, assessment_student stud, assessment assess
where choice.assessment_student_component_id = comp.assessment_student_component_id
  and comp.assessment_student_id = stud.assessment_student_id
  and choice.assessment_student_choice_id = choice_ques.assessment_student_choice_id
  and stud.assessment_id = assess.assessment_id
  and assess.session_id = '<REPLACE_ME>';

delete from assessment_student_choice choice
    using assessment_student_component comp, assessment_student stud, assessment assess
where choice.assessment_student_component_id = comp.assessment_student_component_id
  and comp.assessment_student_id = stud.assessment_student_id
  and stud.assessment_id = assess.assessment_id
  and assess.session_id = '<REPLACE_ME>';

delete from assessment_student_component comp
    using assessment_student stud, assessment assess
where comp.assessment_student_id = stud.assessment_student_id
  and stud.assessment_id = assess.assessment_id
  and assess.session_id = '<REPLACE_ME>';
delete from assessment_student_answer ans
    using assessment_student_component comp, assessment_student stud, assessment assess
where ans.assessment_student_component_id = comp.assessment_student_component_id
  and comp.assessment_student_id = stud.assessment_student_id
  and stud.assessment_id = assess.assessment_id
  and assess.session_id = '<REPLACE_ME>';

delete from assessment_student_choice choice
    using assessment_student_component comp, assessment_student stud, assessment assess
where choice.assessment_student_component_id = comp.assessment_student_component_id
  and comp.assessment_student_id = stud.assessment_student_id
  and stud.assessment_id = assess.assessment_id
  and assess.session_id = '<REPLACE_ME>';

delete from assessment_student_component comp
    using assessment_student stud, assessment assess
where comp.assessment_student_id = stud.assessment_student_id
  and stud.assessment_id = assess.assessment_id
  and assess.session_id = '<REPLACE_ME>';

delete from assessment_saga_event_states;
delete from assessment_saga;

delete from staged_assessment_student_answer;
delete from staged_assessment_student_choice;
delete from staged_assessment_student_component;
delete from staged_assessment_student;
delete from staged_student_result_upload;

delete from assessment_question ques
    using assessment assess, assessment_component comp, assessment_form form
where ques.assessment_component_id = comp.assessment_component_id
  and form.assessment_form_id = comp.assessment_form_id
  and form.assessment_id = assess.assessment_id
  and assess.session_id = '<REPLACE_ME>';

delete from assessment_choice choice
    using assessment assess, assessment_component comp, assessment_form form
where choice.assessment_component_id = comp.assessment_component_id
  and form.assessment_form_id = comp.assessment_form_id
  and form.assessment_id = assess.assessment_id
  and assess.session_id = '<REPLACE_ME>';

delete from assessment_component comp
    using assessment assess, assessment_form form
where form.assessment_form_id = comp.assessment_form_id
  and form.assessment_id = assess.assessment_id
  and assess.session_id = '<REPLACE_ME>';

delete from assessment_student_history stud_hist
    using assessment assess
where stud_hist.assessment_id = assess.assessment_id
  and assess.session_id = '<REPLACE_ME>';

update assessment_student stud
set download_date = null,
    proficiency_score = null,
    provincial_special_case_code = null,
    adapted_assessment_code = null,
    assessment_form_id = null,
    irt_score = null,
    raw_score = null,
    mc_total = null,
    oe_total = null,
    marking_session = null,
    school_of_record_at_write_school_id = null
    FROM assessment assess
where stud.assessment_id = assess.assessment_id
  and assess.session_id = '<REPLACE_ME>';

delete from assessment_form form
    using assessment assess
where form.assessment_id = assess.assessment_id
  and assess.session_id = '<REPLACE_ME>';

update assessment_session
set assessment_registrations_export_date = null,
    assessment_registrations_export_user_id = null,
    approval_student_cert_sign_date = null,
    approval_student_cert_user_id = null,
    approval_assessment_analysis_sign_date = null,
    approval_assessment_analysis_user_id = null,
    approval_assessment_design_sign_date = null,
    approval_assessment_design_user_id = null,
    session_writing_attempts_export_date = null,
    session_writing_attempts_export_user_id = null,
    pen_merges_export_date = null,
    pen_merges_export_user_id = null
where session_id = '<REPLACE_ME>';

delete from assessment_saga_event_states;
delete from assessment_saga;

delete from staged_assessment_student_answer;
delete from staged_assessment_student_choice;
delete from staged_assessment_student_component;
delete from staged_assessment_student;
delete from staged_student_result_upload;

delete from assessment_question ques
    using assessment assess, assessment_component comp, assessment_form form
where ques.assessment_component_id = comp.assessment_component_id
  and form.assessment_form_id = comp.assessment_form_id
  and form.assessment_id = assess.assessment_id
  and assess.session_id = '<REPLACE_ME>';

delete from assessment_choice choice
    using assessment assess, assessment_component comp, assessment_form form
where choice.assessment_component_id = comp.assessment_component_id
  and form.assessment_form_id = comp.assessment_form_id
  and form.assessment_id = assess.assessment_id
  and assess.session_id = '<REPLACE_ME>';

delete from assessment_component comp
    using assessment assess, assessment_form form
where form.assessment_form_id = comp.assessment_form_id
  and form.assessment_id = assess.assessment_id
  and assess.session_id = '<REPLACE_ME>';

delete from assessment_student_history stud_hist
    using assessment assess
where stud_hist.assessment_id = assess.assessment_id
  and assess.session_id = '<REPLACE_ME>';

update assessment_student stud
set download_date = null,
    proficiency_score = null,
    provincial_special_case_code = null,
    adapted_assessment_code = null,
    assessment_form_id = null,
    irt_score = null,
    raw_score = null,
    mc_total = null,
    oe_total = null,
    marking_session = null,
    school_of_record_at_write_school_id = null
    FROM assessment assess
where stud.assessment_id = assess.assessment_id
  and assess.session_id = '<REPLACE_ME>';

delete from assessment_form form
    using assessment assess
where form.assessment_id = assess.assessment_id
  and assess.session_id = '<REPLACE_ME>';

update assessment_session
set assessment_registrations_export_date = null,
    assessment_registrations_export_user_id = null,
    approval_student_cert_sign_date = null,
    approval_student_cert_user_id = null,
    approval_assessment_analysis_sign_date = null,
    approval_assessment_analysis_user_id = null,
    approval_assessment_design_sign_date = null,
    approval_assessment_design_user_id = null,
    session_writing_attempts_export_date = null,
    session_writing_attempts_export_user_id = null,
    pen_merges_export_date = null,
    pen_merges_export_user_id = null
where session_id = '<REPLACE_ME>';
