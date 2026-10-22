-- liquibase formatted sql

-- changeset installr:0001-transaction-actor
-- comment: Record the subject DN of the client, which started a transaction; existing transactions get 'unknown'

ALTER TABLE PUBLIC.TRANSACTION_LOG ADD COLUMN IF NOT EXISTS ACTOR CHARACTER VARYING(1024) DEFAULT 'unknown' NOT NULL;
ALTER TABLE PUBLIC.TRANSACTION_LOG ALTER COLUMN ACTOR DROP DEFAULT;
