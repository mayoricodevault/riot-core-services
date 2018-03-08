CREATE PROCEDURE [dbo].thingTypeUdf
AS
  DECLARE @id [numeric](20)

  SELECT  @id = id FROM [dbo].[dataType] WHERE id = 27

  IF @id IS NULL
  BEGIN
	  INSERT INTO dbo.datatype(id, code, description, type, typeParent, value)
    VALUES (27,'THING_TYPE','Thing Type','Native Thing Type','NATIVE_THING_TYPE','Thing Type')
  END;
GO

EXECUTE [dbo].[thingTypeUdf] GO

DROP PROCEDURE [dbo].[thingTypeUdf] GO
