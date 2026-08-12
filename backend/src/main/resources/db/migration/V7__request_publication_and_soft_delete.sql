-- Publication lifecycle, soft delete, and the columns SEO needs.
--
-- `urgency_rank` exists so the urgency ordering lives in exactly one place. It was previously
-- duplicated as an identical CASE WHEN across six JPQL queries, which meant adding an urgency
-- level required editing all six. A plain maintained column is used rather than a MySQL STORED
-- generated column, because a generated column would not exist on the H2 profile and local sort
-- order would then silently differ from production.
--
-- `deleted_at` turns deletion into a soft delete so a removed request can answer 410 Gone rather
-- than 404: search engines drop 410s from the index materially faster, and the distinction between
-- "existed and is gone" and "never existed" is otherwise unrecoverable once the row is dropped.

ALTER TABLE requests ADD COLUMN published_at     DATETIME(6);
ALTER TABLE requests ADD COLUMN deleted_at       DATETIME(6);
ALTER TABLE requests ADD COLUMN urgency_rank     INTEGER NOT NULL DEFAULT 1;
ALTER TABLE requests ADD COLUMN meta_title       VARCHAR(70);
ALTER TABLE requests ADD COLUMN meta_description VARCHAR(160);

UPDATE requests
   SET urgency_rank = CASE urgency
       WHEN 'URGENT' THEN 3
       WHEN 'HIGH'   THEN 2
       WHEN 'MEDIUM' THEN 1
       ELSE 0
   END;

-- Existing PUBLISHED rows are deliberately left PUBLISHED. The new PENDING default applies only to
-- newly created requests; retroactively unpublishing live content would drop every indexed URL.
UPDATE requests SET published_at = created_at WHERE status = 'PUBLISHED' AND published_at IS NULL;

-- Covers the default public ordering: status filter, then urgency desc, then newest.
CREATE INDEX ix_requests_status_urgency ON requests (status, urgency_rank, created_at);
CREATE INDEX ix_requests_deleted_at     ON requests (deleted_at);

-- A published slug is frozen. If an admin does change one, the old value lands here so the public
-- endpoint can answer 301 instead of 404 -- a silent slug change is otherwise a self-inflicted
-- de-index of a URL that already ranks.
CREATE TABLE request_slug_history (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    request_id BIGINT       NOT NULL,
    old_slug   VARCHAR(255) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_request_slug_history_old_slug UNIQUE (old_slug),
    CONSTRAINT fk_request_slug_history_request
        FOREIGN KEY (request_id) REFERENCES requests (id) ON DELETE CASCADE
);
