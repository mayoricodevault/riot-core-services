-- remove if exists
BEGIN
  IF EXISTS ( SELECT * FROM   sysobjects
              WHERE  id = object_id(N'[dbo].addColumn')
                     and OBJECTPROPERTY(id, N'IsProcedure') = 1 )
  BEGIN
     DROP PROCEDURE [dbo].addColumn;
  END;
END;
GO

CREATE PROCEDURE [dbo].addColumn
AS BEGIN
  DECLARE @aux1 NUMERIC(20);
  SELECT count(1) INTO @aux1 FROM INFORMATION_SCHEMA.TABLES
  WHERE TABLE_SCHEMA = 'dbo'
        AND TABLES.TABLE_CATALOG = 'riot_main'
        ANd TABLES.TABLE_NAME = 'folder';
  IF @aux1 = 1
    BEGIN
      ALTER TABLE dbo.folder ADD createdByUser_id bigint(20)
      GO
      ALTER TABLE [dbo].folder ADD CONSTRAINT FK_folder_user0 FOREIGN KEY (createdByUser_id) REFERENCES [dbo].user0 (id);
      GO
    END;
  ELSE
    BEGIN
        IF  NOT EXISTS (SELECT * FROM sys.objects
      WHERE object_id = OBJECT_ID(N'[dbo].[folder]') AND type in (N'U'))

    BEGIN
    CREATE TABLE [dbo].[folder](
	    [id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	    [code] [varchar](255) NULL,
	    [creationTime][datetime] NULL,
	    [lastModificationDate] [datetime] NULL,
	    [name] [varchar](255) NULL,
	    [sequence] [numeric](19, 0)
	    [typeElement] [varchar](255) NULL,
	    [group_id] [numeric](19, 0) NULL,
	    [folderId_id][numeric](19, 0) NULL,
	    [createdByUser_id][numeric](19, 0) NULL,
      PRIMARY KEY CLUSTERED
		  (
			  [id] ASC
		  )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]

    )
  GO

  ALTER TABLE [dbo].[folder]  WITH CHECK ADD  CONSTRAINT [FK_folder_group] FOREIGN KEY([group_id])
  REFERENCES [dbo].[group0] ([id])
  GO
  ALTER TABLE [dbo].[folder]  WITH CHECK ADD  CONSTRAINT [FK_folder_folder] FOREIGN KEY([folderId_id])
  REFERENCES [dbo].[folder] ([id])
  GO
  ALTER TABLE [dbo].[folder]  WITH CHECK ADD  CONSTRAINT [FK_folder_user0] FOREIGN KEY([createdByUser_id])
  REFERENCES [dbo].[user0] ([id])

  END
    END;
END;
GO

EXECUTE [dbo].addColumn GO
DROP PROCEDURE [dbo].addColumn GO