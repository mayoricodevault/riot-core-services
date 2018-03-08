ALTER TABLE [dbo].[connection0] DROP CONSTRAINT UK_3n8yg35ryw9qbsgheytb6nag9
GO

ALTER TABLE [dbo].[reportdefinition] ADD [description] VARCHAR(8000) NOT NULL
GO

ALTER TABLE [dbo].[thingtypetemplate] ADD [autoCreate] [bit] DEFAULT 0
GO
