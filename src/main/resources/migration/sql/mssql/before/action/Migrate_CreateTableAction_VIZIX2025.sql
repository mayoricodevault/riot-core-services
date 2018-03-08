-- TABLE Action configuration
DROP TABLE [dbo].[actionconfiguration]
GO

CREATE TABLE [dbo].[actionconfiguration] (
  [id] [NUMERIC](19,0) IDENTITY(1,1) NOT NULL,
  [code] [VARCHAR](20) NOT NULL,
  [configuration] [text],
  [name] [varchar](150) NOT NULL,
  [status] [varchar](20) NULL,
  [type] [varchar](100) NOT NULL,
  [group_id] [numeric](19, 0) NOT NULL
  PRIMARY KEY CLUSTERED
		(
			[id] ASC
		)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
)
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'ID of action', @level0type = N'Schema',
     @level0name = 'dbo', @level1type = N'Table',  @level1name = 'actionconfiguration', @level2type = N'Column', @level2name = 'id';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'The code of the action', @level0type = N'Schema', @level0name = 'dbo',
     @level1type = N'Table', @level1name = 'actionconfiguration', @level2type = N'Column', @level2name = 'code';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'The configuration in format JSON', @level0type = N'Schema', @level0name = 'dbo',
     @level1type = N'Table',  @level1name = 'actionconfiguration', @level2type = N'Column', @level2name = 'configuration';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'The name of the action', @level0type = N'Schema', @level0name = 'dbo',
     @level1type = N'Table',  @level1name = 'actionconfiguration', @level2type = N'Column', @level2name = 'name';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'The status of the action: ACTIVE, DELETED', @level0type = N'Schema', @level0name = 'dbo',
     @level1type = N'Table',  @level1name = 'actionconfiguration', @level2type = N'Column', @level2name = 'status';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'The type of the action: HTTP', @level0type = N'Schema', @level0name = 'dbo',
     @level1type = N'Table',  @level1name = 'actionconfiguration', @level2type = N'Column', @level2name = 'type';
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'The ID reference to group0 table', @level0type = N'Schema', @level0name = 'dbo',
     @level1type = N'Table',  @level1name = 'actionconfiguration', @level2type = N'Column', @level2name = 'group_id';
GO

ALTER TABLE [dbo].[actionconfiguration]  WITH CHECK ADD  CONSTRAINT [FK_action_group] FOREIGN KEY([group_id])
REFERENCES [dbo].[group0] ([id])
GO

-- TABLE Log action
DROP TABLE [dbo].[logexecutionaction]
GO

