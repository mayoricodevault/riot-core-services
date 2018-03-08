package com.tierconnect.riot.appcore.services;

import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.core.test.BaseTest;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * Created by vealaro on 9/27/16.
 */
@RunWith(value = Parameterized.class)
public class ConnectionServiceTest extends BaseTest {

    private static Logger logger = Logger.getLogger(ConnectionServiceTest.class);

    private String nameConnectionTest;
    private String codeConnectionTest;
    private String codeTypeConnectionTest;

    @Parameters(name = "Connection {index}: with name=\"{0}\", codeConnection=\"{1}\", typeConnection=\"{2}\"")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Connection MQTT0", "MQTT0", "MQTT"},
                {"Connection MQTT2", "MQTT2", "MQTT"},
                {"Connection MQTT3", "MQTT3", "MQTT"},
                {"Connection MQTT4", "MQTT4", "MQTT"},
                {"Connection MQTT5", "MQTT5", "MQTT"},
                {"Connection MQTT6", "MQTT6", "MQTT"},
                {"Connection MQTT7", "MQTT7", "MQTT"},
                {"Connection MQTT8", "MQTT8", "MQTT"},
        });
    }

    public ConnectionServiceTest(String nameConnectionTest, String codeConnectionTest, String codeTypeConnectionTest) {
        this.nameConnectionTest = nameConnectionTest;
        this.codeConnectionTest = codeConnectionTest;
        this.codeTypeConnectionTest = codeTypeConnectionTest;
    }

    @Test
    public void testGetByCode() {
        ConnectionService service = ConnectionService.getInstance();
        Connection mqtt = service.getByCode(codeConnectionTest);
        if (mqtt == null) {
            Assert.assertNotNull(service.insert(createConnection(nameConnectionTest, codeConnectionTest, codeTypeConnectionTest)));
            mqtt = service.getByCode(codeConnectionTest);
        }
        Assert.assertEquals(mqtt.getCode(), codeConnectionTest);
        testDelete();
    }

    @Test
    public void testInsert() {
        try {
            ConnectionService connectionService = ConnectionService.getInstance();
            Assert.assertNotNull(connectionService.insert(createConnection(nameConnectionTest, codeConnectionTest, codeTypeConnectionTest)));
        } catch (UserException e) {
            Assert.assertTrue("Exists code ", e.getMessage().contains("already exist"));
            logger.info("Error: ", e);
        } finally {
            testDelete();
        }
    }

    @Test
    public void testUpdate() {
        try {
            ConnectionService connectionService = ConnectionService.getInstance();
            Connection connectionMQTTA = connectionService.getByCode(codeConnectionTest);
            Connection connectionMQTTB = new Connection();
            if (connectionMQTTA == null) {
                connectionMQTTA = connectionService.insert(createConnection(nameConnectionTest, codeConnectionTest, codeTypeConnectionTest));
            }
            Assert.assertNotNull(connectionMQTTA.getId());
            BeanUtils.copyProperties(connectionMQTTB, connectionMQTTA);
            connectionMQTTB.setCode("MQTT");
            logger.info(String.format("Set code connection \"%s\" to \"%s\" ", codeConnectionTest, "MQTT"));
            connectionService.update(connectionMQTTB);
        } catch (UserException e) {
            Assert.assertTrue("Exists code ", e.getMessage().contains("already exist"));
            logger.error("Error: " + e.getMessage());
        } catch (InvocationTargetException | IllegalAccessException e) {
            Assert.fail();
        } finally {
            testDelete();
        }

    }

    @Test
    public void testDelete() {
        ConnectionService connectionService = ConnectionService.getInstance();
        Connection connectionWithID = connectionService.getByCode(codeConnectionTest);
        if (connectionWithID == null) {
            connectionWithID = connectionService.insert(createConnection(nameConnectionTest, codeConnectionTest, codeTypeConnectionTest));
            logger.info(String.format("Create connection with code \"%s\"", codeConnectionTest));
        }
        Assert.assertTrue(connectionWithID.getId() != null);
        connectionService.delete(connectionWithID);
        logger.info(String.format("Delete connection with code \"%s\" OK!!", codeConnectionTest));
        Connection connectionDelete = connectionService.getByCode(codeConnectionTest);
        Assert.assertNull(connectionDelete);
    }

    private Connection createConnection(String nameConnection, String codeConnection, String codeTypeConnection) {
        ConnectionTypeService connectionTypeService = ConnectionTypeServiceBase.getInstance();
        ConnectionService connectionService = ConnectionService.getInstance();
        Assert.assertNotNull(connectionTypeService);
        Assert.assertNotNull(connectionService);
        ConnectionType connectionTypeMQTT = connectionTypeService.getConnectionTypeByCode(codeTypeConnection);
        Assert.assertNotNull(connectionTypeMQTT);
        Connection connection = new Connection();
        connection.setConnectionType(connectionTypeMQTT);
        connection.setCode(codeConnection);
        connection.setGroup(connectionTypeMQTT.getGroup());
        connection.setName(nameConnection);
        connection.setProperties("{\"value\":\"test\"}");
        return connection;
    }

}