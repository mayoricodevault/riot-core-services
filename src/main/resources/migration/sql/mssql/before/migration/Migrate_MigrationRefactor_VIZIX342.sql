ALTER TABLE dbo.version ADD versionName VARCHAR(255)
GO
ALTER TABLE dbo.version ADD versionDesc VARCHAR(255)
GO


CREATE TABLE [dbo].[migration_step_result](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[migrationPath] [varchar](255) NULL,
	[migrationResult] [varchar](4000) NULL,
	[version_id] [numeric](19, 0) NOT NULL,
	PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

ALTER TABLE [dbo].[migration_step_result]  WITH CHECK ADD  CONSTRAINT [FK_migration_step_result_] FOREIGN KEY([version_id])
REFERENCES [dbo].[version] ([id])
GO

ALTER TABLE [dbo].[migration_step_result] CHECK CONSTRAINT [FK_migration_step_result_]
GO