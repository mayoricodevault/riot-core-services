package com.tierconnect.riot.api.mongoShell.operations;

import com.tierconnect.riot.api.mongoShell.MongoDataBaseTest;
import com.tierconnect.riot.api.mongoShell.MongoShellClient;
import com.tierconnect.riot.api.mongoShell.ResultQuery;
import com.tierconnect.riot.api.mongoShell.query.QueryBuilderTest;
import com.tierconnect.riot.api.mongoShell.testUtils.MongoDataBaseTestUtils;
import com.tierconnect.riot.api.mongoShell.testUtils.PropertiesReaderUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.URL;


import static org.junit.Assert.*;

/**
 * Created by achambi on 2/2/17.
 * A class to test Aggregate Operations.
 */
public class AggregateOperationTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void isAllowDiskUse() throws Exception {
        OperationExecutor expectExecutor = Mockito.mock(OperationExecutor.class);
        AggregateOperation aggregateOperation = new AggregateOperation("", expectExecutor, "");
        assertEquals(true, aggregateOperation.isAllowDiskUse());
    }

    @Test
    public void isExplain() throws Exception {
        OperationExecutor expectExecutor = Mockito.mock(OperationExecutor.class);
        AggregateOperation aggregateOperation = new AggregateOperation("", expectExecutor, "");
        assertEquals(true, aggregateOperation.isAllowDiskUse());
    }

    @Test
    public void getCollection() throws Exception {
        OperationExecutor expectExecutor = Mockito.mock(OperationExecutor.class);
        AggregateOperation aggregateOperation = new AggregateOperation("collectionTest", expectExecutor, "");
        assertEquals("collectionTest", aggregateOperation.getCollection());
    }

    @Test
    public void getOperationExecutor() throws Exception {
        OperationExecutor expectExecutor = Mockito.mock(OperationExecutor.class);
        AggregateOperation aggregateOperation = new AggregateOperation("collectionTest", expectExecutor, "");
        assertEquals(expectExecutor, aggregateOperation.getOperationExecutor());
        assertEquals(expectExecutor.hashCode(), aggregateOperation.getOperationExecutor().hashCode());
    }

    @Test
    public void setAllowDiskUse() throws Exception {
        OperationExecutor expectExecutor = Mockito.mock(OperationExecutor.class);
        AggregateOperation aggregateOperation = new AggregateOperation("collectionTest", expectExecutor, "");
        aggregateOperation.setAllowDiskUse(true);
        assertEquals(true, aggregateOperation.isAllowDiskUse());
    }

    @Test
    public void setExplain() throws Exception {
        OperationExecutor expectExecutor = Mockito.mock(OperationExecutor.class);
        AggregateOperation aggregateOperation = new AggregateOperation("collectionTest", expectExecutor, "");
        aggregateOperation.setExplain(true);
        assertEquals(true, aggregateOperation.isExplain());
    }

    @Test
    public void execute() throws Exception {
        MongoDataBaseTestUtils.prepareDataBase();
        PropertiesReaderUtil.setConfigurationFile("propertiesMongoShellLocalHostTest.properties");
        MongoShellClient mockMongoShellClient = Mockito.spy(new MongoShellClient());
        AggregateOperation aggregateOperation = new AggregateOperation("things", mockMongoShellClient.getExecutor(),
                "[{\"$group\": {\"_id\": null, \"total\": {\"$sum\": 1}}}]");
        ResultQuery resultQuery = aggregateOperation.setAllowDiskUse(false).setExplain(false).execute();

        assertEquals(-1, resultQuery.getTotal());
        assertEquals(1, resultQuery.getRows().size());
        String resultGroupTest = resultQuery.getRows().get(0).toString();
        assertEquals("{_id=null, total=53.0}", resultGroupTest);
    }

    @Test
    public void executeWithExplain() throws Exception {
        MongoDataBaseTestUtils.prepareDataBase();
        PropertiesReaderUtil.setConfigurationFile("propertiesMongoShellLocalHostTest.properties");
        MongoShellClient mockMongoShellClient = Mockito.spy(new MongoShellClient());
        AggregateOperation aggregateOperation = new AggregateOperation("things", mockMongoShellClient.getExecutor(),
                "[{\"$group\": {\"_id\": null, \"total\": {\"$sum\": 1}}}]");
        ResultQuery resultQuery = aggregateOperation.setAllowDiskUse(false).setExplain(true).execute();
        assertEquals(-1, resultQuery.getTotal());
        assertEquals(1, resultQuery.getRows().size());
        String resultGroupTest = resultQuery.getRows().get(0).toString();
        assertEquals("{_id=null, total=53.0}", resultGroupTest);
        //TODO: The explain result is change always per mongo version only It is  validating the result  is not null.
        assertNotNull(resultQuery.getExecutionPlan());

    }
}