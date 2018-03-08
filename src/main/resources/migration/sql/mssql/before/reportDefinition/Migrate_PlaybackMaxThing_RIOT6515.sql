ALTER TABLE [dbo].[reportDefinition] ADD  playbackMaxThing int NULL
GO

UPDATE [dbo].[reportDefinition] SET playbackMaxThing = 100
GO