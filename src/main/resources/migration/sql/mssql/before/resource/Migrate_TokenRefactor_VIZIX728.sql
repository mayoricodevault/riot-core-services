ALTER TABLE [dbo].[token] ADD [tokenActive] BIT NULL
GO

UPDATE [dbo].[token] set tokenActive = 1
GO

ALTER TABLE [dbo].[token] ALTER COLUMN [tokenActive] bit NOT NULL
GO

ALTER TABLE [dbo].[token] ADD DEFAULT (((1))) FOR [tokenActive] WITH VALUES
GO
