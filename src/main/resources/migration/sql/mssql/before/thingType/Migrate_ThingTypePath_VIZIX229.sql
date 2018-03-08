CREATE TABLE [dbo].[thingtypepath](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[originThingType_id] [numeric](19, 0) NULL,
	[destinyThingType_id][numeric](19, 0) NULL,
	[path] [varchar](255) NULL,
PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
)
GO

ALTER TABLE [dbo].[thingtypepath]  WITH CHECK ADD  CONSTRAINT [FK_thingtypepath_originThingType_id] FOREIGN KEY([originThingType_id])
REFERENCES [dbo].[thingtype] ([id])
GO

ALTER TABLE [dbo].[thingtypepath]  WITH CHECK ADD  CONSTRAINT [FK_thingtypepath_destinyThingType_id] FOREIGN KEY([destinyThingType_id])
REFERENCES [dbo].[thingtype] ([id])
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'ID of the table', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'thingtypepath', @level2type = N'Column', @level2name = 'id';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'Thing Type ID origin', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'thingtypepath', @level2type = N'Column', @level2name = 'originThingType_id';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'Thing Type ID destiny', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'thingtypepath', @level2type = N'Column', @level2name = 'destinyThingType_id';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'Path to get the value from originThingTypeId to destinyThingType_id', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table',  @level1name = 'thingtypepath', @level2type = N'Column', @level2name = 'path';
GO
