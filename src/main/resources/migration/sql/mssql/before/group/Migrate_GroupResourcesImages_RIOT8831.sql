CREATE TABLE [dbo].[apc_groupresources] (
  [id] [bigint] IDENTITY(1,1) NOT NULL,
  [imageIcon] [image],
  [imageTemplateName] [varchar](255) NULL,
  [group_id] [numeric](20,0) NULL,
  PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
)
GO