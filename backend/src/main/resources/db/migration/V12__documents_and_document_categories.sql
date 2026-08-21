-- Documents become real rows.
--
-- requests.documents_json was a JSON array of bare stored filenames behind JsonListConverter. It
-- could not carry a category, a title, the name the uploader actually chose, a size or a timestamp;
-- it could not be joined; and "how many documents use this category?" -- the question an admin
-- deleting a document category has to be able to answer -- cannot be asked of a TEXT blob at all.
--
-- One document_categories table with a scope discriminator rather than two tables: the two lists
-- are column-identical, and (scope, name) / (scope, slug) uniqueness is more correct than what two
-- separate tables would give by accident. The one thing two tables would enforce for free -- that a
-- request document cannot be filed under a CENTER category -- is asserted in DocumentService, the
-- same shape RequestService.allowedCategory already uses.
--
-- This is NOT the existing `categories` table with a column bolted on. That one is the public need
-- taxonomy: joined by center_categories, referenced by requests.category_id, counted for the
-- homepage grid and driving the public ?category= facet. It has nothing to do with paperwork.
--
-- requests.documents_json is deliberately left in place. RequestDocumentBackfill copies it into
-- request_documents on startup, and the column is dropped by a later migration only once that has
-- been observed to have run in production. Hibernate's `validate` complains about missing columns,
-- never about extra ones, so the entity dropping the field is safe on its own.
--
-- Dialect subset shared by MySQL 8.4 and H2 in MODE=MySQL: no ENGINE/CHARSET, no INSERT IGNORE,
-- indexes created as separate statements rather than inline.

CREATE TABLE document_categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    scope       VARCHAR(16)  NOT NULL,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_document_categories_scope_name UNIQUE (scope, name),
    CONSTRAINT uk_document_categories_scope_slug UNIQUE (scope, slug)
);

CREATE TABLE request_documents (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    request_id        BIGINT       NOT NULL,
    category_id       BIGINT       NOT NULL,
    stored_filename   VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255),
    title             VARCHAR(255),
    content_type      VARCHAR(120),
    size_bytes        BIGINT,
    sort_order        INTEGER      NOT NULL DEFAULT 0,
    uploaded_by_role  VARCHAR(16),
    uploaded_at       DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_request_documents_request  FOREIGN KEY (request_id)  REFERENCES requests (id),
    CONSTRAINT fk_request_documents_category FOREIGN KEY (category_id) REFERENCES document_categories (id)
);

CREATE INDEX idx_request_documents_request  ON request_documents (request_id);
CREATE INDEX idx_request_documents_category ON request_documents (category_id);

CREATE TABLE center_documents (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    center_id         BIGINT       NOT NULL,
    category_id       BIGINT       NOT NULL,
    stored_filename   VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255),
    title             VARCHAR(255),
    content_type      VARCHAR(120),
    size_bytes        BIGINT,
    sort_order        INTEGER      NOT NULL DEFAULT 0,
    uploaded_by_role  VARCHAR(16),
    uploaded_at       DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_center_documents_center   FOREIGN KEY (center_id)   REFERENCES centers (id),
    CONSTRAINT fk_center_documents_category FOREIGN KEY (category_id) REFERENCES document_categories (id)
);

CREATE INDEX idx_center_documents_center  ON center_documents (center_id);
CREATE INDEX idx_center_documents_category ON center_documents (category_id);

-- Starting taxonomy. Persian names are literals here, which is fine: the AGENTS.md rule about
-- Persian text in SQL is about *generating* slugs from it, and these slugs are hand-written Latin.
-- Admins may rename, reorder, deactivate or delete any of them afterwards; nothing re-seeds them.
INSERT INTO document_categories (scope, name, slug, description, sort_order, active, created_at, updated_at) VALUES
    ('REQUEST', 'مستندات مالی',   'financial', 'فاکتور، برگه هزینه و مستندات مالی مرتبط با نیاز', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('REQUEST', 'مستندات پزشکی',  'medical',   'نسخه، گزارش پزشکی و مدارک درمانی', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('REQUEST', 'مستندات عمومی',  'general',   'سایر مدارک پشتیبان درخواست', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CENTER',  'مجوز فعالیت',    'license',   'پروانه یا مجوز رسمی فعالیت مرکز', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CENTER',  'اساسنامه',       'articles',  'اساسنامه و مدارک ثبتی مرکز', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CENTER',  'صورت مالی',      'financial-statement', 'صورت‌های مالی و گزارش عملکرد', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
