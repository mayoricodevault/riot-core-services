ALTER TABLE [dbo].[reportdefinition] ADD isMobileDataEntry [bit] NOT NULL DEFAULT 0
GO

ALTER TABLE [dbo].[reportentryoption] ADD isMobile [bit] NOT NULL DEFAULT 0
GO
