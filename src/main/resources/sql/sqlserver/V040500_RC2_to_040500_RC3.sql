IF  NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'importexport' AND type in (N'U'))
  CREATE TABLE [dbo].[importexport](
    [id] [numeric](19, 0) IDENTITY(1,1) NOT NULL,
    [duration] [numeric](19, 0) NULL,
    [endDate] [datetime] NULL,
    [errorRecord] [numeric](19, 0) NULL,
    [processType] [varchar](255) NULL,
    [startDate] [datetime] NULL,
    [successRecord] [numeric](19, 0) ,
    [totalRecord] [numeric](19, 0) ,
    [userId] [numeric](19, 0) NOT NULL,
    [type] [varchar](255) NULL,
  PRIMARY KEY CLUSTERED
      (
        [id] ASC
      )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
  )
GO