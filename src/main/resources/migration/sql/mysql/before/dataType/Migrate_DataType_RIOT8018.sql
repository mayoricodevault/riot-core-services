DELIMITER $$ CREATE PROCEDURE `riot_main.thingTypeUdf`()
BEGIN
  DECLARE id_ bigint(20);
  SELECT id INTO id_ FROM riot_main.dataType WHERE id = 27;

  IF id_ IS NULL THEN
	INSERT INTO datatype(id, code, description, type, typeParent, value)
    VALUES (27,'THING_TYPE','Thing Type','Native Thing Type','NATIVE_THING_TYPE','Thing Type');
  END IF;
END  $$ DELIMITER ;

CALL riot_main.thingTypeUdf();

DROP PROCEDURE riot_main.thingTypeUdf;
