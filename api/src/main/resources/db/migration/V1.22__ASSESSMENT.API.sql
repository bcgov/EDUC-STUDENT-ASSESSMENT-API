ALTER TABLE ASSESSMENT_QUESTION
    DROP COLUMN MARK_VALUE,
    DROP COLUMN SCALE_FACTOR;

ALTER TABLE ASSESSMENT_QUESTION
ALTER COLUMN QUESTION_NUMBER TYPE NUMERIC;

ALTER TABLE ASSESSMENT_QUESTION
    ADD COLUMN MC_OE_FLAG VARCHAR(1),
    ADD COLUMN ITEM_NUMBER NUMERIC,
    ADD COLUMN QUESTION_VALUE NUMERIC(5,1),
    ADD COLUMN MAX_QUES_VALUE NUMERIC(5,1),
    ADD COLUMN MASTER_QUES_NUMBER NUMERIC,
    ADD COLUMN IRT_INCREMENT NUMERIC(10,1),
    ADD COLUMN PRELOAD_ANSWER VARCHAR(150),
    ADD COLUMN IRT NUMERIC;

CREATE TABLE ASSESSMENT_ANSWER
(
    ASSESSMENT_ANSWER_ID        UUID                                NOT NULL,
    ASSESSMENT_QUESTION_ID      UUID                                NOT NULL,
    MC_OE_FLAG                  VARCHAR(1)                                  ,
    OE_ITEM_TYPE                VARCHAR(1)                                  ,
    ANSWER_NUMBER               NUMERIC                             NOT NULL,
    MC_QUES_TYPE                VARCHAR(12)                                 ,
    MC_ANSWER                   VARCHAR(75)                                 ,
    MC_ANSWER_LOWER             NUMERIC(10,2)                               ,
    MC_ANSWER_UPPER             NUMERIC(10,2)                               ,
    QUESTION_VALUE                  NUMERIC(5,1)                                ,
    IRT                         NUMERIC                                     ,
    ITEM_NUMBER                 NUMERIC                                     ,
    LINKED_ITEM_NUMBER          NUMERIC                                     ,
    SCALE_FACTOR                NUMERIC                                     ,
    COGN_LEVEL_CODE             VARCHAR(10)                                  ,
    TASK_CODE                   VARCHAR(10)                                  ,
    CLAIM_CODE                  VARCHAR(10)                                  ,
    CONTEXT_CODE                VARCHAR(10)                                  ,
    CONCEPTS_CODE               VARCHAR(10)                                  ,
    CREATE_USER                 VARCHAR(100)                        NOT NULL,
    CREATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                 VARCHAR(100)                        NOT NULL,
    UPDATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ASSESSMENT_ANSWER_ID_PK PRIMARY KEY (ASSESSMENT_ANSWER_ID),
    CONSTRAINT FK_ASSESSMENT_QUESTION_ID FOREIGN KEY (ASSESSMENT_QUESTION_ID)
        REFERENCES ASSESSMENT_QUESTION (ASSESSMENT_QUESTION_ID),
    CONSTRAINT FK_COGN_LEVEL_CODE FOREIGN KEY (COGN_LEVEL_CODE)
        REFERENCES COGN_LEVEL_CODE (COGN_LEVEL_CODE),
    CONSTRAINT FK_TASK_CODE FOREIGN KEY (TASK_CODE)
        REFERENCES TASK_CODE (TASK_CODE),
    CONSTRAINT FK_CLAIM_CODE FOREIGN KEY (CLAIM_CODE)
        REFERENCES CLAIM_CODE (CLAIM_CODE),
    CONSTRAINT FK_CONTEXT_CODE FOREIGN KEY (CONTEXT_CODE)
        REFERENCES CONTEXT_CODE (CONTEXT_CODE),
    CONSTRAINT FK_CONCEPTS_CODE FOREIGN KEY (CONCEPTS_CODE)
        REFERENCES CONCEPTS_CODE (CONCEPTS_CODE)
);