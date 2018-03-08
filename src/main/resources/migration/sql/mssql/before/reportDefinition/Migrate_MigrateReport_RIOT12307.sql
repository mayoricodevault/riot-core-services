CREATE TABLE [dbo].[reportbulkprocess](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[affectedRecords] [numeric](19, 0) ,
	[endDate] [datetime] NULL,
	[iniDate] [datetime] NULL,
	[omittedRecords] [numeric](19, 0) NULL,
	[processTime] [numeric](19, 0) NULL,
	[query] [varchar](2000) NULL,
	[operation] [varchar](255) NULL,
	[status] [varchar](255) NULL,
  [totalRecords] [numeric](19, 0) NULL,
	[valuesToChange] [varchar](255) NULL,
	[createdByUser_id] [numeric](19, 0) NULL,
	[reportDefinition_id] [numeric](19, 0) NULL,
	[thingType_id] [numeric](19, 0) NULL,
PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
)
GO

ALTER TABLE [dbo].[reportbulkprocess]  WITH CHECK ADD  CONSTRAINT [FK_reportBulkProcess_user0] FOREIGN KEY([createdByUser_id])
REFERENCES [dbo].[user0] ([id])
GO

ALTER TABLE [dbo].[reportbulkprocess]  WITH CHECK ADD  CONSTRAINT [FK_reportbulkProcess_repDef] FOREIGN KEY([reportDefinition_id])
REFERENCES [dbo].[reportdefinition] ([id])
GO

ALTER TABLE [dbo].[reportbulkprocess]  WITH CHECK ADD  CONSTRAINT [FK_reportbulkProcess_thingType] FOREIGN KEY([thingType_id])
REFERENCES [dbo].[thingtype] ([id])
GO

ALTER TABLE [dbo].[reportentryoptionproperty] ADD [defaultMobileValue] VARCHAR(255) NOT NULL
GO