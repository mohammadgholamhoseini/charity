-- CharityCase -> Request.
--
-- `amount_collected` is dropped: it was written exactly once (as ZERO, by the entity default) and
-- never updated anywhere in the codebase. There is no donation flow, so it only ever rendered an
-- always-0% progress bar. `amount_needed` stays NOT NULL -- it is shown publicly so visitors know
-- how much help a request needs.
--
-- New columns come from the redesign: a public request code, an SEO slug, the request's own city
-- (previously only reachable through its center), a deadline, and the note an admin must write
-- when rejecting.

ALTER TABLE charity_cases RENAME TO requests;

ALTER TABLE requests DROP COLUMN amount_collected;

ALTER TABLE requests ADD COLUMN code        VARCHAR(32);
ALTER TABLE requests ADD COLUMN slug        VARCHAR(255);
ALTER TABLE requests ADD COLUMN city_id     BIGINT;
ALTER TABLE requests ADD COLUMN deadline    DATE;
ALTER TABLE requests ADD COLUMN status_note VARCHAR(1000);

-- Deterministic, collision-free codes for rows that predate the column.
UPDATE requests SET code = CONCAT('RQ-', id) WHERE code IS NULL;

-- Inherit the city from the owning center. Written as a correlated subquery rather than
-- UPDATE ... JOIN, which H2 does not support.
UPDATE requests
   SET city_id = (SELECT c.city_id FROM centers c WHERE c.id = requests.center_id)
 WHERE city_id IS NULL;

ALTER TABLE requests ADD CONSTRAINT fk_requests_city FOREIGN KEY (city_id) REFERENCES cities (id);

-- `slug` is deliberately left NULL here. Persian slugs need Unicode normalisation (Arabic yeh/kaf
-- folding, ZWNJ stripping) that is not reasonably expressible in portable SQL, so RequestSlugBackfill
-- fills them in on startup. Both MySQL and H2 permit repeated NULLs under a UNIQUE index, so the
-- constraint can be created now and satisfied afterwards.
CREATE UNIQUE INDEX uk_requests_code ON requests (code);
CREATE UNIQUE INDEX uk_requests_slug ON requests (slug);

-- Supports the public list: filter by status, order by urgency then creation date.
CREATE INDEX ix_requests_status_created ON requests (status, created_at);
CREATE INDEX ix_requests_center         ON requests (center_id);
CREATE INDEX ix_requests_category       ON requests (category_id);
CREATE INDEX ix_requests_city           ON requests (city_id);
