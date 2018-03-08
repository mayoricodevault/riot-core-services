CREATE TABLE IF NOT EXISTS migration_step_result (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  migrationPath varchar(255) DEFAULT NULL,
  migrationResult longtext,
  version_id bigint(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY UK_kefhocllfwej5ima3ecom8dcp (migrationPath),
  KEY FK_q98fpcmc8da99nb93lmtjdwr5 (version_id)
);
ALTER TABLE migration_step_result
  ADD CONSTRAINT FK_q98fpcmc8da99nb93lmtjdwr5 FOREIGN KEY (version_id) REFERENCES version (id);
