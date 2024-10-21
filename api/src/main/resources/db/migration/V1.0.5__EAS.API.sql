CREATE TABLE EAS_SAGA
(
    SAGA_ID                     UUID                                    NOT NULL,
    SAGA_NAME                   VARCHAR(50)                             NOT NULL,
    SAGA_STATE                  VARCHAR(100)                            NOT NULL,
    PAYLOAD                     VARCHAR                                 NOT NULL,
    RETRY_COUNT                 INTEGER,
    STATUS                      VARCHAR(20)                             NOT NULL,
    CREATE_USER                 VARCHAR(100)                            NOT NULL,
    CREATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP     NOT NULL,
    UPDATE_USER                 VARCHAR(100)                            NOT NULL,
    UPDATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP     NOT NULL,
    CONSTRAINT ASSESSMENT_STUDENT_SAGA_PK PRIMARY KEY (SAGA_ID)
);

CREATE INDEX EAS_SAGA_STATUS_IDX ON EAS_SAGA (STATUS);

CREATE TABLE EAS_SAGA_EVENT_STATES
(
    SAGA_EVENT_ID       UUID                                NOT NULL,
    SAGA_ID             UUID                                NOT NULL,
    SAGA_EVENT_STATE    VARCHAR(100)                        NOT NULL,
    SAGA_EVENT_OUTCOME  VARCHAR(100)                        NOT NULL,
    SAGA_STEP_NUMBER    INTEGER                             NOT NULL,
    SAGA_EVENT_RESPONSE VARCHAR                             NOT NULL,
    CREATE_USER         VARCHAR(100)                         NOT NULL,
    CREATE_DATE         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER         VARCHAR(100)                         NOT NULL,
    UPDATE_DATE         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT EAS_SAGA_EVENT_STATES_PK PRIMARY KEY (SAGA_EVENT_ID)
);

ALTER TABLE EAS_SAGA_EVENT_STATES
    ADD CONSTRAINT EAS_SAGA_EVENT_STATES_SAGA_ID_FK FOREIGN KEY (SAGA_ID) REFERENCES EAS_SAGA (SAGA_ID);
CREATE INDEX EAS_SAGA_EVENT_STATES_SID_SES_SEO_SSN_IDX ON EAS_SAGA_EVENT_STATES (SAGA_ID, SAGA_EVENT_STATE, SAGA_EVENT_OUTCOME, SAGA_STEP_NUMBER);