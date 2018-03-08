package com.tierconnect.riot.api.mongoShell;

import com.tierconnect.riot.api.mongoShell.operations.OperationExecutor;
import com.tierconnect.riot.api.mongoShell.testUtils.PropertiesReaderUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * Created by achambi on 2/2/17.
 * Test to verify the correct implementation to databaseConnection.
 */
public class MongoDataBaseTest {
    @Before
    public void setUp() throws Exception {
        PropertiesReaderUtil.setConfigurationFile("propertiesMongoShellLocalHost.properties");
    }

    @Test
    public void getCollection() throws Exception {
        OperationExecutor expectExecutor = Mockito.mock(OperationExecutor.class);
        MongoDataBase mockMongoDataBase = Mockito.spy(new MongoDataBase(expectExecutor));
        MongoCollection resultMongoCollection = mockMongoDataBase.getCollection("testCollectionObject");
        assertEquals(resultMongoCollection.getExecutor(), expectExecutor);
        assertEquals(resultMongoCollection.getExecutor().hashCode(), expectExecutor.hashCode());
    }

    @Test
    public void getExecutor() throws Exception {
        MongoShellClient mockMongoShellClient = Mockito.spy(new MongoShellClient());
        OperationExecutor expectExecutor = Mockito.mock(OperationExecutor.class);
        Mockito.when(mockMongoShellClient.getExecutor()).thenReturn(expectExecutor);
        MongoDataBase result = mockMongoShellClient.getDataBase();
        OperationExecutor resultExecutor = result.getExecutor();
        assertEquals(expectExecutor, resultExecutor);
        assertEquals(expectExecutor.hashCode(), resultExecutor.hashCode());
    }
}