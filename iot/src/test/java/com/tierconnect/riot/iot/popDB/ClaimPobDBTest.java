package com.tierconnect.riot.iot.popDB;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.RoleService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.iot.popdb.PopDBBase;
import com.tierconnect.riot.iot.popdb.PopDBIOTUtils;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.iot.services.ReportDefinitionService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import org.hibernate.Transaction;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 * Created by dbascope on 06-22-17
 */
//TODO: Test cases not work!, Victor Angel Chambi Nina, 17-08/2017.
public class ClaimPobDBTest {
    private static long rand = Math.round(Math.random()*10000);

    @Test
    public void createClaimStructure() throws Exception {
        Map<String, Object> map = getJSONMap();
        PopDBBase popDB = new PopDBBase();
        setPopDBProperties(map);
        popDB.currentPopDB = "Claim";
        List<Map<String, Object>> serialMap = (List<Map<String, Object>>) map.get("serials");

        PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
        System.getProperties().put("hibernate.cache.use_second_level_cache", "false");
        System.getProperties().put("hibernate.cache.use_query_cache", "false");
        Configuration.init(null);

        for (Map<String, Object> serialNumber : serialMap) {

            Transaction transaction = UserService.getUserDAO().getSession().getTransaction();
            transaction.begin();
            PopDBIOTUtils.initShiroWithRoot();

            setSerialProperty(serialNumber);
            popDB.executeModules((JSONArray) popDB.getPopDBDependencies().get("Claim").get("executeModules"));

            transaction.commit();
        }

        Transaction transaction = UserService.getUserDAO().getSession().getTransaction();
        transaction.begin();

        Group testGroup = GroupService.getInstance().getByCode("claimTest" + rand + "");
        Assert.assertNotNull("Group claimTest" + rand + " does not exists!", testGroup);
        Assert.assertNotNull("Group claimTest" + rand + "_Store does not exists!",
            GroupService.getInstance().getByCode("claimTest" + rand + "_Store"));
        Assert.assertNotNull("Group claimTest" + rand + "_Department does not exists!",
            GroupService.getInstance().getByCode("claimTest" + rand + "_Department"));

        Assert.assertNotNull("ThingType SF_claimTest" + rand + " does not exists!",
            ThingTypeService.getInstance().getByCodeAndGroup("SF_claimTest" + rand + "", testGroup));
        Assert.assertNotNull("ThingType SFS_claimTest" + rand + " does not exists!",
            ThingTypeService.getInstance().getByCodeAndGroup("SFS_claimTest" + rand + "", testGroup));
        Assert.assertNotNull("ThingType SFT_claimTest" + rand + " does not exists!",
            ThingTypeService.getInstance().getByCodeAndGroup("SFT_claimTest" + rand + "", testGroup));

        Assert.assertNotNull("Role RoleclaimTest" + rand + " does not exists!",
            RoleService.getInstance().getByNameAndGroup("RoleclaimTest" + rand + "", testGroup));

        Assert.assertNotNull("User usertest" + rand + " does not exists!",
            UserService.getInstance().getByUsername("usertest" + rand + "", testGroup));

        Assert.assertNotNull("Connection MQTT_claimTest" + rand + " does not exists!",
            ConnectionService.getInstance().getByCodeAndGroup("MQTT_claimTest" + rand + "", testGroup));
        Assert.assertNotNull("Connection MONGO_claimTest" + rand + " does not exists!",
            ConnectionService.getInstance().getByCodeAndGroup("MONGO_claimTest" + rand + "", testGroup));
        Assert.assertNotNull("Connection MYSQL_claimTest" + rand + " does not exists!",
            ConnectionService.getInstance().getByCodeAndGroup("MYSQL_claimTest" + rand + "", testGroup));

        Assert.assertNotNull("Bridge CORE_claimTest" + rand + " does not exists!",
            EdgeboxService.getInstance().getByCode("CORE_claimTest" + rand + ""));
        Assert.assertNotNull("Bridge ALEB_claimTest" + rand + " does not exists!",
            EdgeboxService.getInstance().getByCode("ALEB_claimTest" + rand + ""));
        Assert.assertNotNull("Bridge STAR_claimTest" + rand + " does not exists!",
            EdgeboxService.getInstance().getByCode("STAR_claimTest" + rand + ""));

        Assert.assertNotNull("Report STARflex Dashboard - claimTest" + rand + " does not exists!",
            ReportDefinitionService.getInstance().getByNameAndType("STARflex Dashboard - claimTest" + rand + "", "table"));
        Assert.assertNotNull("Report STARflex Monitor - claimTest" + rand + " does not exists!",
            ReportDefinitionService.getInstance().getByNameAndType("STARflex Monitor - claimTest" + rand + "", "table"));
        Assert.assertNotNull("Report STARflex Status - claimTest" + rand + " does not exists!",
            ReportDefinitionService.getInstance().getByNameAndType("STARflex Status - claimTest" + rand + "", "table"));
        Assert.assertNotNull("Report STARflex Unique Tags - claimTest" + rand + " does not exists!",
            ReportDefinitionService.getInstance().getByNameAndType("STARflex Unique Tags - claimTest" + rand + "", "table"));

        transaction.commit();
    }

    private Map<String, Object> getJSONMap() {
        JSONObject groupJSON = new JSONObject();
        groupJSON.put("name", "claimTest" + rand);
        JSONObject jsonObject = new JSONObject();

        JSONObject userJSON = new JSONObject();
        userJSON.put("username", "usertest" + rand);
        userJSON.put("password", "usertest" + rand);
        userJSON.put("firstName", "FirstName");
        userJSON.put("lastName", "LastName");
        userJSON.put("email", "user@email.com");

        JSONObject serial0 = new JSONObject();
        serial0.put("serial", "testMACId0001" + rand);
        serial0.put("thingTypeCode", "testThingTypeCode");
        JSONObject serial1 = new JSONObject();
        serial1.put("serial", "testMACId0002" + rand);
        serial1.put("thingTypeCode", "testThingTypeCode");

        JSONArray serials = new JSONArray();
        serials.add(serial0);
        serials.add(serial1);
        jsonObject.put("group", groupJSON);
        jsonObject.put("user", userJSON);
        jsonObject.put("serials", serials);

        return jsonObject;
    }

    private void setPopDBProperties(Map<String, Object> map) {
        Map<String, Object> groupMap = (Map<String, Object>) map.get("group");
        Map<String, Object> userMap = (Map<String, Object>) map.get("user");
        System.setProperty("popdb.option", "Claim");
        System.setProperty("popdb.erase", "false");
        System.setProperty("tenant.name", groupMap.get("name").toString());
        System.setProperty("tenant.code", groupMap.get("name").toString());
        System.setProperty("user.username", userMap.get("username").toString());
        System.setProperty("user.password", userMap.get("password").toString());
        System.setProperty("user.firstName", userMap.get("firstName").toString());
        System.setProperty("user.lastName", userMap.get("lastName").toString());
        System.setProperty("user.email", userMap.get("email").toString());
    }

    private void setSerialProperty(Map<String, Object> serialMap) {
        System.setProperty("serial.number", serialMap.get("serial").toString());
        System.setProperty("serial.thingTypeCode", serialMap.get("thingTypeCode").toString());
    }
}
