-- remove if exists
BEGIN
  IF EXISTS ( SELECT * FROM   sysobjects
              WHERE  id = object_id(N'[dbo].hazelcastMapFix_')
                     and OBJECTPROPERTY(id, N'IsProcedure') = 1 )
  BEGIN
     DROP PROCEDURE [dbo].hazelcastMapFix_;
  END;
END;
GO

CREATE PROCEDURE [dbo].hazelcastMapFix_
AS BEGIN
  DECLARE @aux1 NUMERIC(20);
  DECLARE @aux2 NUMERIC(20);
  DECLARE @aux1_new NUMERIC(20);
  DECLARE @aux2_new NUMERIC(20);
  DECLARE @maxid INTEGER;

  SELECT count(1) INTO @aux1 FROM INFORMATION_SCHEMA.TABLES
  WHERE TABLE_SCHEMA = 'dbo'
        AND TABLES.TABLE_CATALOG = 'riot_main'
        ANd TABLES.TABLE_NAME = 'apc_thingtypemap';
  IF @aux1 = 1
  BEGIN
    SELECT count(1) INTO @aux1_new FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = 'dbo'
       AND TABLES.TABLE_CATALOG = 'riot_main'
       ANd TABLES.TABLE_NAME = 'thingtypemap';
    IF @aux1_new = 0
    BEGIN
      IF NOT EXISTS (SELECT * from sysobjects WHERE name='thingtypemap' AND xtype='U')
      BEGIN
        CREATE TABLE [dbo].thingtypemap(
          id NUMERIC(20) NOT NULL IDENTITY(100,1),
          child_id NUMERIC(19) DEFAULT NULL,
          parent_id NUMERIC(19) DEFAULT NULL
        );
      END;
      ALTER TABLE [dbo].thingtypemap ALTER COLUMN id NUMERIC(20) NOT NULL;
      ALTER TABLE [dbo].thingtypemap ADD CONSTRAINT pk_thingtypemap_id PRIMARY KEY (id);
      ALTER TABLE [dbo].thingtypemap ADD CONSTRAINT FK_thing_typemap_thingtype_p FOREIGN KEY (parent_id) REFERENCES [dbo].thingtype (id);
      ALTER TABLE [dbo].thingtypemap ADD CONSTRAINT FK_thing_typemap_thingtype_c FOREIGN KEY (child_id) REFERENCES [dbo].thingtype (id);
    END;
    DROP TABLE [dbo].apc_thingtypemap;
  END;

  SELECT count(1) INTO @aux2 FROM INFORMATION_SCHEMA.TABLES
   WHERE TABLE_SCHEMA = 'dbo'
     AND TABLES.TABLE_CATALOG = 'riot_main'
     AND TABLES.TABLE_NAME = 'notification_template';
  IF @aux2=1
  BEGIN
    SELECT count(1) INTO @aux2_new FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = 'dbo'
       AND TABLES.TABLE_CATALOG = 'riot_main'
       AND TABLES.TABLE_NAME = 'notificationtemplate';
    IF @aux2_new = 0
    BEGIN
      IF NOT EXISTS (SELECT * from sysobjects WHERE name='notificationtemplate' AND xtype='U')
      BEGIN
          CREATE TABLE [dbo].notificationtemplate(
            id NUMERIC(20) NOT NULL IDENTITY(100,1),
            templateBody VARCHAR(MAX),
            templateName VARCHAR(255) DEFAULT NULL
          );
      END;
      ALTER TABLE [dbo].notificationtemplate ADD CONSTRAINT pk_notificationtemplate_id PRIMARY KEY (id);
      INSERT INTO [dbo].notificationtemplate(templateBody, templateName) SELECT a.templateBody, a.templateName FROM [dbo].notification_template a;
    END;
    DROP TABLE [dbo].notification_template;
  END;
END;
GO

EXECUTE [dbo].hazelcastMapFix_ GO
DROP PROCEDURE [dbo].hazelcastMapFix_ GO

