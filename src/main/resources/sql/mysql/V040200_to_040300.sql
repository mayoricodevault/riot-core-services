ALTER TABLE connection0  DROP INDEX UK_3n8yg35ryw9qbsgheytb6nag9;

ALTER TABLE reportdefinition ADD `description` TEXT NOT NULL;

ALTER TABLE thingtypetemplate ADD `autoCreate` bit(1) DEFAULT 0;
