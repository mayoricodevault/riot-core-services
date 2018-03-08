DROP TABLE [dbo].[reportbulkprocess]
GO

CREATE TABLE [dbo].[reportbulkprocess](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[checked] [BIT] NOT NULL DEFAULT 0,
	[endDate] [datetime] NULL,
	[iniDate] [datetime] NULL,
	[processTime] [numeric](19, 0) NULL,
	[status] [varchar](255) NULL,
	[totalAffectedRecords] [numeric](19, 0) ,
	[totalOmittedRecords] [numeric](19, 0) NULL,
  [totalRecords] [numeric](19, 0) NULL,
	[typeProcess] [varchar](255) NULL,
	[createdByUser_id] [numeric](19, 0) NULL,
	[reportDefinition_id] [numeric](19, 0) NULL,
	[progress] [numeric](19, 0) DEFAULT 0,
	[threadName] [varchar](255) NULL
PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
)
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'id of reportbulkprocess', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocess', @level2type = N'Column', @level2name = 'id';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'determines if the bulk process message on UI was close by user', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocess', @level2type = N'Column', @level2name = 'checked';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the date when the process was complete', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocess', @level2type = N'Column', @level2name = 'endDate';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the date when the process was initialize', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocess', @level2type = N'Column', @level2name = 'iniDate';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the duration time of the process execution', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocess', @level2type = N'Column', @level2name = 'processTime';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the status of the process', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocess', @level2type = N'Column', @level2name = 'status';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the total records affected', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocess', @level2type = N'Column', @level2name = 'totalAffectedRecords';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the total records omitted', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocess', @level2type = N'Column', @level2name = 'totalOmittedRecords';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the total of records to be processed', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocess', @level2type = N'Column', @level2name = 'totalRecords';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the type of process to execute (update/delete)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocess', @level2type = N'Column', @level2name = 'typeProcess';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the id of the user that executed the bulk process', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocess', @level2type = N'Column', @level2name = 'createdByUser_id';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the id of the report over the bulk process was executed', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocess', @level2type = N'Column', @level2name = 'reportDefinition_id';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the percentage of progress advanced', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocess', @level2type = N'Column', @level2name = 'progress';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'Thread Name of the Job', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocess', @level2type = N'Column', @level2name = 'threadName';
GO

ALTER TABLE [dbo].[reportbulkprocess]  WITH CHECK ADD  CONSTRAINT [FK_reportBulkProcess_user0] FOREIGN KEY([createdByUser_id])
REFERENCES [dbo].[user0] ([id])
GO

ALTER TABLE [dbo].[reportbulkprocess]  WITH CHECK ADD  CONSTRAINT [FK_reportbulkProcess_repDef] FOREIGN KEY([reportDefinition_id])
REFERENCES [dbo].[reportdefinition] ([id])
GO

CREATE TABLE [dbo].[reportbulkprocessdetail](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[endDate] [datetime] NULL,
	[iniDate] [datetime] NULL,
	[processTime] [numeric](19, 0) NULL,
	[query] [text],
	[status] [varchar](255) NULL,
	[totalAffectedRecords] [numeric](19, 0) ,
	[totalOmittedRecords] [numeric](19, 0) NULL,
  [totalRecords] [numeric](19, 0) NULL,
	[valuesToChange] [varchar](255) NULL,
	[reportBulkProcess_id] [numeric](19, 0) NULL,
	[thingType_id] [numeric](19, 0) NULL
PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
)
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'id of reportbulkprocessdetail', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocessdetail', @level2type = N'Column', @level2name = 'id';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the date when the sub-process was completed', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocessdetail', @level2type = N'Column', @level2name = 'endDate';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the date when the sub-process was initialized', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocessdetail', @level2type = N'Column', @level2name = 'iniDate';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the duration time of the sub-process execution', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocessdetail', @level2type = N'Column', @level2name = 'processTime';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the query to be executed by the sub-process', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocessdetail', @level2type = N'Column', @level2name = 'query';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the status of the sub-process', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocessdetail', @level2type = N'Column', @level2name = 'status';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the total records affected', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocessdetail', @level2type = N'Column', @level2name = 'totalAffectedRecords';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the total records omitted', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocessdetail', @level2type = N'Column', @level2name = 'totalOmittedRecords';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the total of records to be processed', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocessdetail', @level2type = N'Column', @level2name = 'totalRecords';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the values to change', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocessdetail', @level2type = N'Column', @level2name = 'valuesToChange';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the id reference to the main process', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocessdetail', @level2type = N'Column', @level2name = 'reportBulkProcess_id';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'the id reference to the thing type', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocessdetail', @level2type = N'Column', @level2name = 'thingType_id';
GO

ALTER TABLE [dbo].[reportbulkprocessdetail]  WITH CHECK ADD  CONSTRAINT [FK_reportbulkProcess_] FOREIGN KEY([reportBulkProcess_id])
REFERENCES [dbo].[reportbulkprocess] ([id])
GO

ALTER TABLE [dbo].[reportbulkprocessdetail]  WITH CHECK ADD  CONSTRAINT [FK_reportbulkProcess_thingType] FOREIGN KEY([thingType_id])
REFERENCES [dbo].[thingtype] ([id])
GO

CREATE TABLE [dbo].[reportbulkprocessdetaillog](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[status] [varchar](255) NULL,
	[thingId] [numeric](19, 0) ,
	[reportBulkProcessDetail_id] [numeric](19, 0) NULL
PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
)
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'ID of reportbulkprocessdetaillog', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocessdetaillog', @level2type = N'Column', @level2name = 'id';
GO
EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'Status of the register: ADDED', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocessdetaillog', @level2type = N'Column', @level2name = 'status';
GO
EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'Thing ID to be Deleted', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocessdetaillog', @level2type = N'Column', @level2name = 'thingId';
GO
EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'Report Bulk Process Detail ID reference', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'reportbulkprocessdetaillog', @level2type = N'Column', @level2name = 'reportBulkProcessDetail_id';
GO

ALTER TABLE [dbo].[reportbulkprocessdetaillog]  WITH CHECK ADD  CONSTRAINT [FK_reportBulkProcessDetail_Log] FOREIGN KEY([reportBulkProcessDetail_id])
REFERENCES [dbo].[reportbulkprocessdetail] ([id])
GO