ALTER TABLE [dbo].[reportdefinition] ADD heatmap [bit] NOT NULL DEFAULT 0
GO
ALTER TABLE [dbo].[reportdefinition] ADD  heatmapBlur int NULL
GO
ALTER TABLE [dbo].[reportdefinition] ADD  heatmapLabel VARCHAR(255) NULL
GO
ALTER TABLE [dbo].[reportdefinition] ADD  heatmapRadio int NULL
GO
