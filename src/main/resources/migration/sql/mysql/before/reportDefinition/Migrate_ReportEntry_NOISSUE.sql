UPDATE riot_main.reportentryoption a
   SET a.group_id = (SELECT b.group_id
                      FROM riot_main.reportdefinition b
							WHERE b.id = a.reportDefinition_id);
