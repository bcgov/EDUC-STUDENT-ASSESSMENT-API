CREATE TABLE DISTRICT
(
    district_id          VARCHAR(40)  NOT NULL,
    district_number      varchar(3)   NOT NULL,
    fax_number           varchar(10)  NULL,
    phone_number         varchar(10)  NULL,
    email                varchar(255) NULL,
    website              varchar(255) NULL,
    display_name         varchar(255) NOT NULL,
    district_region_code varchar(10)  NOT NULL,
    district_status_code varchar(10)  NOT NULL,
    create_user          varchar(100) NOT NULL,
    create_date          timestamp    NOT NULL,
    update_user          varchar(100) NOT NULL,
    update_date          timestamp    NOT NULL,
    CONSTRAINT district_id_pk PRIMARY KEY (district_id)
);

CREATE TABLE SCHOOL
(
    school_id                         VARCHAR(40)         NOT NULL,
    district_id                       VARCHAR(40)         NOT NULL,
    independent_authority_id          VARCHAR(40)         NULL,
    school_number                     varchar(5)   NOT NULL,
    fax_number                        varchar(10)  NULL,
    phone_number                      varchar(10)  NULL,
    email                             varchar(255) NULL,
    website                           varchar(255) NULL,
    display_name                      varchar(255) NOT NULL,
    school_organization_code          varchar(10)  NOT NULL,
    school_category_code              varchar(10)  NOT NULL,
    facility_type_code                varchar(10)  NOT NULL,
    school_reporting_requirement_code varchar(10)  NOT NULL,
    opened_date                       timestamp    NULL,
    closed_date                       timestamp    NULL,
    create_user                       varchar(100) NOT NULL,
    create_date                       timestamp    NOT NULL,
    update_user                       varchar(100) NOT NULL,
    update_date                       timestamp    NOT NULL,
    display_name_no_spec_chars        varchar(255) NULL,
    CONSTRAINT school_id_pk PRIMARY KEY (school_id)
);

--Create STUDENT_LINK table from required environment (Student API)
--SELECT STUDENT_ID,PEN,LEGAL_FIRST_NAME,LEGAL_LAST_NAME FROM STUDENT;

--Create GRAD_STUDENT_RECORD table from required environment (Grad Student API)
--SELECT GRADUATION_STUDENT_RECORD_ID, SCHOOL_OF_RECORD_ID FROM GRADUATION_STUDENT_RECORD;

-----------------------------------------------
--Check query for missing sessions
SELECT DISTINCT TRIM(tabSess.ASSMT_SESSION) AS sess
FROM STUD_GRAD_ASSMT tabSess WHERE CONCAT(TRIM(tabSess.ASSMT_SESSION), TRIM(tabSess.ASSMT_CODE)) NOT in(
    (SELECT CONCAT(TRIM(ASSMT_SESSION), TRIM(ASSMT_CODE)) AS overall
     FROM TAB_AVAILABLE_GRAD_ASSMT_SESS sga
     GROUP BY sga.ASSMT_SESSION, sga.ASSMT_CODE))
                               AND TRIM(tabSess.ASSMT_SESSION) NOT IN (SELECT CONCAT(TRIM(as2.COURSE_YEAR), TRIM(as2.COURSE_MONTH)) FROM ASSESSMENT_SESSION as2) ;

--Check query for missing assessment types
SELECT DISTINCT TRIM(tabSess.ASSMT_SESSION), TRIM(tabSess.ASSMT_CODE) AS sess
FROM STUD_GRAD_ASSMT tabSess WHERE CONCAT(TRIM(tabSess.ASSMT_SESSION), TRIM(tabSess.ASSMT_CODE)) NOT in(
    (SELECT CONCAT(TRIM(ASSMT_SESSION), TRIM(ASSMT_CODE)) AS overall
     FROM TAB_AVAILABLE_GRAD_ASSMT_SESS sga
     GROUP BY sga.ASSMT_SESSION, sga.ASSMT_CODE));

--Check for missing keys/forms
SELECT DISTINCT TRIM(tabSess.ASSMT_SESSION), TRIM(tabSess.ASSMT_CODE), tabSess.FORM_CODE AS sess
FROM STUD_GRAD_ASSMT tabSess WHERE CONCAT(TRIM(tabSess.ASSMT_SESSION), TRIM(tabSess.ASSMT_CODE)) NOT in(
    (SELECT CONCAT(TRIM(ASSMT_SESSION), TRIM(ASSMT_CODE)) AS overall
     FROM TAB_GRAD_ASSMT_KEY sga
     GROUP BY sga.ASSMT_SESSION, sga.ASSMT_CODE));

-----------------------------------------------

--Drop indexes
ALTER TABLE ASSESSMENT_STUDENT DROP CONSTRAINT ASSESSMENT_ID_FK;
ALTER TABLE ASSESSMENT_STUDENT DROP CONSTRAINT FK_ASSESSMENT_FORM_ID;
ALTER TABLE ASSESSMENT_STUDENT DROP CONSTRAINT FK_ASSESSMENT_ID;
ALTER TABLE ASSESSMENT_STUDENT DROP CONSTRAINT FK_PROVINCIAL_SPECIAL_CASE_CODE;
ALTER TABLE ASSESSMENT_STUDENT_ANSWER DROP CONSTRAINT FK_ASSESSMENT_STUDENT_ID;
ALTER TABLE ASSESSMENT_STUDENT_COMPONENT DROP CONSTRAINT FK_ASSESSMENT_STUDENT_ID;

--Add back indexes
ALTER TABLE api_eas.assessment_student ADD CONSTRAINT assessment_id_fk FOREIGN KEY (assessment_id) REFERENCES assessment(assessment_id);
ALTER TABLE api_eas.assessment_student ADD CONSTRAINT fk_assessment_form_id FOREIGN KEY (assessment_form_id) REFERENCES assessment_form(assessment_form_id);
ALTER TABLE api_eas.assessment_student ADD CONSTRAINT fk_assessment_id FOREIGN KEY (assessment_id) REFERENCES assessment(assessment_id);
ALTER TABLE api_eas.assessment_student ADD CONSTRAINT fk_provincial_special_case_code FOREIGN KEY (provincial_special_case_code) REFERENCES provincial_special_case_code(provincial_special_case_code);
ALTER TABLE api_eas.assessment_student_answer ADD CONSTRAINT fk_assessment_student_id FOREIGN KEY (assessment_student_id) REFERENCES assessment_student(assessment_student_id)
ALTER TABLE api_eas.assessment_student_component ADD CONSTRAINT fk_assessment_student_id FOREIGN KEY (assessment_student_id) REFERENCES assessment_student(assessment_student_id)