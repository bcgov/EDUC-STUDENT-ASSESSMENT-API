CREATE TABLE ASSESSMENT_COMPONENT
(
    ASSESSMENT_COMPONENT_ID     UUID                                NOT NULL,
    ASSESSMENT_FORM_ID          UUID                                NOT NULL,
    COMPONENT_TYPE_CODE         VARCHAR(1)                                  ,
    COMPONENT_SUB_TYPE_CODE     VARCHAR(10)                                 ,
    QUESTION_COUNT              NUMERIC                             NOT NULL,
    NUM_OMITS                   NUMERIC                                     ,
    OE_ITEM_CNT                 NUMERIC                                     ,
    OE_MARK_COUNT               NUMERIC                                     ,
    PRINT_MATERIALS_FLAG        VARCHAR(1)                                  ,
    CREATE_USER                 VARCHAR(100)                        NOT NULL,
    CREATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                 VARCHAR(100)                        NOT NULL,
    UPDATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ASSESSMENT_COMPONENT_ID_PK PRIMARY KEY (ASSESSMENT_COMPONENT_ID),
    CONSTRAINT FK_ASSESSMENT_FORM_ID FOREIGN KEY (ASSESSMENT_FORM_ID)
        REFERENCES ASSESSMENT_FORM (ASSESSMENT_FORM_ID)
);

CREATE TABLE ASSESSMENT_STUDENT_COMPONENT
(
    ASSESSMENT_STUDENT_COMPONENT_ID     UUID                                NOT NULL,
    ASSESSMENT_STUDENT_ID               UUID                                NOT NULL,
    CSID                                VARCHAR(11)                                 ,
    SEQN                                VARCHAR(4)                                  ,
    COMPONENT_TYPE_CODE                 VARCHAR(1)                             NOT NULL,
    COMPONENT_SUB_TYPE_CODE             VARCHAR(10)                                     ,
    BATCH                               NUMERIC                                     ,
    COMPONENT_TOTAL                     NUMERIC(5,1)                                     ,
    COMPONENT_SOURCE                    VARCHAR(1)                                  ,
    CHOICE_PATH_ID                      VARCHAR(1)                                  ,
    CREATE_USER                         VARCHAR(100)                        NOT NULL,
    CREATE_DATE                         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                         VARCHAR(100)                        NOT NULL,
    UPDATE_DATE                         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ASSESSMENT_STUDENT_COMPONENT_ID_PK PRIMARY KEY (ASSESSMENT_STUDENT_COMPONENT_ID),
    CONSTRAINT FK_ASSESSMENT_STUDENT_ID FOREIGN KEY (ASSESSMENT_STUDENT_ID)
        REFERENCES ASSESSMENT_STUDENT (ASSESSMENT_STUDENT_ID)
);

ALTER TABLE ASSESSMENT_QUESTION DROP CONSTRAINT FK_ASSESSMENT_FORM_ID;

ALTER TABLE ASSESSMENT_QUESTION
    ADD COLUMN ASSESSMENT_COMPONENT_ID UUID NOT NULL;

ALTER TABLE ASSESSMENT_QUESTION
    ADD CONSTRAINT FK_ASSESSMENT_COMPONENT_ID FOREIGN KEY (ASSESSMENT_COMPONENT_ID)
        REFERENCES ASSESSMENT_COMPONENT (ASSESSMENT_COMPONENT_ID);

ALTER TABLE ASSESSMENT_QUESTION
    DROP COLUMN ASSESSMENT_FORM_ID,
    DROP COLUMN MC_OE_FLAG;

ALTER TABLE ASSESSMENT_ANSWER DROP CONSTRAINT FK_ASSESSMENT_QUESTION_ID;

ALTER TABLE ASSESSMENT_ANSWER
    ALTER COLUMN ASSESSMENT_QUESTION_ID DROP NOT NULL;

ALTER TABLE ASSESSMENT_ANSWER
    ADD COLUMN ASSESSMENT_COMPONENT_ID UUID NOT NULL;

ALTER TABLE ASSESSMENT_ANSWER
    ADD CONSTRAINT FK_ASSESSMENT_COMPONENT_ID FOREIGN KEY (ASSESSMENT_COMPONENT_ID)
        REFERENCES ASSESSMENT_COMPONENT (ASSESSMENT_COMPONENT_ID);

ALTER TABLE ASSESSMENT_ANSWER
    DROP COLUMN MC_OE_FLAG;

ALTER TABLE ASSESSMENT_STUDENT
    ADD COLUMN MARKING_SESSION VARCHAR(6),
    ADD COLUMN PERCENT_COMPLETE NUMERIC;