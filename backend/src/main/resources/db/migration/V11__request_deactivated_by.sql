-- Records which role took a request out of the listing.
--
-- A centre may now deactivate and republish its own requests, but must not be able to undo an
-- admin's takedown -- that is a moderation decision, and before this the centre could not only
-- reverse it but silently erase the mandatory reason along with it.
--
-- Telling the two apart needs the actor to be remembered, which nothing recorded. The column is
-- set on the way into INACTIVE and cleared on the way out, so it only ever describes the current
-- deactivation rather than accumulating history.
--
-- Existing INACTIVE rows are backfilled to ADMIN. Until now the admin panel was the only place a
-- request could be deactivated from, so that is what every one of them actually is; the safe
-- default is also the restrictive one.

ALTER TABLE requests ADD COLUMN deactivated_by VARCHAR(16);

UPDATE requests SET deactivated_by = 'ADMIN' WHERE status = 'INACTIVE';
