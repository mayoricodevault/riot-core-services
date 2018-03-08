-- TABLE scheduledrule
DROP TABLE [dbo].[scheduledrule]
GO

CREATE TABLE [dbo].[scheduledrule] (
  [id] [NUMERIC](19,0) IDENTITY(1,1) NOT NULL,
  [active] [BIT],
  [code] [varchar](255) NULL,
  [cron_description] [varchar](255) NULL,
  [description] [varchar](255) NULL,
  [name] [varchar](255),
  [rule_execution_mode] [varchar](255),
  [status] [varchar](255),
  [group_id] [numeric](19, 0) NOT NULL,
  [edgebox_id] [numeric](19, 0) NOT NULL,
  [reportDefinition_id] [numeric](19, 0) NOT NULL,
  PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
)
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'ID of scheduledrule', @level0type = N'Schema',
     @level0name = 'dbo', @level1type = N'Table',  @level1name = 'scheduledrule', @level2type = N'Column', @level2name = 'id';
GO

ALTER TABLE [dbo].[scheduledrule]  WITH CHECK ADD  CONSTRAINT [FK_scheduled_group] FOREIGN KEY([group_id])
REFERENCES [dbo].[group0] ([id])
GO

ALTER TABLE [dbo].[scheduledrule]  WITH CHECK ADD  CONSTRAINT [FK_scheduled_edgebox] FOREIGN KEY([edgebox_id])
REFERENCES [dbo].[edgebox] ([id])
GO

ALTER TABLE [dbo].[scheduledrule]  WITH CHECK ADD  CONSTRAINT [FK_scheduled_reportDefinition] FOREIGN KEY([reportDefinition_id])
REFERENCES [dbo].[reportDefinition] ([id])
GO

ALTER TABLE [dbo].[edgeboxrule] ADD [scheduledRule_id] [numeric](19, 0) NOT NULL
GO

ALTER TABLE [dbo].[edgeboxrule]  WITH CHECK ADD  CONSTRAINT [FK_edgeboxrule_scheduled] FOREIGN KEY([scheduledRule_id])
REFERENCES [dbo].[scheduledrule] ([id])
GO

