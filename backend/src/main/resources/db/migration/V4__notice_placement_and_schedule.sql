-- Notices become scheduled announcements.
--
-- `position` (FOOTER | BANNER) becomes `placement` (FOOTER | TOP_BANNER) to match the design's
-- wording, and gains a display window plus an optional call-to-action link. A notice whose window
-- has passed reads as «منقضی» in the admin table; that state is derived from end_at, not stored.

ALTER TABLE notices ADD COLUMN placement VARCHAR(32);
ALTER TABLE notices ADD COLUMN start_at  DATETIME(6);
ALTER TABLE notices ADD COLUMN end_at    DATETIME(6);
ALTER TABLE notices ADD COLUMN link_url  VARCHAR(500);

UPDATE notices SET placement = 'TOP_BANNER' WHERE position = 'BANNER';
UPDATE notices SET placement = 'FOOTER'     WHERE placement IS NULL;

ALTER TABLE notices DROP COLUMN position;

-- Serving the active announcement for a placement is a hot path on every public page.
CREATE INDEX ix_notices_placement_active ON notices (placement, active);
