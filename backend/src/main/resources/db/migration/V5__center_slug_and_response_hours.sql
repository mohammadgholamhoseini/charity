-- Centers gain an SEO slug (for /centers/<slug>) and the response hours the design shows on the
-- request detail sidebar («ساعات پاسخ‌گویی»). Slugs are backfilled by CenterSlugBackfill on startup
-- for the same Unicode reasons as request slugs.

ALTER TABLE centers ADD COLUMN slug           VARCHAR(255);
ALTER TABLE centers ADD COLUMN response_hours VARCHAR(120);

CREATE UNIQUE INDEX uk_centers_slug ON centers (slug);
