-- Categories gain an SEO slug and the label colours the admin picks from the eight design swatches.
-- Colours are stored as an explicit background/foreground pair rather than a palette key, so the
-- admin can be given new swatches later without a migration.
--
-- Only the columns are created here. Populating slugs and colours, and reconciling the seeded
-- taxonomy with the one in the redesign, happens in CategorySeeder: it needs Persian-aware
-- slugging and name matching that portable SQL cannot express.

ALTER TABLE categories ADD COLUMN slug        VARCHAR(120);
ALTER TABLE categories ADD COLUMN label_bg    VARCHAR(9);
ALTER TABLE categories ADD COLUMN label_text  VARCHAR(9);
ALTER TABLE categories ADD COLUMN sort_order  INTEGER NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX uk_categories_slug ON categories (slug);
