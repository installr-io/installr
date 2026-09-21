-- liquibase formatted sql

-- changeset test:0002-add-users
CREATE TABLE IF NOT EXISTS APP_USER (ID INT PRIMARY KEY, USERNAME VARCHAR(255));
-- rollback DROP TABLE APP_USER;
