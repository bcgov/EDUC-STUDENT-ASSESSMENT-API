ALTER TABLE STAGED_ASSESSMENT_STUDENT_COMPONENT DROP CONSTRAINT FK_STAGED_ASSESSMENT_STUDENT_COMPONENT_ID;
ALTER TABLE STAGED_ASSESSMENT_STUDENT_ANSWER DROP CONSTRAINT FK_STAGED_ASSESSMENT_STUDENT_COMPONENT_ID;

ALTER TABLE STAGED_ASSESSMENT_STUDENT_COMPONENT ADD CONSTRAINT FK_STAGED_ASSESSMENT_STUDENT_COMPONENT_ID FOREIGN KEY (ASSESSMENT_STUDENT_ID) REFERENCES STAGED_ASSESSMENT_STUDENT (ASSESSMENT_STUDENT_ID) ON DELETE CASCADE;
ALTER TABLE STAGED_ASSESSMENT_STUDENT_ANSWER ADD CONSTRAINT FK_STAGED_ASSESSMENT_STUDENT_COMPONENT_ID FOREIGN KEY (ASSESSMENT_STUDENT_COMPONENT_ID) REFERENCES STAGED_ASSESSMENT_STUDENT_COMPONENT (ASSESSMENT_STUDENT_COMPONENT_ID) ON DELETE CASCADE;