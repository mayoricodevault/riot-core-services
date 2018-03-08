ALTER TABLE [dbo].[reportdefinition] ADD [bulkEdit] bit NOT NULL CONSTRAINT DF__reportDef__bulkedit DEFAULT(0)
GO

CREATE TABLE [dbo].[notification_template](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[templateBody] [varchar](8000) NOT NULL,
	[templateName] [varchar](255) NULL,
  PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

ALTER TABLE [dbo].[apc_reportgroupby] ADD [unit] [varchar](50) NULL
GO

ALTER TABLE [dbo].[connection0] ADD [code] [varchar](255) NULL
GO

UPDATE [dbo].[reportentryoption]
   SET group_id = (SELECT group_id
                      FROM[dbo].[reportdefinition]
							WHERE id = reportDefinition_id)
GO
