-- Login throttling. The design's login screen promises «پس از ۵ تلاش ناموفق حساب موقتاً قفل می‌شود»,
-- but nothing enforced it: there was no attempt counter, no lockout and no rate limit, and Spring's
-- accountNonLocked was hardcoded true.
--
-- Counters live on the user row rather than in memory so a restart cannot be used to reset them.

ALTER TABLE users ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN locked_until    DATETIME(6);
ALTER TABLE users ADD COLUMN last_login_at   DATETIME(6);
