ALTER TABLE ASSESSMENT_STUDENT_ANSWER
DROP COLUMN ASSESSMENT_COMPONENT_ID;

ALTER TABLE ASSESSMENT_STUDENT_COMPONENT
    ADD COLUMN ASSESSMENT_COMPONENT_ID UUID NOT NULL;
