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

DELETE C0 FROM dbo.connection0 AS C0, dbo.connectiontype AS CT  WHERE C0.connectionType_id = CT.id AND ( ct.code = 'MQTTConnection' OR ct.code = 'SMTPConnection' )
GO

DELETE FROM dbo.connectiontype WHERE code = 'MQTTConnection' OR code = 'SMTPConnection'
GO

UPDATE connectiontype SET description='External DataBase Connection' WHERE code='DBConnection'
GO
