-- Requests are published the moment a centre creates them, and carry fewer fields.
--
-- The approval queue is gone: PENDING and REJECTED no longer occur, so the rows still
-- sitting in those statuses are published rather than left stranded in a state nothing
-- can move them out of. DRAFT stays -- a centre can still save a request before it is
-- ready -- and so do COMPLETED and INACTIVE.
--
-- city_id goes because a request never had a location of its own to give: V2 created the
-- column and backfilled it straight from centers.city_id. The city and province facets now
-- read that same column through the centre, which is where the value came from all along.

UPDATE requests
   SET status       = 'PUBLISHED',
       published_at = COALESCE(published_at, CURRENT_TIMESTAMP),
       -- The note only ever held a rejection reason, and there is nothing left to reject.
       status_note  = NULL
 WHERE status IN ('PENDING', 'REJECTED');

-- The FK has to go first; MySQL refuses to drop a column a constraint still references.
-- The ix_requests_city index is left implicit: it covers this column alone, so both MySQL
-- and H2 drop it with the column. Naming it here would not survive both dialects, since
-- MySQL wants DROP INDEX ... ON <table> and H2 does not accept that form.
ALTER TABLE requests DROP CONSTRAINT fk_requests_city;
ALTER TABLE requests DROP COLUMN city_id;

-- The deadline was optional and never enforced anything; the contact details on the centre
-- are the ones visitors are meant to use, and the request-level field duplicated them.
ALTER TABLE requests DROP COLUMN deadline;
ALTER TABLE requests DROP COLUMN contact_info;

-- details_json is a free-form map, but beneficiaryName was the only key the panel ever
-- wrote, and identifying the beneficiary is exactly what this platform does not do. Cleared
-- with LIKE rather than a JSON function because those differ between MySQL and H2.
UPDATE requests
   SET details_json = '{}'
 WHERE details_json LIKE '%beneficiaryName%';
