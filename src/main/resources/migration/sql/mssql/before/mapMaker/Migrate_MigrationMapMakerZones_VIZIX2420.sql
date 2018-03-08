ALTER TABLE [dbo].[localMap] ADD [rotationDegree] DOUBLE
GO

CREATE TABLE [dbo].[localmappoint] (
  [id] bigint NOT NULL IDENTITY,
  [arrayIndex] bigint DEFAULT NULL,
  [x] float DEFAULT NULL,
  [y] float DEFAULT NULL,
  [localMap_id] bigint DEFAULT NULL,
  PRIMARY KEY ([id])
 ,
  CONSTRAINT [FK_qk4vcf03cqdbk2bpineusepyl] FOREIGN KEY ([localMap_id]) REFERENCES localmap ([id])
);

CREATE INDEX [FK_qk4vcf03cqdbk2bpineusepyl] ON localmappoint ([localMap_id]);