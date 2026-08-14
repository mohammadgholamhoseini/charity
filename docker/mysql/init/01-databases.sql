-- Runs only when the MySQL data directory is empty (a fresh `mysql_data` volume).
-- An existing volume is never touched by this file.
--
-- MYSQL_DATABASE creates exactly one schema and grants MYSQL_USER on it. This deployment
-- needs two — one per branch — so the second schema and its grant are created here.
-- Without the grant the backend fails at startup with "Access denied ... to database".
--
-- The underscore in YARIJU_DEVELOPMENT is escaped in the GRANT because MySQL treats the
-- database name there as a LIKE pattern: unescaped, `_` is a single-character wildcard and
-- the grant would silently cover more schemas than intended.

CREATE DATABASE IF NOT EXISTS `YARIJU`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS `YARIJU_DEVELOPMENT`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

GRANT ALL PRIVILEGES ON `YARIJU`.* TO 'charity_user'@'%';
GRANT ALL PRIVILEGES ON `YARIJU\_DEVELOPMENT`.* TO 'charity_user'@'%';
FLUSH PRIVILEGES;
