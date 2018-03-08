IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='userpassword' AND xtype='U')
  CREATE TABLE userpassword (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    creationTime bigint(20) DEFAULT NULL,
    failedAttempts bigint(20) DEFAULT NULL,
    hashedPassword varchar(255) DEFAULT NULL,
    lastFailedTime bigint(20) DEFAULT NULL,
    status varchar(255) DEFAULT NULL,
    user_id bigint(20) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY FK_o0eengy9i0jygdor8elyxv2xt (user_id)
  );
  ALTER TABLE userpassword
    ADD CONSTRAINT FK_o0eengy9i0jygdor8elyxv2xt FOREIGN KEY (user_id) REFERENCES user0 (id);
GO

TRUNCATE TABLE userpassword;

INSERT INTO userpassword
SELECT NULL, datediff(ss, '1970-01-01', SYSDATETIMEOFFSET() at time zone 'UTC'), 0, hashedPassword, null, "ACTIVE", id
FROM user0;

ALTER TABLE user0 DROP hashedPassword;
