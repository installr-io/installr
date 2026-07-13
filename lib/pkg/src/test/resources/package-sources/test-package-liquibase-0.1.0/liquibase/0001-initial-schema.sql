-- liquibase formatted sql

-- changeset test:0001-initial-schema
CREATE TABLE IF NOT EXISTS PERSON (ID INT PRIMARY KEY, NAME VARCHAR(255));
-- rollback DROP TABLE PERSON;
