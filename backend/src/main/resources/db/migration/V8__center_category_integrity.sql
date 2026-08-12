-- Two data repairs that the new rules require.
--
-- 1) A request's category must be one of its center's allowed categories. Existing rows can violate
--    this, because nothing enforced it before. The least destructive repair is to grant the center
--    the category it is demonstrably already publishing in, rather than reassigning live requests to
--    some other category. Enforcement for new writes lives in RequestService, not a DB trigger:
--    triggers are invisible to the ORM and cannot produce a Persian error message.
INSERT INTO center_categories (center_id, category_id)
SELECT DISTINCT r.center_id, r.category_id
  FROM requests r
 WHERE NOT EXISTS (
       SELECT 1 FROM center_categories cc
        WHERE cc.center_id = r.center_id AND cc.category_id = r.category_id);

-- 2) Center approval was dead code. Centers are created only by an admin and `createByAdmin` already
--    set APPROVED directly, so PENDING and REJECTED were never reachable -- as was the whole
--    /api/admin/centers/pending screen. The enum collapses to APPROVED | INACTIVE, matching the
--    single active/inactive switch in the redesigned admin form.
UPDATE centers SET status = 'APPROVED' WHERE status IS NULL OR status IN ('PENDING', 'REJECTED');
