-- Repaints the category label colours for the 7c palette (lapis / lacquer / turquoise).
--
-- These colours cannot be restyled from CSS: an admin picks a background/foreground pair
-- per category and it is stored on the row, so the warm pairs seeded for the old palette
-- would otherwise survive the repaint and clash with every page they appear on.
--
-- Each statement is guarded on the OLD colour rather than on the slug alone. CategorySeeder
-- promises that an admin's own edits are never overwritten, and a migration that repainted
-- unconditionally would quietly break that promise for anyone who had already picked their
-- own swatch. A row that no longer carries the seeded colour is one somebody chose, and it
-- is left exactly as it is.

UPDATE categories SET label_bg = '#D6F2F6', label_text = '#0B7F91'
 WHERE slug = 'darman'        AND label_bg = '#F3E6D6';
UPDATE categories SET label_bg = '#FBE4E8', label_text = '#9B1C31'
 WHERE slug = 'balaya-tabiei' AND label_bg = '#F7E3E0';
UPDATE categories SET label_bg = '#DEE9FC', label_text = '#1F5FC0'
 WHERE slug = 'tahsil'        AND label_bg = '#E7EDF3';
UPDATE categories SET label_bg = '#E7E6F7', label_text = '#4B3F9E'
 WHERE slug = 'maishat'       AND label_bg = '#F1E4EC';
UPDATE categories SET label_bg = '#DCEFE8', label_text = '#1E6E57'
 WHERE slug = 'maskan'        AND label_bg = '#E6EFEC';
UPDATE categories SET label_bg = '#E4EEDC', label_text = '#4A6B2E'
 WHERE slug = 'eshteghal'     AND label_bg = '#EFEAD9';
UPDATE categories SET label_bg = '#F2E4F2', label_text = '#7A3E75'
 WHERE slug = 'jahizieh'      AND label_bg = '#F0E7F3';
UPDATE categories SET label_bg = '#E3EAF7', label_text = '#254F8E'
 WHERE slug = 'aytam'         AND label_bg = '#E9EDF2';

-- Categories that predate the redesign were backfilled with the neutral fallback rather
-- than a swatch, so they are matched on the colour alone -- their slugs are derived from
-- whatever the admin named them and are not known here.
UPDATE categories SET label_bg = '#E9F0FC', label_text = '#576E96'
 WHERE label_bg = '#EFEAE3';
