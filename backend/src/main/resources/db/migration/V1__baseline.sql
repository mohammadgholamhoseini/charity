-- Baseline: the schema as Hibernate's ddl-auto=update left it before Flyway was introduced.
--
-- On the existing master MySQL database this file is NEVER executed: flyway.baseline-on-migrate
-- marks the non-empty schema as already being at version 1 and starts at V2. It runs only when
-- creating a database from scratch (fresh MySQL, or the in-memory H2 used by the `local` profile),
-- so it must stay functionally equivalent to what Hibernate generated -- same table and column
-- names, because V2+ depend on them.
--
-- SQL here is restricted to the dialect subset shared by MySQL 8.4 and H2 in MODE=MySQL:
-- no ENGINE/CHARSET clauses, no vendor-specific functions.

CREATE TABLE users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    username   VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    role       VARCHAR(255) NOT NULL,
    full_name  VARCHAR(255),
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email)
);

CREATE TABLE categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    icon_url    VARCHAR(255),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name)
);

CREATE TABLE provinces (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_provinces_name UNIQUE (name)
);

CREATE TABLE cities (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL,
    province_id BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cities_province_name UNIQUE (province_id, name),
    CONSTRAINT fk_cities_province FOREIGN KEY (province_id) REFERENCES provinces (id)
);

CREATE TABLE centers (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    name          VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255),
    province_id   BIGINT,
    city_id       BIGINT,
    description   VARCHAR(1000),
    contact_phone VARCHAR(255),
    address       VARCHAR(1000),
    card_number   VARCHAR(255),
    sheba         VARCHAR(255),
    logo_url      VARCHAR(255),
    status        VARCHAR(255),
    created_at    DATETIME(6),
    updated_at    DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_centers_user     FOREIGN KEY (user_id)     REFERENCES users (id),
    CONSTRAINT fk_centers_province FOREIGN KEY (province_id) REFERENCES provinces (id),
    CONSTRAINT fk_centers_city     FOREIGN KEY (city_id)     REFERENCES cities (id)
);

CREATE TABLE center_categories (
    center_id   BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (center_id, category_id),
    CONSTRAINT fk_center_categories_center   FOREIGN KEY (center_id)   REFERENCES centers (id),
    CONSTRAINT fk_center_categories_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE charity_cases (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    center_id           BIGINT        NOT NULL,
    category_id         BIGINT        NOT NULL,
    title               VARCHAR(255)  NOT NULL,
    description         VARCHAR(3000),
    amount_needed       DECIMAL(19,2) NOT NULL,
    amount_collected    DECIMAL(19,2),
    image_url           VARCHAR(255),
    contact_info        VARCHAR(500),
    details_json        TEXT,
    documents_json      TEXT,
    status              VARCHAR(255),
    urgency             VARCHAR(255),
    telegram_posted     BOOLEAN       NOT NULL DEFAULT FALSE,
    telegram_message_id INTEGER,
    bale_posted         BOOLEAN       NOT NULL DEFAULT FALSE,
    bale_message_id     INTEGER,
    created_at          DATETIME(6),
    updated_at          DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_charity_cases_center   FOREIGN KEY (center_id)   REFERENCES centers (id),
    CONSTRAINT fk_charity_cases_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE notices (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    title      VARCHAR(255) NOT NULL,
    content    TEXT         NOT NULL,
    position   VARCHAR(255),
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id)
);
