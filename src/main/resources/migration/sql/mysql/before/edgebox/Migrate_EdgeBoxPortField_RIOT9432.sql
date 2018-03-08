ALTER TABLE edgebox ADD COLUMN port BIGINT(20) DEFAULT 0;
ALTER TABLE edgebox ADD COLUMN type varchar(255) DEFAULT NULL;

ALTER TABLE edgeboxrule ADD COLUMN sortOrder int(11) DEFAULT 0;
ALTER TABLE edgeboxrule ADD COLUMN honorLastDetect bit(1) DEFAULT 0;
ALTER TABLE edgeboxrule ADD COLUMN executeLoop bit(1) DEFAULT 0;