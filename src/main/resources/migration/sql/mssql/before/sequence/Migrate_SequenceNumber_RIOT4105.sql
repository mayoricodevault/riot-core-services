CREATE TABLE [dbo].[sequence](
	[id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
	[currentValue] [numeric](19, 0) NULL,
	[initialValue] [numeric](19, 0) NULL,
	[name] [varchar](255) NULL,
	[thingTypeField_id] [numeric](19, 0) NULL,
PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

SET ANSI_PADDING OFF
GO

ALTER TABLE [dbo].[sequence]  WITH CHECK ADD  CONSTRAINT [FK_kdtiey768mmsvfdwaiy7ktasl] FOREIGN KEY([thingTypeField_id])
REFERENCES [dbo].[ThingTypeField] ([id])
GO

ALTER TABLE [dbo].[sequence] CHECK CONSTRAINT [FK_kdtiey768mmsvfdwaiy7ktasl]
GO

ALTER TABLE [dbo].[shift] ADD  [code] VARCHAR(255) GO

ALTER TABLE [dbo].[reportDefinition] ADD  delete0 bit NOT NULL DEFAULT 'FALSE';

ALTER TABLE [dbo].[reportDefinition] ADD  dismiss bit NOT NULL DEFAULT 'FALSE';

ALTER TABLE [dbo].[reportDefinition] ADD  rfidPrint bit NOT NULL DEFAULT 'FALSE';

ALTER TABLE [dbo].[thingtype] ADD serialFormula VARCHAR(255) ;