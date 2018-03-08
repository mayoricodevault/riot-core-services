ALTER TABLE [dbo].[reportdefinition] ADD  chartViewProperties0 NVARCHAR(MAX) NULL
GO

CREATE TABLE [dbo].[reportdefinitionconfig](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[keyType] [varchar](255) NULL,
	[keyValue] [varchar](8000) NULL,
	[reportDefinition_id] [numeric](19, 0) NOT NULL,
	PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

ALTER TABLE [dbo].[reportdefinitionconfig]  WITH CHECK ADD  CONSTRAINT [FK_reportdefinitionconfig_] FOREIGN KEY([reportDefinition_id])
REFERENCES [dbo].[reportdefinitionconfig] ([id])
GO

ALTER TABLE [dbo].[reportdefinitionconfig] CHECK CONSTRAINT [FK_reportdefinitionconfig_]
GO

ALTER TABLE [dbo].[reportdefinition] ADD heatmap [bit] NOT NULL DEFAULT 0
GO
ALTER TABLE [dbo].[reportdefinition] ADD  heatmapBlur int NULL
GO
ALTER TABLE [dbo].[reportdefinition] ADD  heatmapLabel VARCHAR(255) NULL
GO
ALTER TABLE [dbo].[reportdefinition] ADD  heatmapRadio int NULL
GO
