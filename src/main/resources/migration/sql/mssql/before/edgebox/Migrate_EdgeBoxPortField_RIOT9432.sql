ALTER TABLE [dbo].[edgebox] ADD  port [bigint] DEFAULT 0;
GO
ALTER TABLE [dbo].[edgebox] ADD  type [varchar](255) NULL
GO


ALTER TABLE [dbo].[edgeboxrule] ADD  sortOrder [int] 0
GO
ALTER TABLE [dbo].[edgeboxrule] ADD  honorLastDetect [bit] NOT NULL DEFAULT 'FALSE';
GO
ALTER TABLE [dbo].[edgeboxrule] ADD  executeLoop [bit] NOT NULL DEFAULT 'FALSE';
GO