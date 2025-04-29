CREATE TABLE ASSESSMENT_STUDENT_HISTORY
(
    ASSESSMENT_STUDENT_HISTORY_ID       UUID                                NOT NULL,
    ASSESSMENT_STUDENT_ID               UUID                                NOT NULL,
    ASSESSMENT_ID                       UUID                                NOT NULL,
    SCHOOL_ID                           UUID                                NOT NULL,
    STUDENT_ID                          UUID                                NOT NULL,
    PEN                                 VARCHAR(9)                          NOT NULL,
    LOCAL_ID                            VARCHAR(12),
    IS_ELECTRONIC_EXAM                  BOOLEAN,
    FINAL_PERCENTAGE                    VARCHAR(3),
    PROVINCIAL_SPECIAL_CASE_CODE        VARCHAR(1),
    COURSE_STATUS_CODE                  VARCHAR(1),
    CREATE_USER                         VARCHAR(100)                        NOT NULL,
    CREATE_DATE                         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                         VARCHAR(100)                        NOT NULL,
    UPDATE_DATE                         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ASSESSMENT_STUDENT_HISTORY_ID_PK PRIMARY KEY (ASSESSMENT_STUDENT_HISTORY_ID)
);
