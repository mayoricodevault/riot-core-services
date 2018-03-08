CREATE TABLE [dbo].[reportcustomfilter](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[displayOrder] [varchar](255) NULL ,
	[label] [varchar](255) NULL,
	[operator] [varchar](255) NULL,
	[propertyName] [varchar](255) NULL,
	[value] [varchar](255) NULL,
	[editable] [bit] NULL,
	[dataTypeId] [numeric](19, 0) NOT NULL,
	[reportDefinition_id] [numeric](19, 0) NULL,
PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
)
GO

ALTER TABLE [dbo].[reportcustomfilter]  WITH CHECK ADD  CONSTRAINT [FK_customfilter_reportdefinition] FOREIGN KEY([reportDefinition_id])
REFERENCES [dbo].[reportdefinition] ([id])
GO