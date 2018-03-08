package com.tierconnect.riot.migration.steps.smartContractDefinition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.entities.SmartContractDefinition;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.iot.services.SmartContractDefinitionService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.migration.steps.edgebox.Migrate_AddParamsForSmedBridge_VIZIX1957;
import org.apache.log4j.Logger;


import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Created by mauricio on 7/4/17.
 */
public class Migrate_PartiesFormat_VIZIX_5224 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_AddParamsForSmedBridge_VIZIX1957.class);
    private static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        changePartiesFormat();
    }

    private void changePartiesFormat() throws IOException{
        SmartContractDefinitionService service = SmartContractDefinitionService.getInstance();
        List<SmartContractDefinition> list = service.listPaginated(null, null, null);
        for (SmartContractDefinition definition : list) {
            String roles = definition.getRoles();
            roles = roles.replace("\"party\":", "\"parties\":");
            JsonNode rolesNode = objectMapper.readTree(roles);
            ArrayNode newRoles = objectMapper.createArrayNode();
            Iterator iterator = rolesNode.iterator();
            while(iterator.hasNext()){
                JsonNode role = (JsonNode) iterator.next();
                JsonNode partiesNode = role.get("parties");
                ArrayNode array = objectMapper.createArrayNode();
                array.add(partiesNode);
                ((ObjectNode)role).replace("parties",array);
                newRoles.add(role);
            }
            String newRolesText = objectMapper.writer().writeValueAsString(newRoles);
            definition.setRoles(newRolesText);
            service.update(definition);
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }
}

