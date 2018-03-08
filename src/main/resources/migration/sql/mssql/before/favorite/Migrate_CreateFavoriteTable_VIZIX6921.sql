IF  NOT EXISTS (SELECT * FROM sys.objects
WHERE object_id = OBJECT_ID(N'[dbo].[favorite]') AND type in (N'U'))

BEGIN
CREATE TABLE [dbo].[favorite](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[date] [numeric](19, 0) NULL,
	[elementId][numeric](19, 0) NULL,
	[elementName] [varchar](255) NULL,
	[sequence][numeric](19, 0) NULL,
	[status] [varchar](255) NULL,
	[typeElement] [varchar](255) NULL,
	[elementGroupId] [numeric](19, 0) NULL,
	[user_id][numeric](19, 0) NULL,
PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]

)
GO

ALTER TABLE [dbo].[favorite]  WITH CHECK ADD  CONSTRAINT [FK_favorite_user] FOREIGN KEY([user_id])
REFERENCES [dbo].[user0] ([id])

END