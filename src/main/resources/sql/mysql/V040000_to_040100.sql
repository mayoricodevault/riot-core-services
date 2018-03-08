ALTER TABLE attachment ADD COLUMN  operationOverFile varchar(255) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `apc_groupresources` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `imageIcon` longblob,
  `imageTemplateName` varchar(255) DEFAULT NULL,
  `group_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_mbvgldapp14qytn24otlht001` (`group_id`),
  CONSTRAINT `FK_mbvgldapp14qytn24otlht001` FOREIGN KEY (`group_id`) REFERENCES `group0` (`id`)
);

ALTER TABLE edgebox ADD COLUMN port BIGINT(20) DEFAULT 0;
ALTER TABLE edgebox ADD COLUMN type varchar(255) DEFAULT NULL;

ALTER TABLE edgeboxrule ADD COLUMN sortOrder int(11) DEFAULT 0;
ALTER TABLE edgeboxrule ADD COLUMN honorLastDetect bit(1) DEFAULT 0;
ALTER TABLE edgeboxrule ADD COLUMN executeLoop bit(1) DEFAULT 0;

CREATE INDEX IDX_token_tokenExpirationTime ON token (tokenExpirationTime);

CREATE INDEX IDX_thingtype_thingTypeCode ON thingtype (thingTypeCode);

CREATE INDEX IDX_thingtypefield_name ON thingtypefield (name);

CREATE INDEX IDX_datatype_code ON datatype (code);

CREATE INDEX IDX_group0_code ON group0 (code);

CREATE UNIQUE INDEX IDX_field_name ON apc_field (name);

CREATE UNIQUE INDEX IDX_user_apiKey ON user0 (apiKey);

CREATE UNIQUE INDEX IDX_user_username ON user0 (username);