CREATE TABLE [dbo].[logexecutionaction]
(
  [id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
  [endDate] [datetime] ,
  [iniDate] [datetime] NOT NULL,
  [processTime] [numeric](19, 0),
  [request] [text] ,
  [response] [text] ,
  [responseCode] [varchar](20) ,
  [actionConfiguration_id] [numeric](19, 0) NOT NULL ,
  [createdByUser_id] [numeric](19, 0) NOT NULL
  PRIMARY KEY CLUSTERED
  (
      [id] ASC
  )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
)
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'ID of log', @level0type = N'Schema', @level0name = 'dbo',
                            @level1type = N'Table',  @level1name = 'logexecutionaction', @level2type = N'Column', @level2name = 'id';
GO
EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'The date when the call was completed in MODAL mode', @level0type = N'Schema', @level0name = 'dbo',
                            @level1type = N'Table',  @level1name = 'logexecutionaction', @level2type = N'Column', @level2name = 'endDate';
GO
EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'The date when the call was initialized', @level0type = N'Schema', @level0name = 'dbo',
                            @level1type = N'Table',  @level1name = 'logexecutionaction', @level2type = N'Column', @level2name = 'iniDate';
GO
EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'The duration time of the call', @level0type = N'Schema', @level0name = 'dbo',
                            @level1type = N'Table',  @level1name = 'logexecutionaction', @level2type = N'Column', @level2name = 'processTime';
GO
EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'The configuration send', @level0type = N'Schema', @level0name = 'dbo',
                            @level1type = N'Table',  @level1name = 'logexecutionaction', @level2type = N'Column', @level2name = 'request';
GO
EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'Http response', @level0type = N'Schema', @level0name = 'dbo',
                            @level1type = N'Table',  @level1name = 'logexecutionaction', @level2type = N'Column', @level2name = 'response';
GO
EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'The code http of the call', @level0type = N'Schema', @level0name = 'dbo',
                            @level1type = N'Table',  @level1name = 'logexecutionaction', @level2type = N'Column', @level2name = 'responseCode';
GO
EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'The ID reference to action configuration table', @level0type = N'Schema', @level0name = 'dbo',
                            @level1type = N'Table',  @level1name = 'logexecutionaction', @level2type = N'Column', @level2name = 'actionConfiguration_id';
GO
EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'The ID reference to user0 table', @level0type = N'Schema', @level0name = 'dbo',
                            @level1type = N'Table',  @level1name = 'logexecutionaction', @level2type = N'Column', @level2name = 'createdByUser_id';
GO


ALTER TABLE [dbo].[logexecutionaction]  WITH CHECK ADD  CONSTRAINT [FK_log_action] FOREIGN KEY([actionConfiguration_id])
REFERENCES [dbo].[actionconfiguration] ([id])
GO


ALTER TABLE [dbo].[logexecutionaction]  WITH CHECK ADD  CONSTRAINT [FK_log_user] FOREIGN KEY([createdByUser_id])
REFERENCES [dbo].[user0] ([id])
GO

-- TABLE Report action
DROP TABLE [dbo].[reportactions]
GO

CREATE TABLE [dbo].[reportactions]
(
  [id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
  [displayOrder] [int] NOT NULL,
  [actionConfiguration_id] [numeric](19, 0) NOT NULL,
  [createdByUser_id] [numeric](19, 0) NOT NULL,
  [reportDefinition_id] [numeric](19, 0) NOT NULL
  PRIMARY KEY CLUSTERED
    (
      [id] ASC
    )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
)
GO

EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'ID of report action', @level0type = N'Schema', @level0name = 'dbo',
                            @level1type = N'Table',  @level1name = 'reportactions', @level2type = N'Column', @level2name = 'id';
GO
EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'Display order of the action', @level0type = N'Schema', @level0name = 'dbo',
                            @level1type = N'Table',  @level1name = 'reportactions', @level2type = N'Column', @level2name = 'displayOrder';
GO
EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'The ID reference to action configuration table', @level0type = N'Schema', @level0name = 'dbo',
                            @level1type = N'Table',  @level1name = 'reportactions', @level2type = N'Column', @level2name = 'actionConfiguration_id';
GO
EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'The ID reference to user0 table', @level0type = N'Schema', @level0name = 'dbo',
                            @level1type = N'Table',  @level1name = 'reportactions', @level2type = N'Column', @level2name = 'createdByUser_id';
GO
EXEC sp_addextendedproperty @name = N'MS_Description', @value = 'The ID reference to report definition table', @level0type = N'Schema', @level0name = 'dbo',
                            @level1type = N'Table',  @level1name = 'reportactions', @level2type = N'Column', @level2name = 'reportDefinition_id';
GO

ALTER TABLE [dbo].[reportactions]  WITH CHECK ADD  CONSTRAINT [FK_report_user] FOREIGN KEY([createdByUser_id])
REFERENCES [dbo].[user0] ([id])
GO

ALTER TABLE [dbo].[reportactions]  WITH CHECK ADD  CONSTRAINT [FK_report_action] FOREIGN KEY([actionConfiguration_id])
REFERENCES [dbo].[actionconfiguration] ([id])
GO

ALTER TABLE [dbo].[reportactions]  WITH CHECK ADD  CONSTRAINT [FK_report_reportdefinition] FOREIGN KEY([reportDefinition_id])
REFERENCES [dbo].[reportdefinition] ([id])
GO
