DROP TABLE ASSESSMENT_STUDENT_ANSWER;

CREATE TABLE ASSESSMENT_STUDENT_ANSWER
(
    ASSESSMENT_STUDENT_ANSWER_ID    UUID                                NOT NULL,
    ASSESSMENT_STUDENT_COMPONENT_ID UUID                                NOT NULL,
    ITEM_NUMBER                     NUMERIC NULL,
    QUESTION_NUMBER                 NUMERIC NULL,
    MC_ASSESSMENT_RESPONSE_UNSORTED VARCHAR(40) NULL,
    MC_ASSESSMENT_RESPONSE_SORTED   VARCHAR(40) NULL,
    SCORE                           NUMERIC NULL,
    CREATE_USER                     VARCHAR(100)                        NOT NULL,
    CREATE_DATE                     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                     VARCHAR(100)                        NOT NULL,
    UPDATE_DATE                     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ASSESSMENT_STUDENT_ANSWER_ID_PK PRIMARY KEY (ASSESSMENT_STUDENT_ANSWER_ID),
    CONSTRAINT FK_ASSESSMENT_STUDENT_COMPONENT_ID FOREIGN KEY (ASSESSMENT_STUDENT_COMPONENT_ID) REFERENCES ASSESSMENT_STUDENT_COMPONENT (ASSESSMENT_STUDENT_COMPONENT_ID)
);